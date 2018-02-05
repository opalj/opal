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

import scala.annotation.switch

import org.opalj.fpcf.PropertyKey.SomeEPKs
import org.opalj.fpcf.properties.Purity.HasAllocations
import org.opalj.fpcf.properties.Purity.IsNonDeterministic
import org.opalj.fpcf.properties.Purity.ModifiesReceiver
import org.opalj.fpcf.properties.Purity.PerformsDomainSpecificOperations
import org.opalj.fpcf.properties.Purity.IsConditional
import org.opalj.fpcf.properties.Purity.PureFlags
import org.opalj.fpcf.properties.Purity.ExternallyPureFlags
import org.opalj.fpcf.properties.Purity.SideEffectFreeWithoutAllocationsFlags
import org.opalj.fpcf.properties.Purity.SideEffectFreeFlags
import org.opalj.fpcf.properties.Purity.ExternallySideEffectFreeFlags
import org.opalj.fpcf.properties.Purity.ImpureFlags

sealed trait PurityPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Purity

}

/**
 * Describes the level of the purity of a method.
 *
 * In general, a method is pure if its result only depends on its inputs
 * and/or immutable global state and the execution of the method does not have any side effects;
 * an instance method's inputs include the current object that is the receiver of the call.
 *
 * '''The description of the purity levels is inspired by the definition found on wikipedia:'''
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
 * Given the preceding specification, the purity of a method is described by the subclasses of
 * this property. In the following, the prefix of the names of the purity levels are used to
 * identify the certainty of the computation of the purity; the letters have the following meaning:
 *  - LB = Lower Bound; the method is at least &lt;PURITY_LEVEL&gt;, but can still be even more pure.
 *  - C = Conditional; i.e., the current purity level depends on the purity level of other entities
 *    (These states are primarily used by the analysis to record the analysis progress.)
 *  - D = The method is &lt;PURITY_LEVEL&GT; if certain '''Domain-specific''' (non-pure) operations
 *    are ignored.
 *
 * [[Impure]] methods have no constraints on their behavior. They may have side effect and
 * depend on all accessible (global) state. Analyses can always return `(LB)Impure` as a safe
 * default value - even if they are not able to prove that a method is indeed impure; however,
 * in the latter case using [[LBImpure]] is recommended as this enables subsequent analyses
 * to refine the property. There are several implementations of [[LBImpure]] which give additional
 * reasoning why the analysis classified a method as impure.
 *
 * [[LBSideEffectFree]] methods may depend on all accessible (and mutable) state, but may not have
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
 * Analyses may return [[LBSideEffectFree]] as a safe default value if they are unable to guarantee
 * that a method is [[LBPure]], even if it is. However, to return `SideEffectFree` the analysis has
 * to guarantee that the method does not have any side effects.
 *
 * [[LBPure]] methods must be side effect free as described above and their result may only
 * depend on their parameters (including the receiver object) and global constants. In particular,
 * the result of a pure method must be structurally identical each time the method is invoked with
 * structurally identical parameters.
 * I.e., pure methods may depend on the aliasing relation between their
 * parameters or between their parameters and global constants. E.g., the following method is
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
 * Analyses may return [[LBPure]] only if they are able to guarantee that a method fulfills these
 * requirements.
 *
 * [[LBSideEffectFreeWithoutAllocations]] and [[PureWithoutAllocations]] have the same requirements
 * as [[LBSideEffectFree]] and [[LBPure]], but invoking methods with these properties may not
 * cause any allocation of heap objects (including arrays).
 *
 * [[LBExternallySideEffectFree]] and [[LBExternallyPure]] methods are also similar to
 * [[LBSideEffectFree]] and [[LBPure]] methods, respectively, but may modify their receiver object.
 * These properties may be used to detect changes that are confined because the receiver object is
 * under the control of the caller.
 *
 * [[LBDSideEffectFree]] and [[LBDPure]] methods may perform actions that are
 * generally considered impure (or non-deterministic in the case of `DPure`), but that
 * some clients may want to treat as pure. Such actions include, e.g. logging. A `Rater` is used to
 * identify such actions and the properties contain a set of reasons assigned by the Rater.
 *
 * [[LBDExternallySideEffectFree]] and [[LBDExternallyPure]] methods are
 * similar, but may again modify their receiver.
 *
 * [[CLBSideEffectFree]] and [[CLBPure]] can be used by analyses to specify
 * intermediate results. `CLBSideEffectFree` methods are methods that are
 * [[LBSideEffectFree]] depending on other analysis results not yet available.
 * `CLBSideEffectFree` methods may not become [[LBPure]] anymore. CLBPure methods
 * are methods that may still become [[LBPure]] depending on the properties of the depending
 * entities.
 * Hence, `CLBPure` methods might also become [[Impure]] or [[LBSideEffectFree]], or
 * [[CLBSideEffectFree]].
 *
 * [[CLBSideEffectFreeWithoutAllocations]] and [[CPureWithoutAllocations]] are
 * again similar, but such methods may still become [[LBSideEffectFreeWithoutAllocations]] or
 * [[PureWithoutAllocations]], respectively.
 *
 * [[CLBExternallySideEffectFree]] and [[CLBExternallyPure]] methods on the
 * other hand can only become [[LBExternallySideEffectFree]] or [[CLBPure]], respectively.
 * They might also become even less pure, such as [[LBImpure]].
 *
 * [[CLBDSideEffectFree]] and [[CLBDPure]] methods are
 * methods that may only become [[LBDSideEffectFree]] or [[LBDPure]],
 * respectively. They might also become anything below these, such as [[LBImpure]].
 *
 * [[CLBDExternallySideEffectFree]] and
 * [[CLBDExternallyPure]] methods are methods that may only become
 * [[LBDExternallySideEffectFree]] or [[LBDExternallyPure]], respectively.
 * They might also become anything below these, such as [[LBImpure]].
 *
 * [[Impure]] is (also) used as the fallback value if no purity information could be
 * computed for a method (no analysis is scheduled). Conceptually, clients must treat this in the
 * same way as [[LBImpure]] except that a future refinement may be possible in case of [[LBImpure]].
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

    def hasAllocations: Boolean = (flags & HasAllocations) != 0
    def isDeterministic: Boolean = (flags & IsNonDeterministic) == 0
    def modifiesReceiver: Boolean = (flags & ModifiesReceiver) != 0
    def usesDomainSpecificActions: Boolean = (flags & PerformsDomainSpecificOperations) != 0
    def isConditional: Boolean = (flags & IsConditional) != 0

    /**
     * Combines this purity value with another one to represent the progress by a purity
     * analysis in one phase.
     * Conditional as well as unconditional values are combined to the purity level that expresses
     * a weaker purity, thereby incorporating the effect of counter-examples to a stronger purity.
     * Thus, the result of this operation is used to represent a (potentially conditional) upper
     * bound on the possible final result of the purity analysis that performs this operation.
     * If one of the combined purity values is conditional and the other is not, the result will be
     * the same as if the conditional purity value was combined with the conditional value that
     * corresponds to the unconditional value.
     */
    def combine(other: Purity): Purity = {
        other match {
            case that: ClassifiedImpure ⇒ that
            case _                      ⇒ Purity(this.flags | other.flags)
        }
    }

    def withoutExternal: Purity = if (modifiesReceiver) Purity(flags & ~ModifiesReceiver) else this

    def unconditional: Purity = if (isConditional) Purity(flags & ~IsConditional) else this

}

