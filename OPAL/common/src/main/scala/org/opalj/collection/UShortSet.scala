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
package collection

/**
 * A compact, sorted set of unsigned short values.
 *
 * @author Michael Eichberg
 */
trait UShortSet extends SmallValuesSet {

    /**
     * Adds the given value to this set's values.
     *
     * Even if the given value is already in this set a fresh copy is returned unless
     * this set is already "full". In the latter case `this` set is returned.
     * This property ensures that the set appears to be immutable.
     */
    def +(value: UShort): UShortSet

    def ++(values: UShortSet): mutable.UShortSet = {
        super[SmallValuesSet].++(values).asInstanceOf[mutable.UShortSet]
    }

    def max: Int /* Redefined to avoid ambiguous references. */

    def min: Int /* Redefined to avoid ambiguous references. */

    final override def last: Int = max

    final override def head: Int = min

    final override def nonEmpty: Boolean = super[SmallValuesSet].nonEmpty

    /**
     * Returns a (new) set object that can safely be mutated.
     */
    def mutableCopy: mutable.UShortSet

    /**
     * Returns `true` if this set contains the given value.
     *
     * If the given value is not an unsigned short value ([0..65535]) the
     * result is undefined.
     */
    def contains(value: UShort): Boolean

    def map[T](f: UShort ⇒ T): scala.collection.mutable.Set[T] = {
        val set = scala.collection.mutable.Set.empty[T]
        foreach(v ⇒ set += f(v))
        set
    }

    /**
     * Maps all values to a list, reversing the order in which the elements occurred
     * in the (sorted) set.
     */
    def mapToList[T](f: UShort ⇒ T): List[T] = {
        var result = List.empty[T]
        foreach(v ⇒ result = f(v) :: result)
        result
    }

    def filter(f: UShort ⇒ Boolean): mutable.UShortSet = {
        var result: mutable.UShortSet = mutable.UShortSet.empty
        foreach(v ⇒ if (f(v)) result = v +≈: result)
        result
    }

    def foldLeft[T](i: T)(f: (T, UShort) ⇒ T): T = {
        var value = i;
        foreach { v ⇒ value = f(value, v) }
        value
    }

    /**
     * Returns a new `Iterator`. The iterator is primarily defined to facilitate
     * the integration with Scala's standard collections API.
     *
     * @note Whenever possible try to use this set's native methods
     *      (e.g., foreach and contains) as they are guaranteed to be optimized for
     *      performance.
     */
    def iterator: Iterator[UShort]

    /**
     * Returns a new `Iterable`. The method is primarily defined to facilitate
     * the integration with Scala's standard collections API.
     *
     * @note Whenever possible try to use this set's native methods
     *      (e.g., foreach and contains) as they are guaranteed to be optimized for
     *      performance.
     */
    def iterable: Iterable[UShort]

    override def toString: String = iterator.mkString("UShortSet(", ",", ")")
}

object UShortSet {

    final def empty = org.opalj.collection.mutable.UShortSet.empty
}
