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

import org.opalj.fpcf.PropertyKey.SomeEPKs
import org.opalj.fpcf.properties.DomainSpecific.DomainSpecificReason
import org.opalj.fpcf.properties.ImpureBase.ImpureDueToUnknownProperty
import org.opalj.fpcf.properties.ImpureBase.ImpureReason
import org.opalj.fpcf.properties.Purity.HAS_ALLOCATIONS
import org.opalj.fpcf.properties.Purity.IS_NON_DETERMINISTIC
import org.opalj.fpcf.properties.Purity.MODIFIES_RECEIVER
import org.opalj.fpcf.properties.Purity.USES_DOMAIN_SPECIFIC_ACTIONS
import org.opalj.fpcf.properties.Purity.IS_CONDITIONAL

import scala.annotation.switch

sealed trait PurityPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Purity

}

/**
 * Describes the purity of a method. A method is pure if its result only depends on its inputs
 * and/or immutable global state and the execution of the method does not have any side effects;
 * the method's inputs include the current object that is the receiver of the call.
 *
 * '''This analysis follows the definition found on wikipedia:'''
 *
 * [...] a function may be considered a pure function if both of the following statements about
 * the function hold:
 *  -   The function always evaluates to the same result value given the same argument value(s).
 *      The function result value cannot depend on any hidden information or state that may change
 *      while program execution proceeds or between different executions of the program, nor can it
 *      depend on any external input from I/O devices.
 *
 *      '''Hence, using true constants (e.g., Math.e) is not a problem as well as creating
 *      intermediate (mutable) data structures.
 *      More precisely, methods are pure if the values they refer to always (even across program
 *      runs) have an identical shape and the precise location in the heap is not relevant (e.g.
 *      java.lang.Object.hashCode() and ...toString() are not pure).'''
 *
 *  -   Evaluation of the result does not cause any semantically observable side effect or output,
 *      such as mutation of mutable objects or output to I/O devices.
 *      The result value need not depend on all (or any) of the argument values. However, it must
 *      depend on nothing other than the argument values. The function may return multiple result
 *      values and these conditions must apply to all returned values for the function to be
 *      considered pure. If an argument is "call-by-reference", any parameter mutation will alter
 *      the value of the argument outside the function, which will render the function impure.
 *
 * Given the preceeding specification, the purity of a method is described by the subclasses of
 * this property:
 *
 * [[ImpureBase]] methods have no constraints on their behavior. They may have side effect and
 * depend on all accessible (global) state. Analyses can always return `Impure` as a safe default
 * value - even if they are not able to prove that a method is indeed impure; however, in the
 * latter case using [[MaybePure]] is recommended as this enable potentially succeeding Analyses
 * to refine the property. Besides `Impure` there are several other implementations of
 * [[ImpureBase]] that are to be treated identically to `Impure` but which give additional reasoning
 * why the analysis classified a method as impure.
 *
 * [[SideEffectFree]] methods may depend on all accessible (and mutable) state, but may not have
 * any side effects.
 * In single-threaded execution, this means that the object graph of the program may not
 * have changed between invocation of the method and its return, except for potentially additional
 * objects allocated by the method. For multi-threaded execution, the object graph may not change
 * due to the invocation of the method, again except allocation of new objects. Note that the object
 * graph may change during execution of the method due to other methods executing on concurrent
 * threads. The method must not have any effects (besides consumption of resources like memory and
 * processor time) on methods executing concurrently, in particular it may not acquire any locks on
 * objects that concurrent methods could also try to acquire.
 *
 * Analyses may return [[SideEffectFree]] as a safe default value if they are unable to guarantee
 * that a method is [[Pure]], even if it is. However, to return `SideEffectFree` the analysis has
 * to guarantee that the method does not have any side effects.
 *
 * [[Pure]] methods must be side effect free as above, but additionally, their result may only
 * depend on their parameters (including the receiver object) and global constants. In particular,
 * the result of a pure method must be structurally identical each time the method is invoked with
 * structurally identical parameters.
 * I.e., pure methods may depend on the aliasing relation between their
 * parameters or between their parameters and global constants.  E.g., the following method is
 * pure:
 * {{{
 * def cmp(s: String) : Boolean = {
 *      // Reference(!) comparison of s with the interned string "Demo":
 *      s eq "Demo";
 * }
 * }}}
 * In multi-threaded execution, pure methods can not depend on any mutable state of their
 * parameters if that state might be mutated by concurrently executing methods.
 *
 * Analyses may return [[Pure]] only if they are able to guarantee that a method fulfills these
 * requirements.
 *
 * [[SideEffectFreeWithoutAllocations]] and [[PureWithoutAllocations]] have the same requirements as
 * [[SideEffectFree]] and [[Pure]], but invoking methods with these properties may not cause any
 * allocation heap objects (including arrays).
 *
 * [[ExternallySideEffectFree]] and [[ExternallyPure]] methods are also similar to
 * [[SideEffectFree]] and [[Pure]] methods, respectively, but may modify their receiver object.
 * These properties may be used to detect changes that are confined because the receiver object is
 * under the control of the caller.
 *
 * [[DomainSpecificSideEffectFree]] and [[DomainSpecificPure]] methods may perform actions that are
 * generally considered impure (or non-deterministic in the case of `DomainSpecificPure`), but that
 * some clients may want to treat as pure. Such actions include, e.g. logging. A Rater is used to
 * identify such actions and the properties contain a set of reasons assigned by the Rater.
 *
 * [[DomainSpecificExternallySideEffectFree]] and [[DomainSpecificExternallyPure]] methods are
 * similar, but may again modify their receiver.
 *
 * [[ConditionallySideEffectFree]] and [[ConditionallyPure]] can be used by analyses to specify
 * intermediate results. `ConditionallySideEffectFree` methods are methods that are
 * [[SideEffectFree]] depending on other analysis results not yet available.
 * `ConditionallySideEffectFree` methods may not become [[Pure]] anymore. ConditionallyPure methods
 * are methods that may still become [[Pure]] depending on the properties of the depending entities.
 * Hence, `ConditionallyPure` methods might also become [[ImpureBase]] or [[SideEffectFree]], or
 * [[ConditionallySideEffectFree]].
 *
 * [[ConditionallySideEffectFreeWithoutAllocations]] and [[ConditionallyPureWithoutAllocations]] are
 * again similar, but such methods may still become [[SideEffectFreeWithoutAllocations]] or
 * [[PureWithoutAllocations]], respectively.
 *
 * [[ConditionallyExternallySideEffectFree]] and [[ConditionallyExternallyPure]] methods on the
 * other hand can only become [[ExternallySideEffectFree]] or [[ConditionallyPure]], respectively.
 * They might also become anything below these, such as [[ImpureBase]].
 *
 * [[ConditionallyDomainSpecificSideEffectFree]] and [[ConditionallyDomainSpecificPure]] methods are
 * methods that may only become [[DomainSpecificSideEffectFree]] or [[DomainSpecificPure]],
 * respectively. They might also become anything below these, such as [[ImpureBase]].
 *
 * [[ConditionallyDomainSpecificExternallySideEffectFree]] and
 * [[ConditionallyDomainSpecificExternallyPure]] methods are methods that may only become
 * [[DomainSpecificExternallySideEffectFree]] or [[DomainSpecificExternallyPure]], respectively.
 * They might also become anything below these, such as [[ImpureBase]].
 *
 * [[MaybePure]] is used as a default fallback value if no purity information could be computed for
 * a method. Conceptually, clients must treat this in the same way as [[ImpureBase]] except that
 * a future refinement may be possible.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
sealed abstract class Purity extends Property with PurityPropertyMetaInformation {

    /**
     * The globally unique key of the [[Purity]] property.
     */
    final def key: PropertyKey[Purity] = Purity.key

    val flags: Int

    val hasAllocations: Boolean = (flags & HAS_ALLOCATIONS) != 0
    val isDeterministic: Boolean = (flags & IS_NON_DETERMINISTIC) == 0
    val modifiesReceiver: Boolean = (flags & MODIFIES_RECEIVER) != 0
    val usesDomainSpecificActions: Boolean = (flags & USES_DOMAIN_SPECIFIC_ACTIONS) != 0
    val isConditional: Boolean = (flags & IS_CONDITIONAL) != 0

    val reasons: Set[DomainSpecificReason] = Set.empty

    def meet(other: Purity): Purity = other match {
        case MaybePure | ImpureBase(_) ⇒ other
        case _                         ⇒ Purity(flags | other.flags, reasons | other.reasons)
    }

    def withoutExternal: Purity = {
        if (modifiesReceiver) Purity(flags & ~MODIFIES_RECEIVER, reasons)
        else this
    }

    def unconditional: Purity = {
        if (isConditional) Purity(flags & ~IS_CONDITIONAL, reasons)
        else this
    }
}

