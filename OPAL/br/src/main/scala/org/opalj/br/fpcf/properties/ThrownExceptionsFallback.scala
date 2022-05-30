/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.PropertyComputation
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.br.analyses.Project
import org.opalj.br.collection.mutable.{TypesSet => BRMutableTypesSet}
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodBodyIsNotAvailable
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodIsNative
import org.opalj.br.fpcf.properties.ThrownExceptions.NoExceptions
import org.opalj.br.instructions.ALOAD_0
import org.opalj.br.instructions.ARETURN
import org.opalj.br.instructions.ASTORE
import org.opalj.br.instructions.ASTORE_0
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.DRETURN
import org.opalj.br.instructions.DSTORE
import org.opalj.br.instructions.DSTORE_0
import org.opalj.br.instructions.FRETURN
import org.opalj.br.instructions.FSTORE
import org.opalj.br.instructions.FSTORE_0
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.IDIV
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.IREM
import org.opalj.br.instructions.IRETURN
import org.opalj.br.instructions.ISTORE
import org.opalj.br.instructions.ISTORE_0
import org.opalj.br.instructions.LDCInt
import org.opalj.br.instructions.LDIV
import org.opalj.br.instructions.LoadLong
import org.opalj.br.instructions.LREM
import org.opalj.br.instructions.LRETURN
import org.opalj.br.instructions.LSTORE
import org.opalj.br.instructions.LSTORE_0
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.MONITOREXIT
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.RETURN
import org.opalj.br.instructions.StackManagementInstruction

/**
 * A very straight forward flow-insensitive analysis which can successfully analyze methods
 * with respect to the potentially thrown exceptions under the conditions that no other
 * methods are invoked and that no exceptions are explicitly thrown (`ATHROW`). This analysis
 * always computes a sound over approximation of the potentially thrown exceptions.
 *
 * The analysis has limited support for the following cases to be more precise in case of
 * common code patterns (e.g., a standard getter):
 *  - If all instance based field reads are using the self reference "this" and
 *    "this" is used in the expected manner
 *  - If no [[org.opalj.br.instructions.MONITORENTER]]/[[org.opalj.br.instructions.MONITOREXIT]]
 *    instructions are found, the return instructions will not throw
 *    `IllegalMonitorStateException`s.
 *
 * Hence, the primary use case of this method is to identify those methods that are guaranteed
 * to '''never throw exceptions'''.
 */
object ThrownExceptionsFallback extends ((PropertyStore, FallbackReason, Entity) => ThrownExceptions) {

    final val ObjectEqualsMethodDescriptor = MethodDescriptor(ObjectType.Object, BooleanType)

    def apply(ps: PropertyStore, reason: FallbackReason, e: Entity): ThrownExceptions = {
        e match { case m: Method => this(ps, m) }
    }

    def apply(ps: PropertyStore, e: Entity): ThrownExceptions = {
        e match { case m: Method => this(ps, m) }
    }