object Purity extends PurityPropertyMetaInformation {

    def baseCycleResolutionStrategy(
        propertyStore: PropertyStore,
        epks:          SomeEPKs
    ): Iterable[Result] = {
        // When we have a cycle we can leverage the "purity" - conceptually (unless we
        // we have a programming bug) all properties (also those belonging to other
        // lattice) model conditional properties under the assumption that we have
        // at least the current properties.
        val e = epks.head.e
        val p = propertyStore(e, key).p
        assert(p.isConditional) // a cycle must not contain a non-conditional property
        // NOTE
        // We DO NOT increase the purity of all methods as this will happen automatically as a
        // sideeffect of setting the purity of one method!
        Iterable(Result(e, propertyStore(e, key).p.unconditional))
    }

    /**
     * The key associated with every purity property. The name is "Purity"; the fallback is
     * "Impure".
     */
    final val key = PropertyKey.create[Purity]("Purity", Impure, baseCycleResolutionStrategy _)

    final val HasAllocations = 0x1;
    final val IsNonDeterministic = 0x2;
    final val ModifiesReceiver = 0x4;
    final val PerformsDomainSpecificOperations = 0x8;
    // "Temporary Flags"
    final val IsConditional = 0x10;

    final val PureFlags = HasAllocations
    final val ExternallyPureFlags = PureFlags | ModifiesReceiver
    final val SideEffectFreeWithoutAllocationsFlags = IsNonDeterministic
    final val SideEffectFreeFlags = SideEffectFreeWithoutAllocationsFlags | PureFlags
    final val ExternallySideEffectFreeFlags = SideEffectFreeFlags | ModifiesReceiver
    // There is no flag for impurity as analyses have to treat [[ClassifiedImpure]] specially anyway
    final val ImpureFlags = ExternallySideEffectFreeFlags | PerformsDomainSpecificOperations

