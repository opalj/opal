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

/**
 * A trait to conveniently implement properties that need aggregated values.
 * Provides conversion between aggregated and individual variants.
 *
 * @tparam T Type of the property
 *
 * @author Dominik Helm
 */
trait AggregatableValueProperty[S <: IndividualProperty[S, T], T <: AggregatedProperty[S, T]]
    extends Property {
    type self

    def meet(other: self): self
}

trait IndividualProperty[S <: IndividualProperty[S, T], T <: AggregatedProperty[S, T]]
    extends AggregatableValueProperty[S, T] {
    override type self = S

    val aggregatedProperty: T
}

trait AggregatedProperty[S <: IndividualProperty[S, T], T <: AggregatedProperty[S, T]]
    extends AggregatableValueProperty[S, T] {
    override type self = T

    val individualProperty: S

    override def meet(other: T): T =
        other.individualProperty.meet(this.individualProperty).aggregatedProperty
}
