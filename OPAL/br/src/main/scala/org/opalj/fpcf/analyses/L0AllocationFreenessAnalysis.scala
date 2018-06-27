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
import org.opalj.br.analyses.DeclaredMethodsKey
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

    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Retrieves and commits the methods allocation freeness as calculated for its declaring class
     * type for the current DefinedMethod that represents the non-overwritten method in a subtype.
     */
    def baseMethodAllocationFreeness(dm: DefinedMethod): PropertyComputationResult = {

        def c(eps: SomeEOptionP): PropertyComputationResult = eps match {
            case FinalEP(_, af) ⇒ Result(dm, af)
            case ep @ IntermediateEP(_, lb, ub) ⇒
                IntermediateResult(dm, lb, ub, Seq(ep), c, CheapPropertyComputation)
            case epk ⇒
                IntermediateResult(
                    dm, MethodWithAllocations, AllocationFreeMethod,
                    Seq(epk), c, CheapPropertyComputation
                )
        }

        c(propertyStore(declaredMethods(dm.definedMethod), AllocationFreeness.key))
    }

    /**
     * Determines the allocation freeness of the method.
     *
     * This function encapsulates the continuation.
     */
    def determineAllocationFreeness(
        definedMethod: DefinedMethod
    ): PropertyComputationResult = {

        if (definedMethod.definedMethod.body.isEmpty)
            return Result(definedMethod, MethodWithAllocations);

        val method = definedMethod.definedMethod
        val declaringClassType = method.classFile.thisType

        // If thhis is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if (declaringClassType ne definedMethod.declaringClassType)
            return baseMethodAllocationFreeness(definedMethod.asDefinedMethod);

        val methodDescriptor = method.descriptor
        val methodName = method.name
        val body = method.body.get
        val instructions = body.instructions
        val maxPC = instructions.length

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty

        var overwritesSelf = false
        var mayOverwriteSelf = true

        var currentPC = 0
        while (currentPC < maxPC) {
            val instruction = instructions(currentPC)
            (instruction.opcode: @switch) match {
                case NEW.opcode | NEWARRAY.opcode | MULTIANEWARRAY.opcode | ANEWARRAY.opcode ⇒
                    return Result(definedMethod, MethodWithAllocations);

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
                                        return Result(definedMethod, MethodWithAllocations);

                                    case epk ⇒
                                        dependees += epk
                                }

                            case _ /* Empty or Failure */ ⇒
                                // We know nothing about the target method (it is not
                                // found in the scope of the current project).
                                return Result(definedMethod, MethodWithAllocations);

                        }
                }

                case ASTORE_0.opcode if !method.isStatic ⇒
                    if (mayOverwriteSelf) overwritesSelf = true
                    else // A PUTFIELD may result in a NPE raised (and therefore allocated)
                        return Result(definedMethod, MethodWithAllocations)

                case PUTFIELD.opcode | GETFIELD.opcode ⇒ // may allocate NPE on non-receiver
                    if (method.isStatic || overwritesSelf)
                        return Result(definedMethod, MethodWithAllocations);
                    else if (instructions(body.pcOfPreviousInstruction(currentPC)).opcode !=
                        ALOAD_0.opcode)
                        return Result(definedMethod, MethodWithAllocations);
                    else mayOverwriteSelf = false

                case INVOKEDYNAMIC.opcode | INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode ⇒
                    // We don't handle these calls here, just treat them as having allocations
                    return Result(definedMethod, MethodWithAllocations);

                case _ ⇒
                    // All other instructions (IFs, Load/Stores, Arith., etc.) allocate no objects
                    // as long as no implicit exceptions are raised.
                    if (instruction.jvmExceptions.nonEmpty) {
                        // JVM Exceptions result in the exception object being allocated.
                        return Result(definedMethod, MethodWithAllocations);
                    }
            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        if (dependees.isEmpty)
            return Result(definedMethod, AllocationFreeMethod);

        // This function computes the “allocation freeness for a method based on the allocation
        // freeness of its callees
        def c(eps: SomeEPS): PropertyComputationResult = {
            // Let's filter the entity.
            dependees = dependees.filter(_.e ne eps.e)

            eps match {
                case FinalEP(_, AllocationFreeMethod) ⇒
                    if (dependees.isEmpty)
                        Result(definedMethod, AllocationFreeMethod)
                    else {
                        IntermediateResult(
                            definedMethod,
                            MethodWithAllocations,
                            AllocationFreeMethod,
                            dependees,
                            c,
                            CheapPropertyComputation
                        )
                    }

                case FinalEP(_, MethodWithAllocations) ⇒
                    Result(definedMethod, MethodWithAllocations)

                case _: IntermediateEP[_, _] ⇒
                    dependees += eps
                    IntermediateResult(
                        definedMethod,
                        MethodWithAllocations,
                        AllocationFreeMethod,
                        dependees,
                        c,
                        CheapPropertyComputation
                    )
            }
        }

        IntermediateResult(
            definedMethod, MethodWithAllocations, AllocationFreeMethod,
            dependees, c, CheapPropertyComputation
        )
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineAllocationFreeness(e: Entity): PropertyComputationResult = {
        e match {
            case m: DefinedMethod  ⇒ determineAllocationFreeness(m)
            case m: DeclaredMethod ⇒ Result(m, MethodWithAllocations)
            case _ ⇒
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
        val declaredMethods = project.get(DeclaredMethodsKey).declaredMethods.collect {
            case dm if dm.hasSingleDefinedMethod && dm.definedMethod.body.isDefined ⇒ dm.asDefinedMethod
        }
        propertyStore.scheduleEagerComputationsForEntities(declaredMethods)(analysis.determineAllocationFreeness)
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