object Purity extends PurityPropertyMetaInformation {

    def baseCycleResolutionStrategy(
        propertyStore: PropertyStore,
        epks:          SomeEPKs
    ): Iterable[Result] = {
        // When we have a cycle, we can leverage the "purity" if all properties are either
        // conditionally pure or conditionally side-effect free

        val purity = epks.foldLeft(PureWithoutAllocations: Purity) { (purity, epk) ⇒
            epk match {
                case EPK(e, `key`) ⇒
                    val p = propertyStore(e, key).p
                    assert(p.isConditional) // a cycle must not contain a final property
                    purity meet p.unconditional

                case _ ⇒
                    // We have a complex cycle which involves other properties...
                    // let's give up.
                    ImpureDueToUnknownProperty
            }
        }
        // NOTE
        // We DO NOT increase the pureness of all methods as this will happen automatically
        // as a sideeffect of setting the pureness of one method!
        Iterable(Result(epks.head.e, purity))
    }

    /**
     * The key associated with every purity property. The name is "Purity"; the fallback is
     * "MaybePure".
     */
    final val key = PropertyKey.create[Purity]("Purity", MaybePure, baseCycleResolutionStrategy _)

    final val EMPTY_FLAGS = 0x0;
    final val HAS_ALLOCATIONS = 0x1;
    final val IS_NON_DETERMINISTIC = 0x2;
    final val MODIFIES_RECEIVER = 0x4;
    final val USES_DOMAIN_SPECIFIC_ACTIONS = 0x8;
    final val IS_CONDITIONAL = 0x10;

