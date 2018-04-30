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
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions._
import org.opalj.fpcf.properties.AllocationFreeness
import org.opalj.fpcf.properties.MethodWithAllocations
import org.opalj.fpcf.properties.AllocationFreeMethod

import scala.annotation.switch

/**
 * A simple analysis that identifies methods that never allocate any objects/arrays.
 *
 * @author Dominik Helm
 */
class L0AllocationFreenessAnalysis private[analyses] ( final val project: SomeProject)
    extends FPCFAnalysis {

    import project.nonVirtualCall

    val declaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Determines the allocation freeness of the method.
     *
     * This function encapsulates the continuation.
     */
    def determineAllocationFreeness(
        declaredMethod: DeclaredMethod
    ): PropertyComputationResult = {

        if (!declaredMethod.hasDefinition)
            return Result(declaredMethod, MethodWithAllocations);

        val method = declaredMethod.methodDefinition
        val declaringClassType = method.classFile.thisType
        val methodDescriptor = method.descriptor
        val methodName = method.name
        val body = method.body.get
        val instructions = body.instructions
        val maxPC = instructions.length

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty

        var currentPC = 0
        while (currentPC < maxPC) {
            val instruction = instructions(currentPC)
            (instruction.opcode: @switch) match {
                case NEW.opcode | NEWARRAY.opcode | MULTIANEWARRAY.opcode | ANEWARRAY.opcode ⇒
                    return Result(declaredMethod, MethodWithAllocations);

                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode ⇒ instruction match {
                    case MethodInvocationInstruction(`declaringClassType`, _, `methodName`, `methodDescriptor`) ⇒
                    // We have a self-recursive call; such calls do not influence the allocation
                    // freeness and are ignored.
                    // Let's continue with the evaluation of the next instruction.

                    case mii: NonVirtualMethodInvocationInstruction ⇒
                        nonVirtualCall(mii) match {
                            case Success(callee) ⇒
                                /* Recall that self-recursive calls are handled earlier! */
                                val allocationFreeness =
                                    propertyStore(declaredMethods(callee), AllocationFreeness.key)

                                allocationFreeness match {
                                    case FinalEP(_, AllocationFreeMethod) ⇒ /* Nothing to do */

                                    // Handling cyclic computations
                                    case ep @ IntermediateEP(_, _, AllocationFreeMethod) ⇒
                                        dependees += ep

                                    case EPS(_, _, _) ⇒
                                        return Result(declaredMethod, MethodWithAllocations);

                                    case epk ⇒
                                        dependees += epk
                                }

                            case _ /* Empty or Failure */ ⇒
                                // We know nothing about the target method (it is not
                                // found in the scope of the current project).
                                return Result(declaredMethod, MethodWithAllocations);

                        }
                }

                case INVOKEDYNAMIC.opcode | INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode ⇒
                    // We don't handle these calls here, just treat them as having allocations
                    return Result(declaredMethod, MethodWithAllocations);

                case _ ⇒
                    // All other instructions (IFs, Load/Stores, Arith., etc.) allocate no objects
                    // as long as no implicit exceptions are raised.
                    if (instruction.jvmExceptions.nonEmpty) {
                        // JVM Exceptions result in the exception object being allocated.
                        return Result(declaredMethod, MethodWithAllocations);
                    }
            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        if (dependees.isEmpty)
            return Result(declaredMethod, AllocationFreeMethod);

        // This function computes the “allocation freeness for a method based on the allocation
        // freeness of its callees
        def c(eps: SomeEPS): PropertyComputationResult = {
            // Let's filter the entity.
            dependees = dependees.filter(_.e ne eps.e)

            eps match {
                case FinalEP(_, AllocationFreeMethod) ⇒
                    if (dependees.isEmpty)
                        Result(declaredMethod, AllocationFreeMethod)
                    else {
                        IntermediateResult(
                            declaredMethod,
                            MethodWithAllocations,
                            AllocationFreeMethod,
                            dependees,
                            c
                        )
                    }

                case FinalEP(_, MethodWithAllocations) ⇒
                    Result(declaredMethod, MethodWithAllocations)

                case IntermediateEP(_, _, _) ⇒
                    dependees += eps
                    IntermediateResult(
                        declaredMethod,
                        MethodWithAllocations,
                        AllocationFreeMethod,
                        dependees,
                        c
                    )
            }
        }

        IntermediateResult(
            declaredMethod,
            MethodWithAllocations,
            AllocationFreeMethod,
            dependees,
            c
        )
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineAllocationFreeness(e: Entity): PropertyComputationResult = {
        e match {
            case m: DeclaredMethod ⇒ determineAllocationFreeness(m)
            case e ⇒
                throw new UnknownError("allocation freeness is only defined for methods")
        }
    }
}

trait L0AllocationFreenessAnalysisScheduler extends ComputationSpecification {
    override def derives: Set[PropertyKind] = Set(AllocationFreeness)

    override def uses: Set[PropertyKind] = Set.empty
}

object EagerL0AllocationFreenessAnalysis extends L0AllocationFreenessAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L0AllocationFreenessAnalysis(project)
        val declaredMethods = project.get(DeclaredMethodsKey).declaredMethods
        propertyStore.scheduleForEntities(declaredMethods)(analysis.determineAllocationFreeness)
        analysis
    }
}

object LazyL0AllocationFreenessAnalysis extends L0AllocationFreenessAnalysisScheduler
    with FPCFLazyAnalysisScheduler {
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L0AllocationFreenessAnalysis(project)
        propertyStore.registerLazyPropertyComputation(
            AllocationFreeness.key,
            analysis.doDetermineAllocationFreeness
        )
        analysis
    }
}