    /**
     * Returns the purity level matching the given flags for internal use by the combine operation
     * and unconditional/withoutExternal.
     * This will not return Impure/LBImpure as they have to be handled seperately.
     */
    private def apply(flags: Int): Purity = {
        (flags: @switch) match {
            case PureWithoutAllocations.flags              ⇒ PureWithoutAllocations
            case LBSideEffectFreeWithoutAllocations.flags  ⇒ LBSideEffectFreeWithoutAllocations
            case LBPure.flags                              ⇒ LBPure
            case LBSideEffectFree.flags                    ⇒ LBSideEffectFree
            case CPureWithoutAllocations.flags             ⇒ CPureWithoutAllocations
            case CLBSideEffectFreeWithoutAllocations.flags ⇒ CLBSideEffectFreeWithoutAllocations
            case CLBPure.flags                             ⇒ CLBPure
            case CLBSideEffectFree.flags                   ⇒ CLBSideEffectFree
            // If `ModifiesReceiver` or `PerformsDomainSpecificOperations` is present,
            // we don't distinguish between purity levels with/without allocations anymore
            case _ ⇒ ((flags | HasAllocations): @switch) match {
                case LBExternallyPure.flags             ⇒ LBExternallyPure
                case LBExternallySideEffectFree.flags   ⇒ LBExternallySideEffectFree
                case CLBExternallyPure.flags            ⇒ CLBExternallyPure
                case CLBExternallySideEffectFree.flags  ⇒ CLBExternallySideEffectFree
                case LBDPure.flags                      ⇒ LBDPure
                case LBDSideEffectFree.flags            ⇒ LBDSideEffectFree
                case LBDExternallyPure.flags            ⇒ LBDExternallyPure
                case LBDExternallySideEffectFree.flags  ⇒ LBDExternallySideEffectFree
                case CLBDPure.flags                     ⇒ CLBDPure
                case CLBDSideEffectFree.flags           ⇒ CLBDSideEffectFree
                case CLBDExternallyPure.flags           ⇒ CLBDExternallyPure
                case CLBDExternallySideEffectFree.flags ⇒ CLBDExternallySideEffectFree
            }
        }
    }
}

/**
 * The respective method is pure and invoking it does not cause any heap objects to be allocated.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object PureWithoutAllocations extends Purity {
    final def isRefinable = false
    final val flags = 0 // <=> no flag is set

    override def combine(other: Purity): Purity = other
}

/**
 * The respective method is at least pure.
 *
 *  @see [[Purity]] for further details regarding the purity levels.
 */
