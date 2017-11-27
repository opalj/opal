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
package properties

import org.opalj.br.collection.{TypesSet ⇒ BRTypesSet}
import org.opalj.br.collection.mutable.{TypesSet ⇒ BRMutableTypesSet}
import org.opalj.br.PC
import org.opalj.br.ObjectType
import org.opalj.br.BooleanType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.SomeProject

/**
 * Specifies for each method the exceptions that are potentially thrown by the respective method.
 * This includes the set of exceptions thrown by called methods (if any). The property '''does not
 * take the exceptions of methods which override the respective method into account'''.
 * Nevertheless, in case of a method call all potential receiver methods are
 * taken into consideration; if the set is unbounded, `ThrownExceptionsAreUnknown` is returned.
 *
 * Note that it may be possible to compute some meaningful upper type bound for the set of
 * thrown exceptions even if methods are called for which the set of thrown exceptions is unknown.
 * This is generally the case if those calls are all done in a try block but the catch/finally
 * blocks only calls known methods - if any.
 * An example is shown next and even if we assume that we don't know
 * the exceptions potentially thrown by `Class.forName` we could still determine that this method
 * will never throw an exception.
 * {{{
 * object Validator {
 *      def isAvailable(s : String) : Boolean = {
 *          try { Class.forName(s); true} finally {return false;}
 *      }
 * }
 * }}}
 *
 * @author Michael Eichberg
 */
sealed abstract class ThrownExceptions extends Property {

    final type Self = ThrownExceptions

    final def key = ThrownExceptions.Key
}

object ThrownExceptions {

    private[this] final val cycleResolutionStrategy = (
        ps: PropertyStore,
        epks: Iterable[SomeEPK]
    ) ⇒ {
        // IMPROVE We should have support to handle cycles of "ThrownExceptions"
        val exceptions = new BRMutableTypesSet(ps.context[SomeProject].classHierarchy)

        var unknownExceptions: String = null
        epks.foreach {
            case EPK(e, ThrownExceptionsByOverridingMethods.Key) ⇒
                ps(e, ThrownExceptionsByOverridingMethods.Key).p match {
                    case UnknownThrownExceptionsByOverridingMethods ⇒
                        unknownExceptions = "Overridden method throw unknown exceptions"
                    case c: AllThrownExceptionsByOverridingMethods ⇒
                        exceptions ++= c.exceptions.concreteTypes
                }

            case EPK(e, Key) ⇒
                ps(e, Key).p match {
                    case u: ThrownExceptionsAreUnknown ⇒ unknownExceptions = u.reason
                    case t: AllThrownExceptions        ⇒ exceptions ++= t.types.concreteTypes
                }
        }

        val p = if (unknownExceptions != null) {
            ThrownExceptionsAreUnknown(unknownExceptions)
        } else if (exceptions.nonEmpty) {
            new AllThrownExceptions(exceptions, false)
        } else {
            NoExceptionsAreThrown.NoInstructionThrowsExceptions
        }

        val e = epks.find(_.pk == Key).get.e
        Iterable(Result(e, p))
    }

    final val Key: PropertyKey[ThrownExceptions] = {
        PropertyKey.create[ThrownExceptions](
            "ThrownExceptions",
            ThrownExceptionsFallbackAnalysis,
            cycleResolutionStrategy
        )
    }
}

class AllThrownExceptions(
        val types:        BRTypesSet,
        val isRefineable: Boolean
) extends ThrownExceptions {

    override def toString: String = s"AllThrownExceptions($types)"

    override def equals(other: Any): Boolean = {
        other match {
            case that: AllThrownExceptions ⇒
                this.types == that.types && this.isRefineable == that.isRefineable
            case _ ⇒ false
        }
    }

    override def hashCode: Int = 13 * types.hashCode + (if (isRefineable) 41 else 53)

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

    final val UnresolvableCycle = {
        ThrownExceptionsAreUnknown("a cycle was detected which the analysis could not resolve")
    }

    final val UnknownExceptionIsThrown = {
        ThrownExceptionsAreUnknown("unable to determine the precise type(s) of a thrown exception")
    }

    final val UnresolvedInvokeDynamic = {
        ThrownExceptionsAreUnknown("the call targets of the unresolved invokedynamic are unknown")
    }

    final val MethodIsNative = ThrownExceptionsAreUnknown("the method is native")

    final val MethodBodyIsNotAvailable = {
        ThrownExceptionsAreUnknown("the method body (of the concrete method) is not available")
    }

    final val UnboundedTargetMethods = {
        ThrownExceptionsAreUnknown("the set of target methods is unbounded/extensible")
    }

    final val AnalysisLimitation = {
        ThrownExceptionsAreUnknown(
            "the analysis is too simple to compute a sound approximation of the thrown exceptions"
        )
    }

    final val SubclassesHaveUnknownExceptions = {
        ThrownExceptionsAreUnknown(
            "one or more subclass throw unknown exceptions"
        )
    }

    final val MethodIsOverrideable = {
        ThrownExceptionsAreUnknown(
            "the method is overrideable by a not yet existing type"
        )
    }

}

