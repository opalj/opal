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

import org.opalj.br.collection.mutable.{TypesSet ⇒ BRMutableTypesSet}
import org.opalj.br.PC
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.instructions._
import org.opalj.fpcf.properties._

/**
 * Transitive analysis of thrown exceptions
 * [[org.opalj.fpcf.properties.ThrownExceptions]] property.
 *
 * @author Andreas Muttscheller
 */
class L1ThrownExceptionsAnalysis private (
        final val project: SomeProject
) extends FPCFAnalysis {

    private def lazilyDetermineThrownExceptions(e: Entity): PropertyComputationResult = {
        e match {
            case m: Method ⇒
                determineThrownExceptions(m)
            case e ⇒
                val m = s"the ThrownExceptions property is only defined for methods; found $e"
                throw new UnknownError(m)
        }
    }

    /**
     * Determines the exceptions a method throws. This analysis also follows invocation instructions
     * and adds the exceptions thrown by the called method into its own result.
     * The given method must have a body!
     */
    def determineThrownExceptions(m: Method): PropertyComputationResult = {
        if (m.isNative)
            return ImmediateResult(m, ThrownExceptionsAreUnknown.MethodIsNative);
        if (m.isAbstract)
            return ImmediateResult(m, NoExceptionsAreThrown.MethodIsAbstract);
        val body = m.body
        if (body.isEmpty)
            return ImmediateResult(m, ThrownExceptionsAreUnknown.MethodBodyIsNotAvailable);

        // If an unknown subclass can override this method we cannot gather information about
        // the thrown exceptions. Return the analysis immediately.
        if (project.get(IsOverridableMethodKey)(m).isYes) {
            return Result(m, UnknownThrownExceptionsByOverridingMethods);
        }

        //
        //... when we reach this point the method is non-empty
        //
        val code = body.get
        val cfJoins = code.cfJoins
        val instructions = code.instructions
        val isStaticMethod = m.isStatic

        val exceptions = new BRMutableTypesSet(ps.context[SomeProject].classHierarchy)

        var result: ThrownExceptions = null

        var isSynchronizationUsed = false

        var isLocalVariable0Updated = false
        var fieldAccessMayThrowNullPointerException = false
        var isFieldAccessed = false

        var dependees = Set.empty[EOptionP[Method, Property]]

        /* Implicitly (i.e., as a side effect) collects the thrown exceptions in the exceptions set.
         *
         * @return `true` if it is possible to collect all potentially thrown exceptions.
         */
        def collectAllExceptions(pc: PC, instruction: Instruction): Boolean = {
            instruction.opcode match {

                case ATHROW.opcode ⇒
                    result = ThrownExceptionsAreUnknown.UnknownExceptionIsThrown
                    false
                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode ⇒
                    val MethodInvocationInstruction(declaringClass, _, name, descriptor) = instruction
                    if ((declaringClass eq ObjectType.Object) && (
                        (name == "<init>" && descriptor == MethodDescriptor.NoArgsAndReturnVoid) ||
                        (name == "hashCode" && descriptor == MethodDescriptor.JustReturnsInteger) ||
                        (name == "equals" && descriptor == ThrownExceptionsFallbackAnalysis.ObjectEqualsMethodDescriptor) ||
                        (name == "toString" && descriptor == MethodDescriptor.JustReturnsString)
                    )) {
                        true
                    } else {
                        instruction match {
                            case mii: NonVirtualMethodInvocationInstruction ⇒
                                project.nonVirtualCall(mii) match {
                                    case Success(callee) ⇒
                                        val thrownExceptions = ps(callee, ThrownExceptions.Key)

                                        thrownExceptions match {
                                            case EP(_, NoExceptionsAreThrown.NoInstructionThrowsExceptions) ⇒
                                                true
                                            case EP(_, e: ThrownExceptionsAreUnknown) ⇒
                                                result = e
                                                false
                                            // Handling cyclic computations
                                            case epk ⇒
                                                dependees += epk
                                                true
                                        }
                                    case _ ⇒
                                        result = ThrownExceptionsAreUnknown.UnknownExceptionIsThrown
                                        false
                                }
                            case _ ⇒
                                result = ThrownExceptionsAreUnknown.UnknownExceptionIsThrown
                                false
                        }
                    }

                case INVOKEDYNAMIC.opcode ⇒
                    result = ThrownExceptionsAreUnknown.UnresolvedInvokeDynamic
                    false

                case INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode ⇒
                    val callerPackage = m.classFile.thisType.packageName
                    val callees = instruction match {
                        case iv: INVOKEVIRTUAL   ⇒ project.virtualCall(callerPackage, iv)
                        case ii: INVOKEINTERFACE ⇒ project.interfaceCall(ii)
                        case _                   ⇒ Set.empty[Method]
                    }
                    result = NoExceptionsAreThrown.NoInstructionThrowsExceptions
                    callees.foreach { callee ⇒
                        // Check the classhierarchy for thrown exceptions
                        val classHierarchy = ps(callee, properties.ThrownExceptionsByOverridingMethods.Key)
                        classHierarchy match {
                            case EP(_, AllThrownExceptionsByOverridingMethods(e, r)) ⇒
                                exceptions ++= e.concreteTypes
                                if (r)
                                    dependees += classHierarchy
                            case EP(_, UnknownThrownExceptionsByOverridingMethods) ⇒
                                result = ThrownExceptionsAreUnknown.SubclassesHaveUnknownExceptions
                            case epk ⇒ // Not yet computed
                                dependees += epk
                        }
                    }
                    callees.nonEmpty

                // let's determine if the register 0 is updated (i.e., if the register which
                // stores the this reference in case of instance methods is updated)
                case ISTORE_0.opcode | LSTORE_0.opcode |
                    DSTORE_0.opcode | FSTORE_0.opcode |
                    ASTORE_0.opcode ⇒
                    isLocalVariable0Updated = true
                    true

                case ISTORE.opcode | LSTORE.opcode |
                    FSTORE.opcode | DSTORE.opcode |
                    ASTORE.opcode ⇒
                    val lvIndex = instruction.indexOfWrittenLocal
                    if (lvIndex == 0) isLocalVariable0Updated = true
                    true

                case GETFIELD.opcode ⇒
                    isFieldAccessed = true
                    fieldAccessMayThrowNullPointerException ||=
                        isStaticMethod || // <= the receiver is some object
                        isLocalVariable0Updated || // <= we don't know the receiver object at all
                        cfJoins.contains(pc) || // <= we cannot locally decide who is the receiver
                        instructions(code.pcOfPreviousInstruction(pc)) != ALOAD_0 // <= the receiver may be null..
                    true

                case PUTFIELD.opcode ⇒
                    isFieldAccessed = true
                    fieldAccessMayThrowNullPointerException = fieldAccessMayThrowNullPointerException ||
                        isStaticMethod || // <= the receiver is some object
                        isLocalVariable0Updated || // <= we don't know the receiver object at all
                        cfJoins.contains(pc) || // <= we cannot locally decide who is the receiver
                        {
                            val predecessorPC = code.pcOfPreviousInstruction(pc)
                            val predecessorOfPredecessorPC = code.pcOfPreviousInstruction(predecessorPC)
                            val valueInstruction = instructions(predecessorPC)

                            instructions(predecessorOfPredecessorPC) != ALOAD_0 || // <= the receiver may be null..
                                valueInstruction.isInstanceOf[StackManagementInstruction] ||
                                // we have to ensure that our "this" reference is not used for something else... =>
                                valueInstruction.numberOfPoppedOperands(NotRequired) > 0
                            // the number of pushed operands is always equal or smaller than 1
                            // except of the stack management instructions
                        }
                    true

                case MONITORENTER.opcode | MONITOREXIT.opcode ⇒
                    exceptions ++= instruction.jvmExceptions
                    isSynchronizationUsed = true
                    true
                case IRETURN.opcode | LRETURN.opcode |
                    FRETURN.opcode | DRETURN.opcode |
                    ARETURN.opcode | RETURN.opcode ⇒
                    // let's forget about the IllegalMonitorStateException for now unless we have
                    // a MONITORENTER/MONITOREXIT instruction
                    true

                case IREM.opcode | IDIV.opcode ⇒
                    if (!cfJoins.contains(pc)) {
                        val predecessorPC = code.pcOfPreviousInstruction(pc)
                        val valueInstruction = instructions(predecessorPC)
                        valueInstruction match {
                            case (i: LoadConstantInstruction[Int] @unchecked) if i.value != 0 ⇒
                                // there will be no arithmetic exception
                                true
                            case _ ⇒
                                exceptions ++= instruction.jvmExceptions
                                true
                        }
                    } else {
                        exceptions ++= instruction.jvmExceptions
                        true
                    }

                case LREM.opcode | LDIV.opcode ⇒
                    if (!cfJoins.contains(pc)) {
                        val predecessorPC = code.pcOfPreviousInstruction(pc)
                        val valueInstruction = instructions(predecessorPC)
                        valueInstruction match {
                            case (i: LoadConstantInstruction[Long] @unchecked) if i.value != 0L ⇒
                                // there will be no arithmetic exception
                                true
                            case _ ⇒
                                exceptions ++= instruction.jvmExceptions
                                true
                        }
                    } else {
                        exceptions ++= instruction.jvmExceptions
                        true
                    }

                case _ /* all other instructions */ ⇒
                    exceptions ++= instruction.jvmExceptions
                    true
            }
        }

        val areAllExceptionsCollected = code.forall(collectAllExceptions)

        if (!areAllExceptionsCollected) {
            assert(result ne null)
            return Result(m, result);
        }
        if (fieldAccessMayThrowNullPointerException ||
            (isFieldAccessed && isLocalVariable0Updated)) {
            exceptions += ObjectType.NullPointerException
        }
        if (isSynchronizationUsed) {
            exceptions += ObjectType.IllegalMonitorStateException
        }

        def c(e: Entity, p: Property, ut: UserUpdateType): PropertyComputationResult = {
            p match {
                case e: NoExceptionsAreThrown ⇒
                    if (exceptions.isEmpty)
                        Result(m, e)
                    else {
                        Result(m, new AllThrownExceptions(exceptions, false))
                    }

                case thrownExceptions: AllThrownExceptions ⇒
                    dependees = dependees.filter {
                        _.e ne e
                    }
                    exceptions ++= thrownExceptions.types.concreteTypes
                    if (dependees.isEmpty)
                        if (exceptions.isEmpty) {
                            Result(m, NoExceptionsAreThrown.NoInstructionThrowsExceptions)
                        } else {
                            Result(m, new AllThrownExceptions(exceptions, false))
                        }
                    else
                        IntermediateResult(m, new AllThrownExceptions(exceptions, true), dependees, c)

                case u: ThrownExceptionsAreUnknown ⇒
                    Result(m, u)

                case AllThrownExceptionsByOverridingMethods(ex, r) ⇒
                    exceptions ++= ex.concreteTypes
                    if (!r) {
                        dependees = dependees.filter { _.e ne e }
                        if (dependees.isEmpty && exceptions.isEmpty) {
                            Result(m, NoExceptionsAreThrown.NoInstructionThrowsExceptions)
                        } else {
                            Result(m, new AllThrownExceptions(exceptions, false))
                        }
                    } else {
                        IntermediateResult(m, new AllThrownExceptions(exceptions, true), dependees, c)
                    }

                case UnknownThrownExceptionsByOverridingMethods ⇒
                    Result(m, ThrownExceptionsAreUnknown.SubclassesHaveUnknownExceptions)
            }
        }

        if (dependees.isEmpty) {
            if (exceptions.isEmpty)
                Result(m, NoExceptionsAreThrown.NoInstructionThrowsExceptions)
            else
                Result(m, new AllThrownExceptions(exceptions, false))
        } else {
            IntermediateResult(m, new AllThrownExceptions(exceptions, true), dependees, c)
        }
    }
}

/**
 * Factory and runner for the [[L1ThrownExceptionsAnalysis]].
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object L1ThrownExceptionsAnalysis extends FPCFAnalysisScheduler {

    override def usedProperties: Set[PropertyKind] = {
        Set(properties.ThrownExceptionsByOverridingMethods.Key)
    }

    override def derivedProperties: Set[PropertyKind] = Set(ThrownExceptions.Key)

    /**
     * Eagerly schedules the computation of the thrown exceptions for all methods with bodies;
     * in general, the analysis is expected to be registered as a lazy computation.
     */
    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L1ThrownExceptionsAnalysis(project)
        val allMethods = project.allMethodsWithBody
        propertyStore.scheduleForEntities(allMethods)(analysis.determineThrownExceptions)
        analysis
    }

    /** Registers an analysis to compute the thrown exceptions lazily. */
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L1ThrownExceptionsAnalysis(project)
        propertyStore.scheduleLazyComputation[ThrownExceptions](
            ThrownExceptions.Key,
            analysis.lazilyDetermineThrownExceptions
        )
        analysis
    }
}
