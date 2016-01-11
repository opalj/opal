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
package mutable

/**
 * A memory-efficient, '''semi-mutable''', sorted set of small values that
 * is highly tailored for small(er) sets.
 *
 * Compared to a standard Scala set, an Int or Long value is used to store multiple
 * values (e.g. a long value (64bit) can be used to store up to four unsigned short
 * values(16bit)).
 *
 * @author Michael Eichberg
 */
trait SmallValuesSet extends org.opalj.collection.SmallValuesSet {

    /**
     * Adds the given value to this set if it is not already contained in this set.
     * If this set has enough space to hold the additional value, a reference to this
     * set is returned. Otherwise, a new set is created and a reference to that set
     * is returned. Hence, '''the return value must not be ignored'''.
     *
     * @param value The value that is added. If `value` is not in the range specified at
     *        creation time the result is undefined.
     * @return The "new" set with the given value.
     */
    def +≈:(value: Int): MutableSmallValuesSet

    /**
     * Adds the values to this set if they are not already contained in this set.
     * If this set has enough space to hold the additional values, a reference to this
     * set is returned. Otherwise, a new set is created and a reference to that set
     * is returned. Hence, '''the return value must not be ignored'''.
     *
     * @param value The value that is added. If `value` is not in the range specified at
     *        creation time the result is undefined.
     * @return The "new" set with the given value.
     */
    def ++≈:(values: SmallValuesSet): MutableSmallValuesSet = {
        var newSet = this
        values foreach { v ⇒ newSet = v +≈: newSet }
        newSet
    }

    def -(value: Int): MutableSmallValuesSet /* Redefines the return type. */

    def mutableCopy: MutableSmallValuesSet /* Redefines the return type. */

    def filter(f: Int ⇒ Boolean): SmallValuesSet
}

/**
 * Factory to create [[SmallValuesSet]]s.
 *
 * @author Michael Eichberg
 */
object SmallValuesSet {

    /**
     * Creates a new empty set that can store values in the range `[0,max]`.
     *
     * The behavior of the returned set is undefined if a value should be stored in it
     * which is outside of the specified range!
     */
    def empty(max: Int): SmallValuesSet = {
        if (max <= UByte.MaxValue)
            EmptyUByteSet
        else if (max <= UShort.MaxValue)
            EmptyUShortSet
        else
            new SmallValuesSetBackedByScalaSet(0)
    }

    /**
     * Creates a new set that can store values in the range `[0,max]` and which contains
     * the given `value`.
     *
     * The behavior of the returned set is undefined if a value should be stored in it
     * which is outside of the specified range!
     */
    def create(max: Int, value: Int): SmallValuesSet = {
        if (max <= UByte.MaxValue)
            UByteSet(value)
        else if (max <= UShort.MaxValue)
            UShortSet(value)
        else
            new SmallValuesSetBackedByScalaSet(0)
    }

    /**
     * Creates a new empty set that can store values in the range `[min,max]`.
     *
     * The behavior of the returned set is undefined if a value should be stored in it
     * which is outside of the specified range!
     */
    def empty(min: Int, max: Int): SmallValuesSet = {
        if (min == 0) {
            empty(max)
        } else {
            val size = max - min
            if (size <= UByte.MaxValue)
                new SmallValuesSetBackedByOPALSet(min, EmptyUByteSet)
            else if (size <= UShort.MaxValue)
                new SmallValuesSetBackedByOPALSet(min, EmptyUShortSet)
            else
                new SmallValuesSetBackedByScalaSet()
        }
    }

    /**
     * Creates a new set that can store values in the range `[min,max]` and which
     * contains the given `value`.
     *
     * The behavior of the returned set is undefined if a value should be stored in it
     * which is outside of the specified range!
     */
    def create(min: Int, max: Int, value: Int): SmallValuesSet = {
        if (min == 0) {
            create(max, value)
        } else {
            val size = max - min
            if (size <= UByte.MaxValue)
                new SmallValuesSetBackedByOPALSet(min, UByteSet(value - min))
            else if (size <= UShort.MaxValue)
                new SmallValuesSetBackedByOPALSet(min, UShortSet(value - min))
            else
                new SmallValuesSetBackedByScalaSet(value)
        }
    }
}

