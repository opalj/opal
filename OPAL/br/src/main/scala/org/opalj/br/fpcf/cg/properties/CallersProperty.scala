/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cg
package properties

import org.opalj.collection.immutable.LongTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.DeclaredMethods

/**
 * For a given [[org.opalj.br.DeclaredMethod]], and for each call site (represented by the PC), the set of methods
 * that are possible call targets.
 *
 * @author Florian Kuebler
 */
sealed trait CallersPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = CallersProperty
}

sealed trait CallersProperty extends OrderedProperty with CallersPropertyMetaInformation {

    def hasCallersWithUnknownContext: Boolean

    def hasVMLevelCallers: Boolean

    def size: Int

    def callers(implicit declaredMethods: DeclaredMethods): TraversableOnce[(DeclaredMethod, Int /*PC*/ )]

    /**
     * Returns a new callers object, containing all callers of `this` object and a call from
     * `caller` at program counter `pc`.
     *
     * In case, the specified call is already contained, `this` is returned, i.e. the reference does
     * not change when the of callers set remains unchanged.
     */
    def updated(caller: DeclaredMethod, pc: Int): CallersProperty

    def updatedWithUnknownContext(): CallersProperty

    def updatedWithVMLevelCall(): CallersProperty

    override def toString: String = {
        s"Callers(size=${this.size})"
    }

    final def key: PropertyKey[CallersProperty] = CallersProperty.key

    override def checkIsEqualOrBetterThan(e: Entity, other: CallersProperty): Unit = {
        if (other.size < size)
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
    }
}

sealed trait CallersWithoutUnknownContext extends CallersProperty {
    final override def hasCallersWithUnknownContext: Boolean = false
}

sealed trait CallersWithUnknownContext extends CallersProperty {
    final override def hasCallersWithUnknownContext: Boolean = true
    final override def updatedWithUnknownContext(): CallersWithUnknownContext = this
}

sealed trait CallersWithVMLevelCall extends CallersProperty {
    final override def hasVMLevelCallers: Boolean = true
    final override def updatedWithVMLevelCall(): CallersWithVMLevelCall = this
}

sealed trait CallersWithoutVMLevelCall extends CallersProperty {
    final override def hasVMLevelCallers: Boolean = false
}

sealed trait EmptyConcreteCallers extends CallersProperty {
    final override def size: Int = 0

    final override def callers(
        implicit
        declaredMethods: DeclaredMethods
    ): TraversableOnce[(DeclaredMethod, Int)] = {
        Nil
    }

    final override def updated(
        caller: DeclaredMethod, pc: Int
    ): CallersProperty = {
        val set = LongTrieSet(CallersProperty.toLong(caller.id, pc))

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

sealed trait CallersImplementation extends CallersProperty {
    val encodedCallers: LongTrieSet /* MethodId + PC*/
    final override def size: Int = encodedCallers.size

    final override def callers(
        implicit
        declaredMethods: DeclaredMethods
    ): TraversableOnce[(DeclaredMethod, Int /*PC*/ )] = {
        for {
            encodedPair ← encodedCallers.iterator
        } yield {
            val (mId, pc) = CallersProperty.toMethodAndPc(encodedPair)
            declaredMethods(mId) → pc
        }
    }
}

class CallersOnlyWithConcreteCallers(
        val encodedCallers: LongTrieSet /*MethodId + PC*/
) extends CallersImplementation with CallersWithoutVMLevelCall with CallersWithoutUnknownContext {

    override def updated(
        caller: DeclaredMethod, pc: Int
    ): CallersProperty = {
        val encodedCaller = CallersProperty.toLong(caller.id, pc)
        val newSet = encodedCallers + encodedCaller

        // requires the LongTrieSet to return `this` if the `encodedCaller` is already contained.
        if (newSet eq encodedCallers)
            this
        else
            new CallersOnlyWithConcreteCallers(newSet)
    }

    override def updatedWithUnknownContext(): CallersProperty =
        CallersImplWithOtherCalls(
            encodedCallers,
            hasVMLevelCallers = false,
            hasCallersWithUnknownContext = true
        )

    override def updatedWithVMLevelCall(): CallersProperty =
        CallersImplWithOtherCalls(
            encodedCallers,
            hasVMLevelCallers = true,
            hasCallersWithUnknownContext = false
        )
}

class CallersImplWithOtherCalls(
        val encodedCallers:                LongTrieSet /*MethodId + PC*/ ,
        private val specialCallSitesFlags: Byte // last bit vm lvl, second last bit unknown context
) extends CallersImplementation {
    assert(!encodedCallers.isEmpty)
    assert(specialCallSitesFlags >= 0 && specialCallSitesFlags <= 3)

    override def hasVMLevelCallers: Boolean = (specialCallSitesFlags & 1) != 0

    override def hasCallersWithUnknownContext: Boolean = (specialCallSitesFlags & 2) != 0

    override def updated(
        caller: DeclaredMethod, pc: Int
    ): CallersProperty = {
        val encodedCaller = CallersProperty.toLong(caller.id, pc)
        val newSet = encodedCallers + encodedCaller

        // requires the LongTrieSet to return `this` if the `encodedCaller` is already contained.
        if (newSet eq encodedCallers)
            this
        else
            new CallersImplWithOtherCalls(newSet, specialCallSitesFlags)
    }

    override def updatedWithVMLevelCall(): CallersProperty =
        if (hasVMLevelCallers)
            this
        else
            new CallersImplWithOtherCalls(encodedCallers, (specialCallSitesFlags | 1).toByte)

    override def updatedWithUnknownContext(): CallersProperty =
        if (hasCallersWithUnknownContext)
            this
        else
            new CallersImplWithOtherCalls(encodedCallers, (specialCallSitesFlags | 2).toByte)
}

object CallersImplWithOtherCalls {
    def apply(
        encodedCallers:               LongTrieSet /* MethodId + PC */ ,
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

object CallersProperty extends CallersPropertyMetaInformation {

    final val key: PropertyKey[CallersProperty] = {
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

    def toLong(methodId: Int, pc: Int): Long = {
        assert(pc >= 0 && pc < 0xFFFF)
        assert(methodId >= 0 && methodId <= 0x3FFFFF)
        methodId.toLong | (pc.toLong << 22)
    }

    def toMethodAndPc(pcAndMethod: Long): (Int, Int) = {
        (pcAndMethod.toInt & 0x3FFFFF, (pcAndMethod >> 22).toInt)
    }
}
