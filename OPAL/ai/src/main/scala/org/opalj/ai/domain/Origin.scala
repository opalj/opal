/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package ai
package domain

/**
 * Provides information about the origin of a value.
 *
 * ==Usage==
 * An analysis that requires information about the origin of a value should declare
 * a corresponding self-type dependency. E.g.,
 * {{{
 * class MyAnalysis extends... {this: Origin =>
 *  ...
 * }
 * }}}
 * Additionally, the analysis has to make sure that the analysis used to capture
 * information about values provides the necessary information.
 *
 * ==Implementation==
 * This trait should be inherited from by all domains that make information about
 * the origin of a value available.
 *
 * @author Michael Eichberg
 */
trait Origin { domain: ValuesDomain ⇒

    implicit object SingleOriginValueOrdering extends Ordering[SingleOriginValue] {

        def compare(x: SingleOriginValue, y: SingleOriginValue): Int = {
            x.origin - y.origin
        }
    }

    /**
     * Should be mixed in by `Value`s that have a single origin.
     */
    trait SingleOriginValue {
        def origin: ValueOrigin
    }

    /**
     * Should be mixed in by `Value` classes that capture information about all origins
     * of a value.
     */
    trait MultipleOriginsValue {
        def origins: Iterable[ValueOrigin]
    }

    /**
     * Returns the origin(s) of the given value if the information is available.
     *
     * @return The source(s) of the given value if the information is available.
     *      Whether the information is available depends on the concrete domains.
     *      This trait only defines a general contract how to get access to a
     *      value's origin (I.e., the origin of the instruction which created the
     *      respective value.)
     *      By default this method returns an empty `Iterable`.
     */
    def origin(value: DomainValue): Iterable[ValueOrigin] =
        value match {
            case sov: SingleOriginValue    ⇒ Iterable[ValueOrigin](sov.origin)
            case mov: MultipleOriginsValue ⇒ mov.origins
            case _                         ⇒ Iterable.empty
        }

    def foreachOrigin(value: DomainValue, f: (ValueOrigin) ⇒ Unit) {
        value match {
            case sov: SingleOriginValue    ⇒ f(sov.origin)
            case mov: MultipleOriginsValue ⇒ mov.origins.foreach(f)
            case _                         ⇒ /* nothing to do */
        }
    }

}
