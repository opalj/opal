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

private[mutable] final class SmallValuesSetBackedByScalaSet(
        private[this] var set: Set[Int] = Set.empty
) extends SmallValuesSet {

    def this(value: Int) { this(Set(value)) }

    override def min = set.min

    override def max = set.max

    override def isEmpty = set.isEmpty

    override def isSingletonSet = set.size == 1

    override def size = set.size

    override def +≈:(value: Int): SmallValuesSetBackedByScalaSet = {
        val set = this.set
        val newSet = set + value
        if (newSet eq set /* <=> the value was already stored in the set */ )
            this
        else
            return new SmallValuesSetBackedByScalaSet(newSet)
    }

    override def -(value: Int): SmallValuesSet = {
        val set = this.set
        val newSet = set - value
        if (newSet eq set)
            this
        else
            new SmallValuesSetBackedByScalaSet()
    }

    override def mutableCopy: SmallValuesSetBackedByScalaSet =
        this // every subsequent mutation results in a new set object anyway

    override def contains(value: Int): Boolean = set.contains(value)

    override def exists(f: Int ⇒ Boolean): Boolean = set.exists(f)

    override def subsetOf(other: org.opalj.collection.SmallValuesSet): Boolean = {
        if (this eq other)
            true
        else
            set.forall(v ⇒ other.contains(v))
    }

    override def foreach[U](f: Int ⇒ U): Unit = set.foreach(rv ⇒ f(rv))

    override def forall(f: Int ⇒ Boolean): Boolean = set.forall(v ⇒ f(v))

    override protected[collection] def mkString(
        pre: String, sep: String, pos: String,
        offset: Int
    ): String =
        set.view.map(_ + offset).mkString(pre, sep, pos)

    override def mkString(start: String, sep: String, end: String): String =
        mkString(start, sep, end, 0)

    override def toString(): String = {
        mkString(
            s"SmallValuesSetBackedByScalaSet(",
            ", ",
            ")",
            0
        )
    }
}

