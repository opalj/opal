/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package cg

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.LongLinkedSet
import org.opalj.collection.immutable.LongLinkedTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.tac.fpcf.analyses.cg.TypeProvider

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

    def callers(method: DeclaredMethod)(
        implicit
        typeProvider: TypeProvider
    ): IterableOnce[(DeclaredMethod, Int /*PC*/ , Boolean /*isDirect*/ )] = {
        callContexts(method).iterator.foldLeft(List[(DeclaredMethod, Int, Boolean)]()) {
            (results, contextData) =>
                val (_, callerContext, pc, isDirect) = contextData
                callerContext match {
                    case NoContext => results
                    case _         => (callerContext.method, pc, isDirect) :: results
                }
        }
    }

    def callersForContextId(calleeContextId: Int): LongLinkedSet

    def callContexts(method: DeclaredMethod)(
        implicit
        typeProvider: TypeProvider
    ): IterableOnce[(Context /*Callee*/ , Context /*Caller*/ , Int /*PC*/ , Boolean /*isDirect*/ )]

    def calleeContexts(
        method: DeclaredMethod
    )(implicit typeProvider: TypeProvider): IterableOnce[Context]

    def forNewCalleeContexts(old: Callers, method: DeclaredMethod)(
        handleContext: Context /*Callee*/ => Unit
    )(
        implicit
        typeProvider: TypeProvider
    ): Unit

    def forNewCallerContexts(old: Callers, method: DeclaredMethod)(
        handleContext: (Context /*Callee*/ , Context /*Caller*/ , Int /*PC*/ , Boolean /*isDirect*/ ) => Unit
    )(
        implicit
        typeProvider: TypeProvider
    ): Unit

    /**
     * Returns a new callers object, containing all callers of `this` object and a call from
     * `caller` at program counter `pc`.
     *
     * In case, the specified call is already contained, `this` is returned, i.e. the reference does
     * not change when the of callers set remains unchanged.
     */
    def updated(calleeContext: Context, callerContext: Context, pc: Int, isDirect: Boolean): Callers

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

    override def callersForContextId(calleeContextId: Int): LongLinkedSet = {
        LongLinkedTrieSet.empty
    }

    override def callContexts(method: DeclaredMethod)(
        implicit
        typeProvider: TypeProvider
    ): IterableOnce[(Context /*Callee*/ , Context /*Caller*/ , Int /*PC*/ , Boolean /*isDirect*/ )] = {
        if (hasCallersWithUnknownContext || hasVMLevelCallers)
            Iterator((typeProvider.newContext(method), NoContext, -1, true))
        else
            Nil
    }

    override def calleeContexts(
        method: DeclaredMethod
    )(implicit typeProvider: TypeProvider): IterableOnce[Context] = {
        if (hasCallersWithUnknownContext || hasVMLevelCallers)
            Iterator(typeProvider.newContext(method))
        else
            Iterator.empty
    }

    override def forNewCalleeContexts(old: Callers, method: DeclaredMethod)(
        handleContext: Context /*Callee*/ => Unit
    )(
        implicit
        typeProvider: TypeProvider
    ): Unit = {
        if ((hasCallersWithUnknownContext || hasVMLevelCallers) &&
            ((old eq null) || !old.hasCallersWithUnknownContext && !old.hasVMLevelCallers))
            handleContext(typeProvider.newContext(method))
    }

    override def forNewCallerContexts(old: Callers, method: DeclaredMethod)(
        handleContext: (Context /*Callee*/ , Context /*Caller*/ , Int /*PC*/ , Boolean /*isDirect*/ ) => Unit
    )(
        implicit
        typeProvider: TypeProvider
    ): Unit = {
        if ((hasCallersWithUnknownContext || hasVMLevelCallers) &&
            ((old eq null) || !old.hasCallersWithUnknownContext && !old.hasVMLevelCallers))
            handleContext(typeProvider.newContext(method), NoContext, -1, true)
    }

    final override def updated(
        calleeContext: Context, callerContext: Context, pc: Int, isDirect: Boolean
    ): Callers = {
        val map = IntMap(calleeContext.id -> LongLinkedTrieSet(Callers.toLong(callerContext.id, pc, isDirect)))

        if (!hasCallersWithUnknownContext && !hasVMLevelCallers) {
            new CallersOnlyWithConcreteCallers(map, 1)
        } else {
            CallersImplWithOtherCalls(map, hasVMLevelCallers, hasCallersWithUnknownContext)
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
    val encodedCallers: IntMap[LongLinkedSet] /* Callee ContextID => Caller ContextId + PC + isDirect */
    override val size: Int

    final override def isEmpty: Boolean = size == 0

    final override def nonEmpty: Boolean = size != 0

    override def callersForContextId(calleeContextId: Int): LongLinkedSet = {
        encodedCallers.getOrElse(calleeContextId, LongLinkedTrieSet.empty)
    }

    final override def callContexts(method: DeclaredMethod)(
        implicit
        typeProvider: TypeProvider
    ): IterableOnce[(Context /*Callee*/ , Context /*Caller*/ , Int /*PC*/ , Boolean /*isDirect*/ )] = {
        val contexts = encodedCallers.iterator.flatMap {
            case (calleeContextId, callers) =>
                val calleeContext = typeProvider.contextFromId(calleeContextId)
                callers.iterator.map { callerData =>
                    val (callerContextId, pc, isDirect) = Callers.toContextPcAndIsDirect(callerData)
                    (calleeContext, typeProvider.contextFromId(callerContextId), pc, isDirect)
                }
        }
        if (hasCallersWithUnknownContext || hasVMLevelCallers)
            contexts ++ Iterator((typeProvider.newContext(method), NoContext, -1, true))
        else
            contexts
    }

    final override def calleeContexts(method: DeclaredMethod)(
        implicit
        typeProvider: TypeProvider
    ): IterableOnce[Context] = {
        val contexts = encodedCallers.keysIterator.map(typeProvider.contextFromId)
        if (hasCallersWithUnknownContext || hasVMLevelCallers) {
            val unknownContext = typeProvider.newContext(method)
            if (!encodedCallers.contains(unknownContext.id))
                contexts ++ Iterator(unknownContext)
            else
                contexts
        } else
            contexts
    }

    final override def forNewCalleeContexts(old: Callers, method: DeclaredMethod)(
        handleContext: Context /*Callee*/ => Unit
    )(
        implicit
        typeProvider: TypeProvider
    ): Unit = {
        val unknownContext = typeProvider.newContext(method)
        val unknownContextId = unknownContext.id

        encodedCallers.foreach {
            case (calleeContextId, _) =>
                if (old eq null)
                    handleContext(typeProvider.contextFromId(calleeContextId))
                else if (old.callersForContextId(calleeContextId).isEmpty) {
                    if (calleeContextId != unknownContextId ||
                        !old.hasCallersWithUnknownContext && !old.hasVMLevelCallers)
                        handleContext(typeProvider.contextFromId(calleeContextId))
                }
        }

        if ((hasCallersWithUnknownContext || hasVMLevelCallers) &&
            ((old eq null) || !old.hasCallersWithUnknownContext && !old.hasVMLevelCallers))
            if (!encodedCallers.contains(unknownContextId))
                handleContext(unknownContext)
    }

    final override def forNewCallerContexts(old: Callers, method: DeclaredMethod)(
        handleContext: (Context /*Callee*/ , Context /*Caller*/ , Int /*PC*/ , Boolean /*isDirect*/ ) => Unit
    )(
        implicit
        typeProvider: TypeProvider
    ): Unit = {
        encodedCallers.foreach {
            case (calleeContextId, callers) =>
                val calleeContext = typeProvider.contextFromId(calleeContextId)
                val seen = if (old eq null) 0 else old.callersForContextId(calleeContextId).size
                callers.forFirstN(callers.size - seen) { encodedPair: Long =>
                    val (callerContextId, pc, isDirect) = Callers.toContextPcAndIsDirect(encodedPair)
                    val callerContext = typeProvider.contextFromId(callerContextId)
                    handleContext(calleeContext, callerContext, pc, isDirect)
                }
        }

        if ((hasCallersWithUnknownContext || hasVMLevelCallers) &&
            ((old eq null) || !old.hasCallersWithUnknownContext && !old.hasVMLevelCallers))
            handleContext(typeProvider.newContext(method), NoContext, -1, true)
    }
}

class CallersOnlyWithConcreteCallers(
        val encodedCallers: IntMap[LongLinkedSet] /* Callee Context => Caller Context + PC + isDirect */ ,
        val size:           Int
) extends CallersImplementation with CallersWithoutVMLevelCall with CallersWithoutUnknownContext {

    override def updated(
        calleeContext: Context, callerContext: Context, pc: Int, isDirect: Boolean
    ): Callers = {
        val encodedCaller = Callers.toLong(callerContext.id, pc, isDirect)

        val calleeContextId = calleeContext.id
        if (encodedCallers.contains(calleeContextId)) {
            val oldSet = encodedCallers(calleeContextId)
            val newSet = oldSet + encodedCaller

            // requires the LongTrieSet to return `this` if the `encodedCaller` is already contained.
            if (newSet eq oldSet)
                this
            else
                new CallersOnlyWithConcreteCallers(
                    encodedCallers + (calleeContextId -> newSet),
                    size + 1
                )
        } else {
            new CallersOnlyWithConcreteCallers(
                encodedCallers + (calleeContextId -> LongLinkedTrieSet(encodedCaller)), size + 1
            )
        }
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
        val encodedCallers:                IntMap[LongLinkedSet] /* Callee Context => Caller Context + PC + isDirect */ ,
        val size:                          Int,
        private val specialCallSitesFlags: Byte // last bit vm lvl, second last bit unknown context
) extends CallersImplementation {
    assert(encodedCallers.nonEmpty)
    assert(specialCallSitesFlags >= 0 && specialCallSitesFlags <= 3)

    override def hasVMLevelCallers: Boolean = (specialCallSitesFlags & 1) != 0

    override def hasCallersWithUnknownContext: Boolean = (specialCallSitesFlags & 2) != 0

    override def updated(
        calleeContext: Context, callerContext: Context, pc: Int, isDirect: Boolean
    ): Callers = {
        val encodedCaller = Callers.toLong(callerContext.id, pc, isDirect)

        val calleeContextId = calleeContext.id
        if (encodedCallers.contains(calleeContextId)) {
            val oldSet = encodedCallers(calleeContextId)
            val newSet = oldSet + encodedCaller

            // requires the LongTrieSet to return `this` if the `encodedCaller` is already contained.
            if (newSet eq oldSet)
                this
            else
                new CallersImplWithOtherCalls(
                    encodedCallers + (calleeContextId -> newSet),
                    size + 1,
                    specialCallSitesFlags
                )
        } else {
            new CallersImplWithOtherCalls(
                encodedCallers + (calleeContextId -> LongLinkedTrieSet(encodedCaller)),
                size + 1,
                specialCallSitesFlags
            )
        }
    }

    override def updatedWithVMLevelCall(): Callers =
        if (hasVMLevelCallers)
            this
        else
            new CallersImplWithOtherCalls(encodedCallers, size, (specialCallSitesFlags | 1).toByte)

    override def updatedWithUnknownContext(): Callers =
        if (hasCallersWithUnknownContext)
            this
        else
            new CallersImplWithOtherCalls(encodedCallers, size, (specialCallSitesFlags | 2).toByte)
}

object CallersImplWithOtherCalls {
    def apply(
        encodedCallers:               IntMap[LongLinkedSet] /* Callee Context => Caller Context + PC + isDirect */ ,
        hasVMLevelCallers:            Boolean,
        hasCallersWithUnknownContext: Boolean
    ): CallersImplWithOtherCalls = {
        assert(hasVMLevelCallers | hasCallersWithUnknownContext)
        assert(encodedCallers.nonEmpty)

        val vmLvlCallers = if (hasVMLevelCallers) 1 else 0
        val unknownContext = if (hasCallersWithUnknownContext) 2 else 0

        new CallersImplWithOtherCalls(
            encodedCallers,
            encodedCallers.iterator.map(_._2.size).sum,
            (vmLvlCallers | unknownContext).toByte
        )
    }
}

object Callers extends CallersPropertyMetaInformation {

    final val key: PropertyKey[Callers] = {
        val name = "opalj.CallersProperty"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) => reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => NoCallers
                case _ =>
                    throw new IllegalStateException(s"analysis required for property: $name")
            }
        )
    }

    def toLong(contextId: Int, pc: Int, isDirect: Boolean): Long = {
        assert(pc >= 0 && pc <= 0xFFFF)
        (contextId.toLong << 16) | pc.toLong | (if (isDirect) Long.MinValue else 0)
    }

    def toContextPcAndIsDirect(pcContextAndIsDirect: Long): (Int, Int, Boolean) = {
        (
            (pcContextAndIsDirect >> 16).toInt,
            pcContextAndIsDirect.toInt & 0xFFFF,
            pcContextAndIsDirect < 0
        )
    }
}