    def apply(ps: PropertyStore, m: Method): ThrownExceptions = {
        if (m.isNative)
            return MethodIsNative;
        if (m.isAbstract)
            return NoExceptions; // Method is abstract...
        val body = m.body
        if (body.isEmpty)
            return MethodBodyIsNotAvailable;

        //
        //... when we reach this point the method is non-empty
        //
        val code = body.get
        val cfJoins = code.cfJoins
        val instructions = code.instructions
        val isStaticMethod = m.isStatic

        val exceptions = new BRMutableTypesSet(ps.context(classOf[Project[_]]).classHierarchy)

        var result: ThrownExceptions = null

        var isSynchronizationUsed = false

        var isLocalVariable0Updated = false
        var fieldAccessMayThrowNullPointerException = false
        var isFieldAccessed = false

        /* Implicitly (i.e., as a side effect) collects the thrown exceptions in the exceptions set.
         *
         * @return `true` if it is possible to collect all potentially thrown exceptions.
         */
        def collectAllExceptions(pc: PC, instruction: Instruction): Boolean = {
            instruction.opcode match {

                case ATHROW.opcode =>
                    result = ThrownExceptions.UnknownExceptionIsThrown
                    false
                case INVOKESPECIAL.opcode =>
                    val INVOKESPECIAL(declaringClass, _, name, descriptor) = instruction
                    if ((declaringClass eq ObjectType.Object) && (
                        (name == "<init>" && descriptor == MethodDescriptor.NoArgsAndReturnVoid) ||
                        (name == "hashCode" && descriptor == MethodDescriptor.JustReturnsInteger) ||
                        (name == "equals" && descriptor == ObjectEqualsMethodDescriptor) ||
                        (name == "toString" && descriptor == MethodDescriptor.JustReturnsString)
                    )) {
                        true
                    } else {
                        result = ThrownExceptions.AnalysisLimitation
                        false
                    }

                case INVOKEDYNAMIC.opcode =>
                    // if the bytecode is valid, the creation of the call site object should
                    // succeed...
                    true

                case INVOKESTATIC.opcode | INVOKEINTERFACE.opcode | INVOKEVIRTUAL.opcode =>
                    result = ThrownExceptions.AnalysisLimitation
                    false

                // let's determine if the register 0 is updated (i.e., if the register which
                // stores the this reference in case of instance methods is updated)
                case ISTORE_0.opcode | LSTORE_0.opcode |
                    DSTORE_0.opcode | FSTORE_0.opcode |
                    ASTORE_0.opcode =>
                    isLocalVariable0Updated = true
                    true
                case ISTORE.opcode | LSTORE.opcode |
                    FSTORE.opcode | DSTORE.opcode |
                    ASTORE.opcode =>
                    val lvIndex = instruction.indexOfWrittenLocal
                    if (lvIndex == 0) isLocalVariable0Updated = true
                    true

                case GETFIELD.opcode =>
                    isFieldAccessed = true
                    fieldAccessMayThrowNullPointerException ||=
                        isStaticMethod || // <= the receiver is some object
                        isLocalVariable0Updated || // <= we don't know the receiver object at all
                        cfJoins.contains(pc) || // <= we cannot locally decide who is the receiver
                        instructions(code.pcOfPreviousInstruction(pc)) != ALOAD_0 // <= the receiver may be null..
                    true

                case PUTFIELD.opcode =>
                    isFieldAccessed = true
                    fieldAccessMayThrowNullPointerException =
                        fieldAccessMayThrowNullPointerException ||
                            isStaticMethod || // <= the receiver is some object
                            isLocalVariable0Updated || // <= we don't know the receiver object at all
                            cfJoins.contains(pc) || // <= we cannot locally decide who is the receiver
                            {
                                val predecessorPC = code.pcOfPreviousInstruction(pc)
                                val predecessorOfPredecessorPC =
                                    code.pcOfPreviousInstruction(predecessorPC)
                                val valueInstruction = instructions(predecessorPC)

                                instructions(predecessorOfPredecessorPC) != ALOAD_0 || // <= the receiver may be null..
                                    valueInstruction.isInstanceOf[StackManagementInstruction] ||
                                    // we have to ensure that our "this" reference is not used for something else... =>
                                    valueInstruction.numberOfPoppedOperands(NotRequired) > 0
                                // the number of pushed operands is always equal or smaller than 1
                                // except of the stack management instructions
                            }
                    true

                case MONITORENTER.opcode | MONITOREXIT.opcode =>
                    exceptions ++= instruction.jvmExceptions
                    isSynchronizationUsed = true
                    true
                case IRETURN.opcode | LRETURN.opcode |
                    FRETURN.opcode | DRETURN.opcode |
                    ARETURN.opcode | RETURN.opcode =>
                    // let's forget about the IllegalMonitorStateException for now unless we have
                    // a MONITORENTER/MONITOREXIT instruction
                    true

                case IREM.opcode | IDIV.opcode =>
                    if (!cfJoins.contains(pc)) {
                        val predecessorPC = code.pcOfPreviousInstruction(pc)
                        val valueInstruction = instructions(predecessorPC)
                        valueInstruction match {
                            case LDCInt(value) if value != 0 =>
                                // there will be no arithmetic exception
                                true
                            case _ =>
                                exceptions ++= instruction.jvmExceptions
                                true
                        }
                    } else {
                        exceptions ++= instruction.jvmExceptions
                        true
                    }

                case LREM.opcode | LDIV.opcode =>
                    if (!cfJoins.contains(pc)) {
                        val predecessorPC = code.pcOfPreviousInstruction(pc)
                        val valueInstruction = instructions(predecessorPC)
                        valueInstruction match {
                            case LoadLong(value) if value != 0L =>
                                // there will be no arithmetic exception
                                true
                            case _ =>
                                exceptions ++= instruction.jvmExceptions
                                true
                        }
                    } else {
                        exceptions ++= instruction.jvmExceptions
                        true
                    }

                case _ /* all other instructions */ =>
                    exceptions ++= instruction.jvmExceptions
                    true
            }
        }
        val areAllExceptionsCollected = code.forall(collectAllExceptions(_, _))
        if (!areAllExceptionsCollected) {
            assert(result ne null)
            return result;
        }
        if (fieldAccessMayThrowNullPointerException ||
            (isFieldAccessed && isLocalVariable0Updated)) {
            exceptions += ObjectType.NullPointerException
        }
        if (isSynchronizationUsed) {
            exceptions += ObjectType.IllegalMonitorStateException
        }

        if (exceptions.isEmpty)
            NoExceptions
        else
            ThrownExceptions(exceptions)
    }

}

class ThrownExceptionsFallback(ps: PropertyStore) extends PropertyComputation[Method] {

    def apply(m: Method): PropertyComputationResult = {
        Result(m, ThrownExceptionsFallback(ps, m))
    }

}
