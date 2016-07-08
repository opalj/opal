/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package properties

//import scala.collection.Set
import org.opalj.fpcf.PropertyComputation
import org.opalj.br.collection.{TypesSet ⇒ BRTypesSet}
import org.opalj.br.collection.mutable.{TypesSet ⇒ BRMutableTypesSet}
import org.opalj.br.PC
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions._

/**
 * Determines for each method the exceptions that are potentially thrown by the respective method.
 * This includes the set of exceptions thrown by called methods (if any). The property does not
 * take the exceptions which are potentially thrown by the methods which override the respective
 * method into account. Nevertheless, in case of a method call all potental receiver methods are
 * taken into consideration.
 *
 * Note that it may be possible to compute some meaningful upper type bound for the set of
 * thrown exceptions even if methods are called for which the set of thrown exceptions is unknown.
 * This is generally the case if those calls are all done in a try block but the catch/finally
 * blocks only call known methods - if any.
 * An example is shown next and even if we assume that we don't know
 * the exceptions potentially thrown by `Class.forName` we could still determine that this method
 * will never throw an exception.
 * {{{
 * object Validator {
 * 	def isAvailable(s : String) : Boolean = {
 * 		try { Class.forName(s); true} finally {return false;}
 *  }
 * }
 * }}}
 */
sealed trait ThrownExceptions extends Property {

    final type Self = ThrownExceptions

    final def key = ThrownExceptions.Key
}

object ThrownExceptions {

    final val Key = {
        PropertyKey.create[ThrownExceptions](
            "ThrownExceptions",
            ThrownExceptionsFallbackAnalysis,
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒
                ((throw new UnknownError("internal error")): Iterable[PropertyComputationResult])
        )
    }
}

object ThrownExceptionsFallbackAnalysis extends ((PropertyStore, Entity) ⇒ ThrownExceptions) {

    def apply(ps: PropertyStore, e: Entity): ThrownExceptions = {
        e match { case m: Method ⇒ this(ps, m) }
    }

