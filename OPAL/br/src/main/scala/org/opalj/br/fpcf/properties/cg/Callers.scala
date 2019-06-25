/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package cg

import org.opalj.collection.GrowableLongSet
import org.opalj.collection.immutable.LongLinkedTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.DeclaredMethods

/**
 * For a given [[org.opalj.br.DeclaredMethod]], and for each call site (represented by the PC),
 * the set of methods that are possible call targets.
 *
 * @author Florian Kuebler
 */
sealed trait CallersPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Callers
}

sealed trait Callers extends OrderedProperty with CallersPropertyMetaInformation {

    def hasCallersWithUnknownContext: Boolean

    def hasVMLevelCallers: Boolean

    def size: Int

    def isEmpty: Boolean

    def nonEmpty: Boolean

    def callers(
        implicit
        declaredMethods: DeclaredMethods
    ): TraversableOnce[(DeclaredMethod, Int /*PC*/ , Boolean /*isDirect*/ )]

    /**
     * Returns a new callers object, containing all callers of `this` object and a call from
     * `caller` at program counter `pc`.
     *
     * In case, the specified call is already contained, `this` is returned, i.e. the reference does
     * not change when the of callers set remains unchanged.
     */
    def updated(caller: DeclaredMethod, pc: Int, isDirect: Boolean): Callers

    def updatedWithUnknownContext(): Callers

    def updatedWithVMLevelCall(): Callers

    override def toString: String = {
        s"Callers(size=${this.size})"
    }

    final def key: PropertyKey[Callers] = Callers.key

    override def checkIsEqualOrBetterThan(e: Entity, other: Callers): Unit = {
        if (other.size < size)
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
    }
}

sealed trait CallersWithoutUnknownContext extends Callers {
    final override def hasCallersWithUnknownContext: Boolean = false
}

sealed trait CallersWithUnknownContext extends Callers {
    final override def hasCallersWithUnknownContext: Boolean = true
    final override def updatedWithUnknownContext(): CallersWithUnknownContext = this
}

sealed trait CallersWithVMLevelCall extends Callers {
    final override def hasVMLevelCallers: Boolean = true
    final override def updatedWithVMLevelCall(): CallersWithVMLevelCall = this
}

sealed trait CallersWithoutVMLevelCall extends Callers {
    final override def hasVMLevelCallers: Boolean = false
}

sealed trait EmptyConcreteCallers extends Callers {
    final override def size: Int = 0

    final override def isEmpty: Boolean = true

    final override def nonEmpty: Boolean = false

    final override def callers(
        implicit
        declaredMethods: DeclaredMethods
    ): TraversableOnce[(DeclaredMethod, Int, Boolean)] = {
        Nil
    }

    final override def updated(
        caller: DeclaredMethod, pc: Int, isDirect: Boolean
    ): Callers = {
        val set = LongLinkedTrieSet(Callers.toLong(caller.id, pc, isDirect))

        if (!hasCallersWithUnknownContext && !hasVMLevelCallers) {
            new CallersOnlyWithConcreteCallers(set)
        } else {
            CallersImplWithOtherCalls(set, hasVMLevelCallers, hasCallersWithUnknownContext)
        }
    }
}

object NoCallers
    extends EmptyConcreteCallers with CallersWithoutUnknownContext with CallersWithoutVMLevelCall {
    override def updatedWithVMLevelCall(): CallersWithVMLevelCall = OnlyVMLevelCallers

    override def updatedWithUnknownContext(): CallersWithUnknownContext = OnlyCallersWithUnknownContext
}

object OnlyCallersWithUnknownContext
    extends EmptyConcreteCallers with CallersWithUnknownContext with CallersWithoutVMLevelCall {
    override def updatedWithVMLevelCall(): CallersWithVMLevelCall = OnlyVMCallersAndWithUnknownContext
}

object OnlyVMLevelCallers
    extends EmptyConcreteCallers with CallersWithoutUnknownContext with CallersWithVMLevelCall {
    override def updatedWithUnknownContext(): CallersWithUnknownContext = OnlyVMCallersAndWithUnknownContext
}

object OnlyVMCallersAndWithUnknownContext
    extends EmptyConcreteCallers with CallersWithVMLevelCall with CallersWithUnknownContext

sealed trait CallersImplementation extends Callers {
    val encodedCallers: GrowableLongSet[_] /* MethodId + PC*/
    final override def size: Int = encodedCallers.size

    final override def isEmpty: Boolean = encodedCallers.isEmpty

    final override def nonEmpty: Boolean = encodedCallers.nonEmpty