    def apply(flags: Int, reasons: Set[DomainSpecificReason]): Purity = {
        (flags: @switch) match {
            case PureWithoutAllocations.flags              ⇒ PureWithoutAllocations
            case SideEffectFreeWithoutAllocations.flags    ⇒ SideEffectFreeWithoutAllocations
            case Pure.flags                                ⇒ Pure
            case SideEffectFree.flags                      ⇒ SideEffectFree
            case ConditionallyPureWithoutAllocations.flags ⇒ ConditionallyPureWithoutAllocations
            case ConditionallySideEffectFreeWithoutAllocations.flags ⇒
                ConditionallySideEffectFreeWithoutAllocations
            case ConditionallyPure.flags           ⇒ ConditionallyPure
            case ConditionallySideEffectFree.flags ⇒ ConditionallySideEffectFree
            case _ if ((flags & USES_DOMAIN_SPECIFIC_ACTIONS) == 0) ⇒
                ((flags | HAS_ALLOCATIONS): @switch) match {
                    case ExternallyPure.flags              ⇒ ExternallyPure
                    case ExternallySideEffectFree.flags    ⇒ ExternallySideEffectFree
                    case ConditionallyExternallyPure.flags ⇒ ConditionallyExternallyPure
                    case ConditionallyExternallySideEffectFree.flags ⇒
                        ConditionallyExternallySideEffectFree
                }
            case _ ⇒ ((flags & ~USES_DOMAIN_SPECIFIC_ACTIONS | HAS_ALLOCATIONS): @switch) match {
                case Pure.flags           ⇒ DomainSpecificPure(reasons)
                case SideEffectFree.flags ⇒ DomainSpecificSideEffectFree(reasons)
                case ExternallyPure.flags ⇒ DomainSpecificExternallyPure(reasons)
                case ExternallySideEffectFree.flags ⇒
                    DomainSpecificExternallySideEffectFree(reasons)
                case ConditionallyPure.flags ⇒ ConditionallyDomainSpecificPure(reasons)
                case ConditionallySideEffectFree.flags ⇒
                    ConditionallyDomainSpecificSideEffectFree(reasons)
                case ConditionallyExternallyPure.flags ⇒
                    ConditionallyDomainSpecificExternallyPure(reasons)
                case ConditionallyExternallySideEffectFree.flags ⇒
                    ConditionallyDomainSpecificExternallySideEffectFree(reasons)
            }
        }
    }
}

