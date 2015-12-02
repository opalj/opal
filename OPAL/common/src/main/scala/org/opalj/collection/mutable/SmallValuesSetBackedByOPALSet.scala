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
 * A small values set backed by an OPAL set.
 *
 * @param offset The value that needs to is subtracted from every value that is stored
 *          in the set to make sure all values are in the range [0,...]. The offset
 *          is added again before the value is returned.
 */
private[mutable] final class SmallValuesSetBackedByOPALSet(
        final val offset: Int,
        private val set:  SmallValuesSet
) extends SmallValuesSet {

    override def min = set.min + offset

    override def max = set.max + offset

    override def size = set.size

    override def isSingletonSet = set.isSingletonSet

    override def isEmpty = set.isEmpty

    override def mutableCopy: SmallValuesSetBackedByOPALSet = {
        new SmallValuesSetBackedByOPALSet(offset, set.mutableCopy)
    }

    override def +≈:(value: Int): SmallValuesSetBackedByOPALSet = {
        val shiftedValue = value - offset
        val set = this.set
        if (set.contains(shiftedValue))
            this
        else
            new SmallValuesSetBackedByOPALSet(offset, (shiftedValue) +≈: set)
    }

    override def -(value: Int): SmallValuesSet = {
        val set = this.set
        val newSet = set - (value - offset)
        if (newSet eq set)
            this
        else
            new SmallValuesSetBackedByOPALSet(offset, newSet)
    }

    override def contains(value: Int): Boolean = set.contains(value - offset)

    override def exists(f: Int ⇒ Boolean): Boolean = set.exists(rv ⇒ f(rv + offset))

    override def subsetOf(other: org.opalj.collection.SmallValuesSet): Boolean = {
        if (this eq other)
            true
        else
            set.forall(v ⇒ other.contains(v + offset))
    }

    override def foreach[U](f: Int ⇒ U): Unit = set.foreach(rv ⇒ f(rv + offset))

    override def forall(f: Int ⇒ Boolean): Boolean = set.forall(v ⇒ f(v + offset))

    override protected[collection] def mkString(
        start: String, sep: String, end: String,
        offset: Int
    ): String = {
        set.mkString(start, sep, end, offset)
    }

    override def mkString(start: String, sep: String, end: String): String = {
        mkString(start, sep, end, offset)
    }

    override def toString(): String = {
        mkString(
            s"SmallValuesSetBackedByOpalSet(offset=$offset;values={",
            ", ",
            "})",
            offset
        )
    }
}