    /**
     * A very straight forward flow-insensitive analysis which can successfully analyze methods
     * with respect to the potentially thrown exceptions under the conditions that no other
     * methods are invoked and that no exceptions are thrown (`ATHROW`). This analysis always
     * computes a sound overapproximation of the potentially thrown exceptions.
     *
     * The analysis has limited support for the following cases to be more precise in case of
     * common code patterns (e.g., a standard getter):
     *  - If all instance based field reads are using the self reference "this" and
     *    "this" is used in the expected manner the [[org.opalj.br.instructions.GETFIELD]]
     *  - If no [[org.opalj.br.instructions.MONITORENTER]]/[[org.opalj.br.instructions.MONITOREXIT]]
     *    instructions are found, the return instructions will not throw
     *    `IllegalMonitorStateException`s.
     *
     * Hence, the primary use case of this method is to identify those methods that are guaranteed
     * to never throw exceptions which dramatically helps other analysis.
     */
    def apply(ps: PropertyStore, m: Method): ThrownExceptions = {
        if (m.isNative)
            return ThrownExceptionsAreUnknown.MethodIsNative;
        if (m.isAbstract)
            return NoExceptionsAreThrown.MethodIsAbstract;
        val body = m.body
        if (body.isEmpty)
            return ThrownExceptionsAreUnknown.MethodBodyIsNotAvailable;

        //
        //... when we reach this point the method is non-empty
        //
        val code = body.get
        val joinInstructions = code.joinInstructions
        val instructions = code.instructions
        val isStaticMethod = m.isStatic

        val exceptions = new BRMutableTypesSet(ps.context[SomeProject].classHierarchy)

        var result: ThrownExceptionsAreUnknown = null

        var isSynchronizationUsed = false

        var isLocalVariable0Updated = false
        var fielAccessMayThrowNullPointerException = false
        var isFieldAccessed = false

        def collectAllExceptions(pc: PC, instruction: Instruction): Boolean = {
            instruction.opcode match {

                case ATHROW.opcode ⇒
                    result = ThrownExceptionsAreUnknown.UnknownExceptionIsThrown
                    false
                case INVOKEDYNAMIC.opcode |
                    INVOKESPECIAL.opcode | INVOKESTATIC.opcode |
                    INVOKEINTERFACE.opcode | INVOKEVIRTUAL.opcode ⇒
                    result = ThrownExceptionsAreUnknown.SomeCallerThrowsUnknownExceptions
                    false

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
                    if (instruction.asInstanceOf[StoreLocalVariableInstruction].lvIndex == 0)
                        isLocalVariable0Updated = true
                    true
                case GETFIELD.opcode ⇒
                    isFieldAccessed = true
                    fielAccessMayThrowNullPointerException = fielAccessMayThrowNullPointerException ||
                        isStaticMethod || // <= the receiver is some object
                        isLocalVariable0Updated || // <= we don't know the receiver object at all
                        joinInstructions.contains(pc) || // <= we cannot locally decide who is the receiver 
                        instructions(code.pcOfPreviousInstruction(pc)) != ALOAD_0 // <= the receiver may be null.. 
                    true

                case PUTFIELD.opcode ⇒
                    isFieldAccessed = true
                    fielAccessMayThrowNullPointerException = fielAccessMayThrowNullPointerException ||
                        isStaticMethod || // <= the receiver is some object
                        isLocalVariable0Updated || // <= we don't know the receiver object at all
                        joinInstructions.contains(pc) || // <= we cannot locally decide who is the receiver 
                        {
                            val predecessorPC = code.pcOfPreviousInstruction(pc)
                            val predecessorOfPredecessorPC = code.pcOfPreviousInstruction(predecessorPC)
                            val valueInstruction = instructions(predecessorPC)

                            instructions(predecessorOfPredecessorPC) != ALOAD_0 || // <= the receiver may be null..
                                valueInstruction.isInstanceOf[StackManagementInstruction] ||
                                // we have to ensure that our "this" reference is not used for something else... =>
                                valueInstruction.numberOfPoppedOperands { idx ⇒ throw new UnknownError } > 0
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

                case i ⇒
                    exceptions ++= instruction.jvmExceptions
                    true
            }
        }
        val areAllExceptionsCollected = code.forall(collectAllExceptions)
        if (fielAccessMayThrowNullPointerException || (isFieldAccessed && isLocalVariable0Updated)) {
            exceptions += ObjectType.NullPointerException
        }
        if (isSynchronizationUsed) {
            exceptions += ObjectType.IllegalMonitorStateException
        }

        if (areAllExceptionsCollected) {
            assert(result eq null)
            if (exceptions.isEmpty)
                NoExceptionsAreThrown.NoInstructionThrowsExceptions
            else
                new AllThrownExceptions(exceptions, false)
        } else {
            assert(result ne null)
            result
        }
    }

}

class ThrownExceptionsFallbackAnalysis(ps: PropertyStore) extends PropertyComputation[Method] {
    def apply(m: Method): PropertyComputationResult = {
        ImmediateResult(m, ThrownExceptionsFallbackAnalysis(ps, m))
    }
}

sealed class AllThrownExceptions(
        val types:        BRTypesSet,
        val isRefineable: Boolean
) extends ThrownExceptions {

    override def toString: String = s"AllThrownExceptions($types)"
}

final case class NoExceptionsAreThrown(
        explanation: String
) extends AllThrownExceptions(BRTypesSet.empty, isRefineable = false) {
    override def toString: String = s"NoExceptionsAreThrown($explanation)"
}

object NoExceptionsAreThrown {

    final val NoInstructionThrowsExceptions = {
        NoExceptionsAreThrown("none of the instructions of the method throws an exception")
    }

    final val MethodIsAbstract = NoExceptionsAreThrown("method is abstract")
}

final case class ThrownExceptionsAreUnknown(reason: String) extends ThrownExceptions {

    def isRefineable: Boolean = false

}

object ThrownExceptionsAreUnknown {

    final val UnknownExceptionIsThrown = {
        ThrownExceptionsAreUnknown("the precise type(s) of a thrown exception could not be determined")
    }

    final val SomeCallerThrowsUnknownExceptions = {
        ThrownExceptionsAreUnknown("called method throws unknown exceptions")
    }

    final val MethodIsNative = {
        ThrownExceptionsAreUnknown("the method is native")
    }

    final val MethodBodyIsNotAvailable = {
        ThrownExceptionsAreUnknown("the method body is not available")
    }
}