/**
 * The fallback/default purity.
 *
 * It should be used in case of a dependency on an element for which no result could be computed.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object MaybePure extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | IS_NON_DETERMINISTIC | MODIFIES_RECEIVER | USES_DOMAIN_SPECIFIC_ACTIONS

    override def meet(other: Purity) = MaybePure
}

/**
 * The respective method is pure and invoking it does not cause any heap objects to be allocated.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object PureWithoutAllocations extends Purity {
    final val isRefineable = false
    final val flags = Purity.EMPTY_FLAGS

    override def meet(other: Purity) = other
}

/**
 * The respective method is pure.
 *
 *  @see [[Purity]] for further details regarding the purity levels.
 */
case object Pure extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS
}

/**
 * The respective method is side-effect free, i.e. it does not have side-effects but its results may
 * still be non-deterministic. Additionally invoking the method does not cause any heap objects to
 * be allocated.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object SideEffectFreeWithoutAllocations extends Purity {
    final val isRefineable = true
    final val flags = IS_NON_DETERMINISTIC
}

/**
 * The respective method is side-effect free, i.e. it does not have side-effects but its results may
 * still be non-deterministic.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object SideEffectFree extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | IS_NON_DETERMINISTIC
}

/**
 * The respective method may modify its receiver, but is pure otherwise.
 *
 * A method calling a `ExternallyPure` method can be `Pure` if the receiver of the call is confined
 * inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object ExternallyPure extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | MODIFIES_RECEIVER
}

/**
 * The respective method may modify its receiver, but otherwise it is side-effect free, i.e. it does
 * not have side effects but its results may still be non-deterministic.
 *
 * A method calling a `ExternallySideEffectFree` method can be `SideEffectFree` if the receiver of
 * the call is confined inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object ExternallySideEffectFree extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | IS_NON_DETERMINISTIC | MODIFIES_RECEIVER
}

/**
 * Reasons that explain why a method is domain specific.
 * Analyses (or rather the Rater objects they use) may use the reasons below or use their own
 * for more reasons.
 */
object DomainSpecific {
    type DomainSpecificReason = String

    /**
     * Domain specific because the method may raise exceptions.
     */
    final val RaisesExceptions: DomainSpecificReason = "raises exceptions"

    /**
     * Domain specific because the method uses `System.out` or `System.err`.
     */
    final val UsesSystemOutOrErr: DomainSpecificReason = "uses System.out or System.err"

    /**
     * Domain specific because the method uses some form of logging.
     */
    final val UsesLogging: DomainSpecificReason = "uses logging"
}

/**
 * The respective method may perform actions that are generally considered impure or
 * non-deterministic that some clients may wish to treat as pure. Otherwise it is pure.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 *
 * @param reasons The actions performed by the method that cause it to be domain specific.
 */
case class DomainSpecificPure(override val reasons: Set[DomainSpecificReason]) extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | USES_DOMAIN_SPECIFIC_ACTIONS
}

/**
 * The respective method may perform actions that are generally considered impure that some clients
 * may wish to treat as pure. Otherwise it is side-effect free.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 *
 * @param reasons The actions performed by the method that cause it to be domain specific.
 */
case class DomainSpecificSideEffectFree(override val reasons: Set[DomainSpecificReason])
    extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | IS_NON_DETERMINISTIC | USES_DOMAIN_SPECIFIC_ACTIONS
}

