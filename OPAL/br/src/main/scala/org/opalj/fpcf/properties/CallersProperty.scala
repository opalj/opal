/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.collection.immutable.LongTrieSet

/**
 * For a given [[DeclaredMethod]], and for each call site (represented by the PC), the set of methods
 * that are possible call targets.
 *
 * @author Florian Kuebler
 */
sealed trait CallersPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = CallersProperty
}

sealed trait CallersProperty extends Property with OrderedProperty with CallersPropertyMetaInformation {

    def hasCallersWithUnknownContext: Boolean

    def hasVMLevelCallers: Boolean

    def size: Int

    def callers(implicit declaredMethods: DeclaredMethods): TraversableOnce[(DeclaredMethod, Int /*PC*/ )]

    def updated(caller: DeclaredMethod, pc: Int)(implicit declaredMethods: DeclaredMethods): CallersProperty

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
    )(implicit declaredMethods: DeclaredMethods): CallersProperty = {
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
            (mId, pc) = CallersProperty.toMethodAndPc(encodedPair)
        } yield declaredMethods(mId) → pc
    }
}

class CallersOnlyWithConcreteCallers(
        val encodedCallers: LongTrieSet /*MethodId + PC*/
) extends CallersImplementation with CallersWithoutVMLevelCall with CallersWithoutUnknownContext {

    override def updated(
        caller: DeclaredMethod, pc: Int
    )(implicit declaredMethods: DeclaredMethods): CallersProperty = {
        val encodedCaller = CallersProperty.toLong(caller.id, pc)
        if (encodedCallers.contains(encodedCaller))
            this
        else
            new CallersOnlyWithConcreteCallers(encodedCallers + encodedCaller)
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
    )(implicit declaredMethods: DeclaredMethods): CallersProperty = {
        val encodedCaller = CallersProperty.toLong(caller.id, pc)
        if (encodedCallers.contains(encodedCaller: java.lang.Long))
            this
        else
            new CallersImplWithOtherCalls(encodedCallers + encodedCaller, specialCallSitesFlags)
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

object LowerBoundCallers extends CallersWithUnknownContext with CallersWithVMLevelCall {

    override lazy val size: Int = {
        Int.MaxValue
    }

    override def callers(
        implicit
        declaredMethods: DeclaredMethods
    ): TraversableOnce[(DeclaredMethod, Int /*PC*/ )] = {
        throw new UnsupportedOperationException()
    }

    override def updated(
        caller: DeclaredMethod, pc: Int
    )(implicit declaredMethods: DeclaredMethods): CallersProperty = this
}

object CallersProperty extends CallersPropertyMetaInformation {

    final val key: PropertyKey[CallersProperty] = {
        PropertyKey.create(
            "CallersProperty",
            (ps: PropertyStore, reason: FallbackReason, m: DeclaredMethod) ⇒ reason match {
                case PropertyIsNotComputedByAnyAnalysis ⇒
                    LowerBoundCallers
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                    NoCallers
            },
            (_: PropertyStore, eps: EPS[DeclaredMethod, CallersProperty]) ⇒ eps.ub,
            (_: PropertyStore, _: DeclaredMethod) ⇒ None
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