case object LBPure extends Purity {
    final def isRefinable = true
    final val flags = PureFlags
}

/**
 * The respective method is at least side-effect free, i.e. it does not have side-effects but its
 * results may still be non-deterministic.
 * Additionally, invoking the method does not cause any heap objects to be allocated.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object LBSideEffectFreeWithoutAllocations extends Purity {
    final def isRefinable = true
    final val flags = SideEffectFreeWithoutAllocationsFlags
}

/**
 * The respective method is side-effect free, i.e. it does not have side-effects but its results may
 * still be non-deterministic.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object LBSideEffectFree extends Purity {
    final def isRefinable = true
    final val flags = SideEffectFreeFlags
}

/**
 * The respective method may modify its receiver, but is pure otherwise.
 *
 * A method calling a `ExternallyPure` method can be `Pure` if the receiver of the call is confined
 * inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object LBExternallyPure extends Purity {
    final def isRefinable = true
    final val flags = HasAllocations | ModifiesReceiver
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
case object LBExternallySideEffectFree extends Purity {
    final def isRefinable = true
    final val flags = ExternallySideEffectFreeFlags
}

/**
 * The respective method may perform actions that are generally considered impure or
 * non-deterministic that some clients may wish to treat as pure. Otherwise it is pure.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object LBDPure extends Purity {
    final def isRefinable = true
    final val flags = HasAllocations | PerformsDomainSpecificOperations
}

/**
 * The respective method may perform actions that are generally considered impure that some clients
 * may wish to treat as pure. Otherwise it is side-effect free.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object LBDSideEffectFree extends Purity {
    final def isRefinable = true
    final val flags = SideEffectFreeFlags | PerformsDomainSpecificOperations
}

/**
 * The respective method may perform actions that are generally considered impure or
 * non-deterministic that some clients may wish to treat as pure and it may modify its receiver.
 * Otherwise it is pure.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object LBDExternallyPure extends Purity {
    final def isRefinable = true
    final val flags = HasAllocations | ModifiesReceiver | PerformsDomainSpecificOperations
}

/**
 * The respective method may perform actions that are generally considered impure that some clients
 * may wish to treat as pure and it may modify its receiver. Otherwise it is side-effect free.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object LBDExternallySideEffectFree extends Purity {
    final def isRefinable = true
    final val flags = ExternallySideEffectFreeFlags | PerformsDomainSpecificOperations
}

sealed trait ConditionalPurity extends Purity {
    final def isRefinable = true // even in the context of a single phase
}

/**
 * Used, if the pureness of a method is dependent on other analysis results not yet available, but
 * the method does not itself allocate any heap objects.
 *
 * A method calling a `CPureWithoutAllocations` method can at most be
 * `CPureWithoutAllocations` itself, unless `CPureWithoutAllocations` is
 * refined to [[org.opalj.fpcf.properties.PureWithoutAllocations]].
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CPureWithoutAllocations extends ConditionalPurity {
    final val flags = IsConditional
}

/**
 * Used, if the pureness of a method is dependent on other analysis results not yet available.
 *
 * A method calling a `CLBPure` method can at most be `CLBPure` itself, unless
 * `CLBPure` is refined to [[LBPure]].
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CLBPure extends ConditionalPurity {
    final val flags = HasAllocations | IsConditional
}

/**
 * Used if the side-effect freeness of a method is dependent on other analysis results not yet
 * available, but the method does not itself allocate any heap objects.
 *
 * A method calling a `CLBSideEffectFreeWithoutAllocations` method can at most be
 * `CLBSideEffectFreeWithoutAllocations` itself, unless
 * `CLBSideEffectFreeWithoutAllocations` is refined to
 * [[LBSideEffectFreeWithoutAllocations]].
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CLBSideEffectFreeWithoutAllocations extends ConditionalPurity {
    final val flags = IsNonDeterministic | IsConditional
}

/**
 * Used if the side-effect freeness of a method is dependent on other analysis results not yet
 * available.
 *
 * A method calling a `CLBSideEffectFree` method can at most be
 * `CLBSideEffectFree` itself, unless `CLBSideEffectFree` is refined to
 * [[LBSideEffectFree]].
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CLBSideEffectFree extends ConditionalPurity {
    final val flags = HasAllocations | IsNonDeterministic | IsConditional
}

/**
 * Used if the method may modify its receiver and its purity is dependent on other analysis results
 * not yet available.
 *
 * A method calling a `CLBExternallyPure` method can be `CLBPure` if the
 * receiver of the call is confined inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CLBExternallyPure extends ConditionalPurity {
    final val flags = HasAllocations | ModifiesReceiver | IsConditional
}

/**
 * Used if the method may modify its receiver and its side-effect freeness is dependent on
 * other analysis results not yet available.
 *
 * A method calling a `CLBExternallySideEffectFree` method can be
 * `CLBSideEffectFree` if the receiver of the call is confined inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CLBExternallySideEffectFree extends ConditionalPurity {
    final val flags = ExternallySideEffectFreeFlags | IsConditional
}

/**
 * Used if a method may perform actions that are generally considered impure or
 * non-deterministic but which some clients wish to treat as pure. Additionally, the purity is
 * also dependent on other analysis results which are not yet available.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CLBDPure extends ConditionalPurity {
    final val flags = HasAllocations | PerformsDomainSpecificOperations | IsConditional
}

/**
 * Used if the method may perform actions that are generally considered impure that some clients may
 * wish to treat as pure and its side-effect freeness is dependent on other analysis results not yet
 * available.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CLBDSideEffectFree extends ConditionalPurity {
    final val flags = SideEffectFreeFlags | PerformsDomainSpecificOperations | IsConditional
}

/**
 * Used if the method may perform actions that are generally considered impure or
 * non-deterministic that some clients may wish to treat as pure, it may modify its receiver and its
 * purity is dependent on other anlysis results not yet available.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CLBDExternallyPure extends ConditionalPurity {
    final val flags = ExternallyPureFlags | PerformsDomainSpecificOperations | IsConditional
}

/**
 * Used if the method may perform actions that are generally considered impure, but that some
 * clients want to treat as pure. A method that is `CLBDExternallySideEffectFree` may modify its
 * receiver and its side-effect freeness is dependent on other entities for which the results are
 * not yet available.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CLBDExternallySideEffectFree extends ConditionalPurity {
    final val flags = {
        ExternallySideEffectFreeFlags | IsConditional | PerformsDomainSpecificOperations
    }
}

/**
 * Clients have to treat the method as impure. If the property is refinable clients can keep
 * the dependency.
 */