/**
 * The respective method may perform actions that are generally considered impure or
 * non-deterministic that some clients may wish to treat as pure and it may modify its receiver.
 * Otherwise it is pure.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 *
 * @param reasons The actions performed by the method that cause it to be domain specific.
 */
case class DomainSpecificExternallyPure(override val reasons: Set[DomainSpecificReason])
    extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | MODIFIES_RECEIVER | USES_DOMAIN_SPECIFIC_ACTIONS
}

/**
 * The respective method may perform actions that are generally considered impure that some clients
 * may wish to treat as pure and it may modify its receiver. Otherwise it is side-effect free.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 *
 * @param reasons The actions performed by the method that cause it to be domain specific.
 */
case class DomainSpecificExternallySideEffectFree(override val reasons: Set[DomainSpecificReason])
    extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | IS_NON_DETERMINISTIC | MODIFIES_RECEIVER | USES_DOMAIN_SPECIFIC_ACTIONS
}

/**
 * Used, if the pureness of a method is dependent on other analysis results not yet available, but
 * the method does not itself allocate any heap objects.
 *
 * A method calling a `ConditionallyPureWithoutAllocations` method can at most be
 * `ConditionallyPureWithoutAllocations` itself, unless `ConditionallyPureWithoutAllocations` is
 * refined to [[org.opalj.fpcf.properties.PureWithoutAllocations]].
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object ConditionallyPureWithoutAllocations extends Purity {
    final val isRefineable = true
    final val flags = IS_CONDITIONAL
}

/**
 * Used, if the pureness of a method is dependent on other analysis results not yet available.
 *
 * A method calling a `ConditionallyPure` method can at most be `ConditionallyPure` itself, unless
 * `ConditionallyPure` is refined to [[Pure]].
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object ConditionallyPure extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | IS_CONDITIONAL
}

/**
 * Used if the side-effect freeness of a method is dependent on other analysis results not yet
 * available, but the method does not itself allocate any heap objects.
 *
 * A method calling a `ConditionallySideEffectFreeWithoutAllocations` method can at most be
 * `ConditionallySideEffectFreeWithoutAllocations` itself, unless
 * `ConditionallySideEffectFreeWithoutAllocations` is refined to
 * [[SideEffectFreeWithoutAllocations]].
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object ConditionallySideEffectFreeWithoutAllocations extends Purity {
    final val isRefineable = true
    final val flags = IS_NON_DETERMINISTIC | IS_CONDITIONAL
}

/**
 * Used if the side-effect freeness of a method is dependent on other analysis results not yet
 * available.
 *
 * A method calling a `ConditionallySideEffectFree` method can at most be
 * `ConditionallySideEffectFree` itself, unless `ConditionallySideEffectFree` is refined to
 * [[SideEffectFree]].
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object ConditionallySideEffectFree extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | IS_NON_DETERMINISTIC | IS_CONDITIONAL
}

/**
 * Used if the method may modify its receiver and its purity is dependent on other analysis results
 * not yet available.
 *
 * A method calling a `ConditionallyExternallyPure` method can be `ConditionallyPure` if the
 * receiver of the call is confined inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object ConditionallyExternallyPure extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | MODIFIES_RECEIVER | IS_CONDITIONAL
}

/**
 * Used if the method may modify its receiver and its side-effect freeness is dependent on
 * other analysis results not yet available.
 *
 * A method calling a `ConditionallyExternallySideEffectFree` method can be
 * `ConditionallySideEffectFree` if the receiver of the call is confined inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object ConditionallyExternallySideEffectFree extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | IS_NON_DETERMINISTIC | MODIFIES_RECEIVER | IS_CONDITIONAL
}

/**
 * Used if the method may perform actions that are generally considered impure or
 * non-deterministic that some clients may wish to treat as pure and its purity is dependent on
 * other analysis results not yet available.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 *
 * @param reasons The actions performed by the method that cause it to be domain specific.
 */
case class ConditionallyDomainSpecificPure(override val reasons: Set[DomainSpecificReason])
    extends Purity {
    final val isRefineable = true
    final val flags = HAS_ALLOCATIONS | USES_DOMAIN_SPECIFIC_ACTIONS | IS_CONDITIONAL
}

