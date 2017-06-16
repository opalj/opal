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

sealed trait PurityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = Purity
}

/**
 * Describes the purity of a method. A method is pure; i.e., the method
 * only operates on the given state or depends on other state/mutable global
 * state; the given state may include the state of the
 * current object that is the receiver of the call if the object/receiver is immutable.
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
 *      Furthermore, instance method based calls can also be pure if
 *      the receiving object is (effectively final) or was created as part of the evaluation
 *      of the method.'''
 *
 *  -   Evaluation of the result does not cause any semantically observable side effect or output,
 *      such as mutation of mutable objects or output to I/O devices.
 *      The result value need not depend on all (or any) of the argument values. However, it must
 *      depend on nothing other than the argument values. The function may return multiple result
 *      values and these conditions must apply to all returned values for the function to be
 *      considered pure. If an argument is "call-by-reference", any parameter mutation will alter
 *      the value of the argument outside the function, which will render the function impure.
 *      '''However, if the referenced object is immutable it is ok.'''
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
        // When we have a cycle all properties are necessarily conditionally pure
        // hence, we can leverage the "pureness"
        Pure
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
 * The respective method is pure.
 */
case object Pure extends Purity { final val isRefineable = false }

/**
 * The respective method is impure.
 */
case object Impure extends Purity { final val isRefineable = false }