    final override def callers(
        implicit
        declaredMethods: DeclaredMethods
    ): TraversableOnce[(DeclaredMethod, Int /*PC*/ , Boolean /*isDirect*/ )] = {
        encodedCallers.iterator.map { encodedPair ⇒
            val (mId, pc, isDirect) = Callers.toMethodPcAndIsDirect(encodedPair)
            (declaredMethods(mId), pc, isDirect)
        }
    }
}

class CallersOnlyWithConcreteCallers(
        val encodedCallers: GrowableLongSet[_] /*MethodId + PC*/
) extends CallersImplementation with CallersWithoutVMLevelCall with CallersWithoutUnknownContext {

    override def updated(
        caller: DeclaredMethod, pc: Int, isDirect: Boolean
    ): Callers = {
        val encodedCaller = Callers.toLong(caller.id, pc, isDirect)
        val newSet = encodedCallers + encodedCaller

        // requires the LongTrieSet to return `this` if the `encodedCaller` is already contained.
        if (newSet eq encodedCallers)
            this
        else
            new CallersOnlyWithConcreteCallers(newSet)
    }

    override def updatedWithUnknownContext(): Callers =
        CallersImplWithOtherCalls(
            encodedCallers,
            hasVMLevelCallers = false,
            hasCallersWithUnknownContext = true
        )

    override def updatedWithVMLevelCall(): Callers =
        CallersImplWithOtherCalls(
            encodedCallers,
            hasVMLevelCallers = true,
            hasCallersWithUnknownContext = false
        )
}

class CallersImplWithOtherCalls(
        val encodedCallers:                GrowableLongSet[_] /*MethodId + PC*/ ,
        private val specialCallSitesFlags: Byte // last bit vm lvl, second last bit unknown context
) extends CallersImplementation {
    assert(!encodedCallers.isEmpty)
    assert(specialCallSitesFlags >= 0 && specialCallSitesFlags <= 3)

    override def hasVMLevelCallers: Boolean = (specialCallSitesFlags & 1) != 0

    override def hasCallersWithUnknownContext: Boolean = (specialCallSitesFlags & 2) != 0

    override def updated(
        caller: DeclaredMethod, pc: Int, isDirect: Boolean
    ): Callers = {
        val encodedCaller = Callers.toLong(caller.id, pc, isDirect)
        val newSet = encodedCallers + encodedCaller

        // requires the LongTrieSet to return `this` if the `encodedCaller` is already contained.
        if (newSet eq encodedCallers)
            this
        else
            new CallersImplWithOtherCalls(newSet, specialCallSitesFlags)
    }

    override def updatedWithVMLevelCall(): Callers =
        if (hasVMLevelCallers)
            this
        else
            new CallersImplWithOtherCalls(encodedCallers, (specialCallSitesFlags | 1).toByte)

    override def updatedWithUnknownContext(): Callers =
        if (hasCallersWithUnknownContext)
            this
        else
            new CallersImplWithOtherCalls(encodedCallers, (specialCallSitesFlags | 2).toByte)
}

object CallersImplWithOtherCalls {
    def apply(
        encodedCallers:               GrowableLongSet[_] /* MethodId + PC */ ,
        hasVMLevelCallers:            Boolean,
        hasCallersWithUnknownContext: Boolean
    ): CallersImplWithOtherCalls = {
        assert(hasVMLevelCallers | hasCallersWithUnknownContext)
        assert(!encodedCallers.isEmpty)

        val vmLvlCallers = if (hasVMLevelCallers) 1 else 0
        val unknownContext = if (hasCallersWithUnknownContext) 2 else 0

        new CallersImplWithOtherCalls(
            encodedCallers, (vmLvlCallers | unknownContext).toByte
        )
    }
}

object Callers extends CallersPropertyMetaInformation {

    final val key: PropertyKey[Callers] = {
        val name = "opalj.CallersProperty"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒ NoCallers
                case _ ⇒
                    throw new IllegalStateException(s"analysis required for property: $name")
            }
        )
    }

    def toLong(methodId: Int, pc: Int, isDirect: Boolean): Long = {
        assert(pc >= 0 && pc <= 0xFFFF)
        assert(methodId >= 0 && methodId <= 0x3FFFFF)
        (methodId.toLong | (pc.toLong << 22)) | (if (isDirect) Long.MinValue else 0)
    }

    def toMethodPcAndIsDirect(pcMethodAndIsDirect: Long): (Int, Int, Boolean) = {
        (
            pcMethodAndIsDirect.toInt & 0x3FFFFF,
            (pcMethodAndIsDirect >> 22).toInt & 0xFFFF,
            pcMethodAndIsDirect < 0
        )
    }
}