/**
 * Used if the method may perform actions that are generally considered impure that some clients may
 * wish to treat as pure and its side-effect freeness is dependent on other analysis results not yet
 * available.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 *
 * @param reasons The actions performed by the method that cause it to be domain specific.
 */
case class ConditionallyDomainSpecificSideEffectFree(
        override val reasons: Set[DomainSpecificReason]
) extends Purity {
    final val isRefineable = true
    final val flags =
        HAS_ALLOCATIONS | IS_NON_DETERMINISTIC | USES_DOMAIN_SPECIFIC_ACTIONS | IS_CONDITIONAL
}

/**
 * Used if the method may perform actions that are generally considered impure or
 * non-deterministic that some clients may wish to treat as pure, it may modify its receiver and its
 * purity is dependent on other anlysis results not yet available.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 *
 * @param reasons The actions performed by the method that cause it to be domain specific.
 */
case class ConditionallyDomainSpecificExternallyPure(
        override val reasons: Set[DomainSpecificReason]
) extends Purity {
    final val isRefineable = true
    final val flags =
        HAS_ALLOCATIONS | MODIFIES_RECEIVER | USES_DOMAIN_SPECIFIC_ACTIONS | IS_CONDITIONAL
}

/**
 * Used if the method may perform actions that are generally considered impure that some clients may
 * wish to treat as pure, it may modify its receiver and its side-effect freeness is dependent on
 * other anlysis results not yet available.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 *
 * @param reasons The actions performed by the method that cause it to be domain specific.
 */
case class ConditionallyDomainSpecificExternallySideEffectFree(
        override val reasons: Set[DomainSpecificReason]
) extends Purity {
    final val isRefineable = true
    final val flags =
        HAS_ALLOCATIONS | IS_NON_DETERMINISTIC | MODIFIES_RECEIVER | USES_DOMAIN_SPECIFIC_ACTIONS | IS_CONDITIONAL
}

case class ImpureBase(reason: ImpureReason) extends Purity {
    final val isRefineable = true
    final val flags =
        HAS_ALLOCATIONS | IS_NON_DETERMINISTIC | MODIFIES_RECEIVER | USES_DOMAIN_SPECIFIC_ACTIONS

    override def meet(other: Purity) = other match {
        case MaybePure ⇒ MaybePure
        case _         ⇒ this
    }

    override def toString = s"Impure($reason)"
}

/**
 * Companion object defining common values for impurity.
 *
 * Analyses may use any of the values below or use their to give other reasons for
 * a method being impure. The reason given may be just the first of several reasons for impurity
 * and it is not required to be the same reason for different runs of the analysis.
 */
object ImpureBase {
    type ImpureReason = String

    /**
     * The respective method is either impure or we encountered an unresolvable cycle and cycle
     * resolution was enforced [[PropertyStore#waitOnPropertyComputationCompletion]].
     *
     * General impurity without further specified reason.
     * Analyses may return this object or any other implementation of ImpureBase to give a more
     * specific reason for the method being impure.
     */
    final val Impure = ImpureBase("")

    /**
     * The method is impure because it uses synchronization.
     */
    final val ImpureDueToSynchronization = ImpureBase("uses synchronization")

    /**
     * The method is impure because it may modify heap objects.
     */
    final val ImpureDueToHeapModification = ImpureBase("modifies heap objects")

    /**
     * The method is impure because it calls a method or uses a type that may be overriden/extended.
     */
    final val ImpureDueToFutureExtension =
        ImpureBase("uses method/type that may be overriden/extended")

    /**
     * The method is impure because it calls a method that may be impure.
     */
    final val ImpureDueToImpureCall = ImpureBase("calls impure method")

    /**
     * The method is impure because it uses an entity not found in the current project's scope.
     */
    final val ImpureDueToUnknownEntity = ImpureBase("depends on unknown entity")

    /**
     * The method is impure because an entity it uses has a property value unknown to the analysis.
     */
    final val ImpureDueToUnknownProperty = ImpureBase("depends on entity with unknown property")
}