/**
 * This property stores information about the exceptions a certain method throw, including
 * the exceptions a possible overridden method in a subclass throws.
 * It uses the ThrownExceptions property to gather information about the exceptions thrown in a
 * particular method. It also includes the thrown exceptions of the respective method in all
 * subclasses.
 *
 * Results can either be `AllThrownExceptionsByOverridingMethods`, which contains a set of possible
 * exceptions thrown in the current classes method or its subclasses. If we aren't able to collect
 * all exceptions, `UnknownThrownExceptionsByOverridingMethods` will be returned. This is the case
 * if the analysis encounters a ATHROW instruction for example.
 *
 * The cycle resolution collects the properties from the given entities and constructs a final
 * result. Possible properties can be `ThrownExceptionsByOverridingMethods` as well as
 * `ThrownExceptions`. The result will be saved in the PropertyStore and propagated to the dependees.
 */
object ThrownExceptionsByOverridingMethods {

    private[this] final val cycleResolutionStrategy = (
        ps: PropertyStore,
        epks: Iterable[SomeEPK]
    ) ⇒ {
        val exceptions = new BRMutableTypesSet(ps.context[SomeProject].classHierarchy)
        var hasUnknownExceptions = false
        epks.foreach {
            case EPK(e, Key) ⇒
                ps(e, Key).p match {
                    case c: AllThrownExceptionsByOverridingMethods ⇒
                        exceptions ++= c.exceptions.concreteTypes
                    case UnknownThrownExceptionsByOverridingMethods ⇒
                        hasUnknownExceptions = true
                    case _ ⇒ throw new UnknownError(s"Cycle involving unknown keys: $e")
                }

            case EPK(e, ThrownExceptions.Key) ⇒
                ps(e, ThrownExceptions.Key).p match {
                    case _: ThrownExceptionsAreUnknown ⇒ hasUnknownExceptions = true
                    case t: AllThrownExceptions        ⇒ exceptions ++= t.types.concreteTypes
                }
        }
        val entity = epks.find(_.pk == Key).get.e
        val p = if (hasUnknownExceptions)
            UnknownThrownExceptionsByOverridingMethods
        else
            AllThrownExceptionsByOverridingMethods(exceptions)

        Iterable(Result(entity, p))
    }

    final val Key: PropertyKey[ThrownExceptionsByOverridingMethods] = {
        PropertyKey.create[ThrownExceptionsByOverridingMethods](
            "ThrownExceptionsByOverridingMethodsProperty",
            AllThrownExceptionsByOverridingMethods(),
            cycleResolutionStrategy
        )
    }
}

sealed abstract class ThrownExceptionsByOverridingMethods extends Property {
    final type Self = ThrownExceptionsByOverridingMethods
    final def key = ThrownExceptionsByOverridingMethods.Key
}

case class AllThrownExceptionsByOverridingMethods(
        exceptions:   BRTypesSet = BRTypesSet.empty,
        isRefineable: Boolean    = false
) extends ThrownExceptionsByOverridingMethods {

    override def equals(other: Any): Boolean = {
        other match {
            case that: AllThrownExceptionsByOverridingMethods ⇒
                this.isRefineable == that.isRefineable &&
                    this.exceptions == that.exceptions
            case _ ⇒ false
        }
    }

    override def hashCode: Int = 13 * exceptions.hashCode +
        (if (isRefineable) 41 else 53)
}

case object UnknownThrownExceptionsByOverridingMethods
    extends ThrownExceptionsByOverridingMethods {
    final val isRefineable = false
}

//
//
// THE FALLBACK/DEFAULT ANALYSIS
//
//

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
object ThrownExceptionsFallbackAnalysis extends ((PropertyStore, Entity) ⇒ ThrownExceptions) {

    final val ObjectEqualsMethodDescriptor = MethodDescriptor(ObjectType.Object, BooleanType)

    def apply(ps: PropertyStore, e: Entity): ThrownExceptions = {
        e match { case m: Method ⇒ this(ps, m) }
    }

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
        val cfJoins = code.cfJoins
        val instructions = code.instructions
        val isStaticMethod = m.isStatic

        val exceptions = new BRMutableTypesSet(ps.context[SomeProject].classHierarchy)

        var result: ThrownExceptionsAreUnknown = null

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

                case ATHROW.opcode ⇒
                    result = ThrownExceptionsAreUnknown.UnknownExceptionIsThrown
                    false
                case INVOKESPECIAL.opcode ⇒
                    val INVOKESPECIAL(declaringClass, _, name, descriptor) = instruction
                    if ((declaringClass eq ObjectType.Object) && (
                        (name == "<init>" && descriptor == MethodDescriptor.NoArgsAndReturnVoid) ||
                        (name == "hashCode" && descriptor == MethodDescriptor.JustReturnsInteger) ||
                        (name == "equals" && descriptor == ObjectEqualsMethodDescriptor) ||
                        (name == "toString" && descriptor == MethodDescriptor.JustReturnsString)
                    )) {
                        true
                    } else {
                        result = ThrownExceptionsAreUnknown.AnalysisLimitation
                        false
                    }

                case INVOKEDYNAMIC.opcode ⇒
                    result = ThrownExceptionsAreUnknown.UnresolvedInvokeDynamic
                    false

                case INVOKESTATIC.opcode | INVOKEINTERFACE.opcode | INVOKEVIRTUAL.opcode ⇒
                    result = ThrownExceptionsAreUnknown.AnalysisLimitation
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
            NoExceptionsAreThrown.NoInstructionThrowsExceptions
        else
            new AllThrownExceptions(exceptions, false)
    }

}

class ThrownExceptionsFallbackAnalysis(ps: PropertyStore) extends PropertyComputation[Method] {

    def apply(m: Method): PropertyComputationResult = {
        ImmediateResult(m, ThrownExceptionsFallbackAnalysis(ps, m))
    }

}