sealed abstract class ClassifiedImpure extends Purity

/**
 * The method needs to be treated as impure for the time being. However, the current
 * analysis is still waiting on the results of some dependencies.
 */
case object MaybePure extends ClassifiedImpure {

    final def isRefinable: Boolean = true

    final val flags = ImpureFlags | IsConditional

    override def combine(other: Purity): Purity = {
        other match {
            case Impure   ⇒ Impure
            case LBImpure ⇒ MaybePure
            case _        ⇒ this
        }
    }

    override def toString: String = "MaybePure"

    override def unconditional: Purity = LBImpure
}

/**
 * The method needs to be treated as impure for the time being. However, the current
 * analysis is not able to derive a more precise result; no more dependency exist.
 */
case object LBImpure extends ClassifiedImpure {

    final def isRefinable: Boolean = true

    final val flags = ImpureFlags

    override def combine(other: Purity): Purity = {
        other match {
            case Impure    ⇒ Impure
            case MaybePure ⇒ MaybePure
            case _         ⇒ this
        }
    }

    override def toString: String = "LBImpure"

}

/** The method is (finally classified as) impure; this also models the fallback. */
case object Impure extends ClassifiedImpure {

    final def isRefinable: Boolean = false

    final val flags = ImpureFlags

    override def combine(other: Purity): Purity = this

    override def toString: String = "Impure"

}
