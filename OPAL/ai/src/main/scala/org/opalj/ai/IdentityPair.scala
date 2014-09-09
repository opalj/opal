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

/**
 * Encapsulates a pair of values that is intended to be used as a key in Maps.
 * Compared to a standard pair (Tuple2), however, comparison of two `IdentityPair`
 * objects is done by doing a reference-based
 * comparison of the stored values.
 *
 * @param _1 A non-null value.
 * @param _2 A non-null value.
 *
 * @author Michael Eichberg
 */
final case class IdentityPair[+T1 <: AnyRef, +T2 <: AnyRef](
        final val _1: T1,
        final val _2: T2) extends Product2[T1, T2] {

    override def canEqual(other: Any): Boolean = other.isInstanceOf[IdentityPair[_, _]]

    override def equals(other: Any): Boolean = {
        other match {
            case that: IdentityPair[_, _] ⇒ (this._1 eq that._1) && (this._2 eq that._2)
            case _                        ⇒ false
        }
    }

    override val hashCode: Int =
        System.identityHashCode(_1) * 113 + System.identityHashCode(_2)
}
