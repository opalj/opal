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
 * A sorted set of unsigned short values.
 *
 * @author Michael Eichberg
 */
trait UShortSet {

    /**
     * Returns a new set that contains this set's values and the given value.
     *
     * Even if the given value is already in this set a fresh copy is returned unless
     * this set is already "full". In the latter case `this` set is returned.
     * This porperty ensures that the set appears to be immutable.
     */
    def +(value: UShort): UShortSet

    /**
     * Creates a clone of this set that can safely be mutated.
     */
    def mutableCopy: mutable.UShortSet

    /**
     * Returns `true` if this set contains the given value.
     *
     * If the given value is not an unsigned short value ([0..65535]) the
     * result is undefined.
     */
    def contains(ushortValue: UShort): Boolean

    /**
     * Executes the given function `f` for each value of this set, starting with
     * the smallest value.
     */
    def foreach(f: UShort ⇒ Unit): Unit

    /**
     * Returns `true` if the predicate `f` returns true for all values of the set.
     */
    def forall(f: UShort ⇒ Boolean): Boolean

    def map[T](f: UShort ⇒ T): scala.collection.mutable.Set[T] = {
        val set = scala.collection.mutable.Set.empty[T]
        foreach(v ⇒ set += f(v))
        set
    }

    def mapToList[T](f: UShort ⇒ T): List[T] = {
        var result = List.empty[T]
        foreach(v ⇒ result = f(v) :: result)
        result
    }

    def filter(f: UShort ⇒ Boolean): UShortSet = {
        var result: mutable.UShortSet = mutable.UShortSet.empty
        foreach(v ⇒ if (f(v)) result = v +≈: result)
        result
    }

    /**
     * Returns a new `Iterator`. The iterator is primarily defined to facilitate
     * the integration with Scala's standard collections API.
     *
     * @note Whenever possible try to use this set's native methods
     *      (e.g., foreach and contains) as they are guaranteed to be optimized for
     *      performance.
     */
    def iterator: Iterator[UShort] = iterable.iterator

    /**
     * Returns a new `Iterable`. The method is primarily defined to facilitate
     * the integration with Scala's standard collections API.
     *
     * @note Whenever possible try to use this set's native methods
     *      (e.g., foreach and contains) as they are guaranteed to be optimized for
     *      performance.
     */
    def iterable: Iterable[Int]

    /**
     * The maximum value in this set.
     */
    def max: UShort

    /**
     * The last value in this set which is also the maximum value.
     */
    def last: UShort = max

    /**
     * The number of elements of this set.
     *
     * @note The size is calculated using an iterator, hence its complexity is O(n).
     */
    def size: Int = iterator.size

    /**
     * Returns `true` if this set is empty.
     */
    def isEmpty: Boolean

    def nonEmpty: Boolean = !isEmpty

    override def toString: String = iterator.mkString("UShortSet(", ",", ")")
}