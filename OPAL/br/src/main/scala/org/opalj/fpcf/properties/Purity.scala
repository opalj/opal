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
 *      More precisely, methods are pure if the values they refer to are always (even across program
 *      runs) have an identical shape and the precise location in the heap is not relevant (e.g.
 *      java.lang.Object.hashCode and toString are not pure).'''
 *
 *  -   Evaluation of the result does not cause any semantically observable side effect or output,
 *      such as mutation of mutable objects or output to I/O devices.
 *      The result value need not depend on all (or any) of the argument values. However, it must
 *      depend on nothing other than the argument values. The function may return multiple result
 *      values and these conditions must apply to all returned values for the function to be
 *      considered pure. If an argument is "call-by-reference", any parameter mutation will alter
 *      the value of the argument outside the function, which will render the function impure.
 *
 * Based on these two requirements, purity of a method can have varying degrees, represented by the
 * subclasses of this property:
 *
 * [[Impure]] methods have no constraints on their behavior. They may have side effect and depend
 * on all accessible state. Analyses can always return [[Impure]] as a safe default value - even if
 * they are not able to prove that a method is Impure.
 *
 * [[SideEffectFree]] methods may still depend on all accessible state, but may not have any side
 * effects. In single-threaded execution, this means that the object graph of the program may not
 * have changed between invocation of the method and its return, except for potentially additional
 * objects allocated by the method. For multi-threaded execution, the object graph may not change
 * due to the invocation of the method (although it may change due to other methods executing on
 * concurrent threads). The method must not have any effects (beside consumption of resources like
 * memory and processor time) on methods executing concurrently, in particular it may not acquire
 * any locks on objects that concurrent methods could also try to acquire. Analyses may return
 * [[SideEffectFree]] as a safe default value if they are unable to guarantee that a method is
 * [[Pure]], even if it is, as long as they can guarantee that the method does not have any side
 * effects.
 *
 * [[Pure]] methods must be side effect free as above, but additionally, their result may only
 * depend on their parameters (including the receiver object) and global constants. In particular,
 * the result of a Pure method must be structurally identical each time the method is invoked with
 * structurally identical parameters. Pure methods may depend on the aliasing relation between their
 * parameters or between their parameters and global constants. In multi-threaded execution, Pure
 * methods can not depend on any mutable state of their parameters if that state might be mutated
 * by concurrently executing methods. Analyses may return [[Pure]] only if they are able to
 * guarantee that a method fulfills these requirements. Otherwise they must return a weaker safe
 * value as stated above.
 *
 * [[ConditionallySideEffectFree]] and [[ConditionallyPure]] can be used by analyses to specify
 * intermediate results. ConditionallySideEffect free methods are methods that may still become
 * [[SideEffectFree]], but that result depends on other analysis results not yet available.
 * ConditionallySideEffectFree methods may not become [[Pure]] anymore. ConditionallyPure methods
 * are methods that may still become [[Pure]], but that result depends on other analysis results not
 * yet available. Depending on these results, ConditionallyPure methods might also become [[Impure]]
 * or [[SideEffectFree]], or, for another intermediate result, [[ConditionallySideEffectFree]].
 *
 * [[MaybePure]] is used as a default fallback value if no purity information could be computed for
 * a method. Clients must treat this in the same way as [[Impure]].
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
sealed abstract class Purity extends Property with PurityPropertyMetaInformation {

    /**
     * Returns the key used by all `Purity` properties.
     */
    // All instances have to share the SAME key!
    final def key = Purity.key
}

object Purity extends PurityPropertyMetaInformation {

    /**
     * The key associated with every purity property.
     */
    final val key: PropertyKey[Purity] = PropertyKey.create(
        // The unique name of the property.
        "Purity",
        // The default property that will be used if no analysis is able
        // to (directly) compute the respective property.
        MaybePure,
        // When we have a cycle, we can leverage the "purity" if all properties are either
        // conditionally pure or conditionally side-effect free
        (propertyStore: PropertyStore, epks: SomeEPKs) ⇒ {
            val purity: Purity = epks.foldLeft(Pure.asInstanceOf[Purity]) { (purity, epk) ⇒
                epk match {
                    case EPK(e, `key`) if purity == Pure && propertyStore(e, key).p == ConditionallyPure ⇒
                        Pure
                    case EPK(e, `key`) if purity != MaybePure ⇒
                        val p = propertyStore(e, key).p
                        if (p == ConditionallyPure || p == ConditionallySideEffectFree)
                            SideEffectFree
                        else
                            MaybePure
                    case _ ⇒ MaybePure
                }
            }
            Iterable(Result(epks.head.e, purity))
        }
    // NOTE
    // We DO NOT increase the pureness of all methods as this will happen automatically
    // as a sideeffect of setting the pureness of one method!
    // (epks: Iterable[EPK]) ⇒ { epks.map(epk ⇒ Result(epk.e, Pure)) }
    )
}

/**
 * The fallback/default purity.
 *
 * It is only used by the framework in case of a dependency
 * on an element for which no result could be computed.
 */
case object MaybePure extends Purity { final val isRefineable = true }

/**
 * Used if we know that the pureness of a methods only depends on the pureness
 * of the target methods.
 *
 * A method calling a ConditionallyPure method can at most be ConditionallyPure itself, unless
 * ConditionallyPure is refined to [[Pure]].
 */
case object ConditionallyPure extends Purity { final val isRefineable = true }

/**
 * Used if we know that side-effect freeness of a method only depends on the
 * side-effect freeness of the target methods.
 *
 * A method calling a ConditionallySideEffectFree method can at most be ConditionallySideEffectFree
 * itself, unless ConditionallySideEffectFree is refined to [[SideEffectFree]].
 */
case object ConditionallySideEffectFree extends Purity { final val isRefineable = true }

/**
 * The respective method is side-effect free, i.e. it does not have side-effects but
 * its results may be non-deterministic.
 */
case object SideEffectFree extends Purity { final val isRefineable = false }

/**
 * The respective method is pure.
 */
case object Pure extends Purity { final val isRefineable = false }

/**
 * The respective method is impure.
 */
case object Impure extends Purity { final val isRefineable = false }
