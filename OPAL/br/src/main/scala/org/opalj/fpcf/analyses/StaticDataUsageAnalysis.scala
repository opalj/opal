/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analyses

import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions._
import org.opalj.fpcf.properties.StaticDataUsage
import org.opalj.fpcf.properties.CompileTimeConstancy
import org.opalj.fpcf.properties.UsesVaryingData
import org.opalj.fpcf.properties.UsesNoStaticData
import org.opalj.fpcf.properties.UsesConstantDataOnly
import org.opalj.fpcf.properties.NoVaryingDataUse
import org.opalj.fpcf.properties.CompileTimeConstantField
import org.opalj.fpcf.properties.CompileTimeVaryingField

import scala.annotation.switch

/**
 * A simple analysis that identifies methods that use global state that may vary during one or
 * between several program executions.
 *
 * @author Dominik Helm
 */
class StaticDataUsageAnalysis private[analyses] ( final val project: SomeProject)
    extends FPCFAnalysis {

    import project.nonVirtualCall
    import project.resolveFieldReference

    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Retrieves and commits the methods static data usage as calculated for its declaring class
     * type for the current DefinedMethod that represents the non-overwritten method in a subtype.
     */
    def baseMethodStaticDataUsage(dm: DefinedMethod): PropertyComputationResult = {

        def c(eps: SomeEOptionP): PropertyComputationResult = eps match {
            case FinalEP(_, sdu)                ⇒ Result(dm, sdu)
            case ep @ IntermediateEP(_, lb, ub) ⇒ IntermediateResult(dm, lb, ub, Seq(ep), c)
            case epk ⇒
                IntermediateResult(dm, UsesVaryingData, UsesNoStaticData, Seq(epk), c)
        }

        c(propertyStore(declaredMethods(dm.definedMethod), StaticDataUsage.key))
    }

    /**
     * Determines the allocation freeness of the method.
     *
     * This function encapsulates the continuation.
     */
    def determineUsage(
        definedMethod: DefinedMethod
    ): PropertyComputationResult = {

        if (definedMethod.methodDefinition.body.isEmpty)
            return Result(definedMethod, UsesVaryingData);

        val method = definedMethod.methodDefinition
        val declaringClassType = method.classFile.thisType

        // If thhis is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if (declaringClassType ne definedMethod.declaringClassType)
            return baseMethodStaticDataUsage(definedMethod.asDefinedMethod);

        val methodDescriptor = method.descriptor
        val methodName = method.name
        val body = method.body.get
        val instructions = body.instructions
        val maxPC = instructions.length

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty
        var maxLevel: StaticDataUsage = UsesNoStaticData

        var currentPC = 0
        while (currentPC < maxPC) {
            val instruction = instructions(currentPC)
            (instruction.opcode: @switch) match {
                case GETSTATIC.opcode ⇒
                    val GETSTATIC(declaringClass, fieldName, fieldType) = instruction

                    maxLevel = UsesConstantDataOnly

                    resolveFieldReference(declaringClass, fieldName, fieldType) match {

                        // ... we have no support for arrays at the moment
                        case Some(field) ⇒
                            propertyStore(field, CompileTimeConstancy.key) match {
                                case FinalEP(_, CompileTimeConstantField) ⇒
                                case FinalEP(_, _) ⇒
                                    return Result(definedMethod, UsesVaryingData);
                                case ep ⇒
                                    dependees += ep
                            }

                        case _ ⇒
                            // We know nothing about the target field (it is not
                            // found in the scope of the current project).
                            return Result(definedMethod, UsesVaryingData);
                    }

                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode ⇒ instruction match {
                    case MethodInvocationInstruction(`declaringClassType`, _, `methodName`, `methodDescriptor`) ⇒
                    // We have a self-recursive call; such calls do not influence the allocation
                    // freeness and are ignored.
                    // Let's continue with the evaluation of the next instruction.

                    case mii: NonVirtualMethodInvocationInstruction ⇒
                        nonVirtualCall(mii) match {
                            case Success(callee) ⇒
                                /* Recall that self-recursive calls are handled earlier! */
                                val constantUsage =
                                    propertyStore(declaredMethods(callee), StaticDataUsage.key)

                                constantUsage match {
                                    case FinalEP(_, UsesNoStaticData) ⇒ /* Nothing to do */

                                    case FinalEP(_, UsesConstantDataOnly) ⇒
                                        maxLevel = UsesConstantDataOnly

                                    // Handling cyclic computations
                                    case ep @ IntermediateEP(_, _, _: NoVaryingDataUse) ⇒
                                        dependees += ep

                                    case EPS(_, _, _) ⇒
                                        return Result(definedMethod, UsesVaryingData);

                                    case epk ⇒
                                        dependees += epk
                                }

                            case _ /* Empty or Failure */ ⇒
                                // We know nothing about the target method (it is not
                                // found in the scope of the current project).
                                return Result(definedMethod, UsesVaryingData);

                        }
                }

                case INVOKEDYNAMIC.opcode | INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode ⇒
                    // We don't handle these calls here, just treat them as having allocations
                    return Result(definedMethod, UsesVaryingData);

                case _ ⇒
                // Other instructions (IFs, Load/Stores, Arith., etc.) do not use static data
            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        if (dependees.isEmpty)
            return Result(definedMethod, maxLevel);

        // This function computes the “static data usage" for a method based on the usage of its
        // callees and the compile-time constancy of its static field reads
        def c(eps: SomeEPS): PropertyComputationResult = {
            // Let's filter the entity.
            dependees = dependees.filter(_.e ne eps.e)

            eps match {
                case FinalEP(_, du: NoVaryingDataUse) ⇒
                    if (du eq UsesConstantDataOnly) maxLevel = UsesConstantDataOnly
                    if (dependees.isEmpty)
                        Result(definedMethod, maxLevel)
                    else {
                        IntermediateResult(definedMethod, UsesVaryingData, maxLevel, dependees, c)
                    }

                case FinalEP(_, UsesVaryingData) ⇒ Result(definedMethod, UsesVaryingData)

                case FinalEP(_, CompileTimeConstantField) ⇒
                    if (dependees.isEmpty)
                        Result(definedMethod, maxLevel)
                    else {
                        IntermediateResult(definedMethod, UsesVaryingData, maxLevel, dependees, c)
                    }

                case FinalEP(_, CompileTimeVaryingField) ⇒ Result(definedMethod, UsesVaryingData)

                case IntermediateEP(_, _, UsesConstantDataOnly) ⇒
                    maxLevel = UsesConstantDataOnly
                    dependees += eps
                    IntermediateResult(definedMethod, UsesVaryingData, maxLevel, dependees, c)

                case IntermediateEP(_, _, _) ⇒
                    dependees += eps
                    IntermediateResult(definedMethod, UsesVaryingData, maxLevel, dependees, c)
            }
        }

        IntermediateResult(definedMethod, UsesVaryingData, maxLevel, dependees, c)
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineUsage(e: Entity): PropertyComputationResult = {
        e match {
            case m: DefinedMethod  ⇒ determineUsage(m)
            case m: DeclaredMethod ⇒ Result(m, UsesVaryingData)
            case e ⇒
                throw new UnknownError("static constant usage is only defined for methods")
        }
    }
}

trait StaticDataUsageAnalysisScheduler extends ComputationSpecification {
    override def derives: Set[PropertyKind] = Set(StaticDataUsage)

    override def uses: Set[PropertyKind] = Set(CompileTimeConstancy)
}

object EagerStaticDataUsageAnalysis extends StaticDataUsageAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new StaticDataUsageAnalysis(project)
        val declaredMethods = project.get(DeclaredMethodsKey).declaredMethods.collect {
            case dm if dm.hasDefinition && dm.methodDefinition.body.isDefined ⇒ dm.asDefinedMethod
        }
        propertyStore.scheduleEagerComputationsForEntities(declaredMethods)(analysis.determineUsage)
        analysis
    }
}

object LazyStaticDataUsageAnalysis extends StaticDataUsageAnalysisScheduler
    with FPCFLazyAnalysisScheduler {
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new StaticDataUsageAnalysis(project)
        propertyStore.registerLazyPropertyComputation(
            StaticDataUsage.key,
            analysis.doDetermineUsage
        )
        analysis
    }
}
