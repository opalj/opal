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

import scala.annotation.tailrec
import scala.reflect.ClassTag

import Locals._

/**
 * Conceptually, an array that enables random access and which is heavily optimized for
 * small(er) collections (up to 12 elements) that are frequently compared and updated
 * and where sharing is beneficial.
 *
 * A `Locals` array contains `null` values for all elements that are not yet set.
 * Furthermore, a `Locals` array is not resizable.
 *
 * ==Usage Scenario==
 * For example, the median of the number of registers that are used per method is 2
 * (JDK and OPAL) and more then 99,5% of all methods have less than 20 elements.
 *
 * @author Michael Eichberg
 */
sealed trait Locals[T >: Null <: AnyRef] {

    /* ABSTRACT */ def size: Int

    /* ABSTRACT */ def isEmpty: Boolean

    /* ABSTRACT */ def nonEmpty: Boolean

    /* ABSTRACT */ def apply(index: Int): T

    /* ABSTRACT */ def set(index: Int, value: T): Unit

    /* ABSTRACT */ def update(f: (T) ⇒ T): Unit

    /* ABSTRACT */ def updated(index: Int, value: T): Locals[T]

    /* ABSTRACT */ def foreach(f: T ⇒ Unit): Unit

    /* ABSTRACT */ def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals[T]

    /* ABSTRACT */ def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals[X]

    /**
     * Transforms the values of this locals array. If all values are the same as
     * before `this` is returned.
     */
    /* ABSTRACT */ def transform(f: T ⇒ T): Locals[T]

    /**
     * Returns `true` if all elements satisfy the given predicate, `false` otherwise.
     */
    def forall[X >: T](f: X ⇒ Boolean): Boolean = {
        foreach { e ⇒ if (!f(e)) return false }
        true
    }

    /**
     * Returns `true` if an element satisfies the given predicate, `false` otherwise.
     */
    def exists[X >: T](f: X ⇒ Boolean): Boolean = {
        foreach { e ⇒ if (f(e)) return true }
        false
    }

    /**
     * Returns `true` if the given element is already in this list, `false` otherwise.
     */
    def contains[X >: T](o: X): Boolean = {
        foreach { e ⇒ if (e == o) return true }
        false
    }

    /**
     * Returns the first element that satisfies the given predicate.
     */
    def find[X >: T](f: X ⇒ Boolean): Option[T] = {
        foreach { e ⇒ if (f(e)) return Some(e) }
        None
    }

    /**
     * Creates a new vector which contains the mapped values as specified by the given
     * function `f`.
     *
     * @param f The function that converts this collection's elements. `f` has to
     *      be able to handle `null` values if this collection may contain `null` values.
     */
    def mapToVector[X](f: T ⇒ X): scala.collection.immutable.Vector[X] = {
        var newLocals = scala.collection.immutable.Vector.empty[X]
        foreach { e ⇒ newLocals :+ f(e) }
        newLocals
    }

    /**
     * Performs a fold left over all elements of this set.
     */
    def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = {
        var result: B = b
        foreach { elem ⇒ result = op(result, elem) }
        result
    }

    /**
     * Creates a Scala sequence. The sequence may contain `null` values.
     */
    def toSeq: Seq[T] = {
        var seq = List.empty[T]
        foreach { e ⇒ seq = e :: seq }
        seq
    }

    def zipWithIndex: Iterator[(T, Int)] = {
        new Iterator[(T, Int)] {
            var index = 0
            def hasNext = index < Locals.this.size
            def next = {
                val currentValue = Locals.this.apply(index)
                val currentIndex = index
                index += 1
                (currentValue, currentIndex)
            }
        }
    }

    def zip(other: Locals[T]): Iterator[(T, T)] = {
        new Iterator[(T, T)] {
            var index = 0
            def hasNext: Boolean = index < Locals.this.size
            def next: (T, T) = {
                val thisValue = Locals.this.apply(index)
                val otherValue = other(index)
                index += 1
                (thisValue, otherValue)
            }
        }
    }

    def corresponds[U >: Null <: AnyRef](
        other: Locals[U])(
            compare: (T, U) ⇒ Boolean): Boolean = {
        this.size == other.size &&
            {
                var i = 0
                while (i < this.size) {
                    if (!compare(this(i), other(i)))
                        return false;
                    i += 1
                }
                true
            }
    }

    def iterator: Iterator[T] = {
        new Iterator[T] {
            var index = 0
            def hasNext = index < Locals.this.size
            def next = {
                val currentValue = Locals.this.apply(index)
                index += 1
                currentValue
            }
        }
    }

    /*ABSTRACT*/ override def equals(other: Any): Boolean

    override def hashCode: Int = {
        var hc = 1
        foreach { e ⇒ hc * 41 + { if (e ne null) e.hashCode else 7 } }
        hc
    }

    def mkString(start: String, sep: String, end: String): String = {
        var s = ""
        var pre = start
        for { e ← this } {
            s = s + pre + e
            pre = sep
        }
        if (s == "")
            start + end
        else
            s + end
    }

    override def toString: String =
        mkString("org.opalj.collection.immutable.Locals(", ",", ")")

}

private[mutable] final object Locals0 extends Locals[Null] {

    final override val size = 0

    final override val isEmpty = true

    final override val nonEmpty = false

    override def apply(index: Int): Nothing =
        throw new IndexOutOfBoundsException("the vector has size 0")

    override def set(index: Int, value: Null): Unit =
        throw new IndexOutOfBoundsException("the vector has size 0")

    override def update(f: (Null) ⇒ Null): Unit = { /*nothing to do*/ }

    override def updated(index: Int, newValue: Null): Locals0.type =
        throw new IndexOutOfBoundsException("the vector has size 0")

    def merge(other: Locals[Null], onDiff: (Null, Null) ⇒ Null): this.type =
        if (this eq other)
            this
        else
            // thrown to make the exception homogenous
            throw new ClassCastException(other+" cannot be cast to Locals0")

    override def foreach(f: Null ⇒ Unit): Unit = { /*nothing to do*/ }

    override def map[X >: Null <: AnyRef: ClassTag](f: Null ⇒ X): Locals[X] = {
        this.asInstanceOf[Locals[X]]
    }

    override def transform(f: Null ⇒ Null) = this

    override def equals(other: Any): Boolean = {
        other match {
            case Locals0 ⇒ true
            case _       ⇒ false
        }
    }
}

private[mutable] sealed abstract class LocalsX[T >: Null <: AnyRef] extends Locals[T] {

    final override def isEmpty = false

    final override def nonEmpty = true

    override def equals(other: Any): Boolean = {
        other match {
            case that: LocalsX[_] if this.size == that.size ⇒
                var i = this.size - 1
                while (i >= 0) {
                    if (this(i) != that(i))
                        return false
                    i -= 1
                }
                true
            case _ ⇒ false
        }
    }
}

private[mutable] final class Locals1[T >: Null <: AnyRef](
        private var v: T = null) extends LocalsX[T] {

    final override def size = 1

    override def apply(index: Int): T = {
        // if (index != 0) throw new IndexOutOfBoundsException("invalid index("+index+")")
        v
    }

    override def set(index: Int, value: T): Unit = {
        // if (index != 0) throw new IndexOutOfBoundsException("invalid index("+index+")")
        this.v = value
    }

    override def update(f: (T) ⇒ T): Unit = {
        this.v = f(this.v)
    }

    override def updated(index: Int, newValue: T): Locals1[T] = {
        // if (index != 0) throw new IndexOutOfBoundsException("invalid index("+index+")")
        new Locals1(newValue)
    }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals1[T] = {
        val that = other.asInstanceOf[Locals1[T]]
        val thisV = this.v
        val thatV = that.v
        if (thisV eq thatV)
            this
        else {
            // Locals1 is left-right stabilized
            val newV = onDiff(thisV, thatV)
            if (newV eq thatV) that
            else if (newV eq thisV) this
            else new Locals1(newV)
        }
    }

    override def foreach(f: T ⇒ Unit): Unit = { f(v) }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals1[X] = {
        new Locals1[X](f(v))
    }

    override def transform(f: T ⇒ T): Locals1[T] = {
        val thisV = v
        val newV = f(thisV)
        if (newV eq thisV)
            this
        else
            new Locals1(newV)
    }
}

private[mutable] final class Locals2[T >: Null <: AnyRef](
        private var v0: T = null,
        private var v1: T = null) extends LocalsX[T] {

    final override def size = 2

    override def apply(index: Int): T = {
        if (index == 0) v0 else v1
        // (index: @scala.annotation.switch) match {
        //  case 0 ⇒ v0
        //  case 1 ⇒ v1
        //  case _ ⇒
        //   throw new IndexOutOfBoundsException("invalid index("+index+")")
        // }
    }

    override def set(index: Int, value: T): Unit = {
        (index: @scala.annotation.switch) match {
            case 0 ⇒ v0 = value
            case 1 ⇒ v1 = value
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def update(f: (T) ⇒ T): Unit = {
        this.v0 = f(this.v0)
        this.v1 = f(this.v1)
    }

    override def updated(index: Int, newValue: T): Locals2[T] = {
        (index: @scala.annotation.switch) match {
            case 0 ⇒ new Locals2(newValue, v1)
            case 1 ⇒ new Locals2(v0, newValue)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals2[T] = {
        val that = other.asInstanceOf[Locals2[T]]
        var useThis = true
        var useThat = true
        val newV0 = {
            val thisV0 = this.v0
            val thatV0 = that.v0
            if (thisV0 eq thatV0)
                thisV0
            else {
                val newV = onDiff(thisV0, thatV0)
                if (newV ne thisV0) useThis = false
                if (newV ne thatV0) useThat = false
                newV
            }
        }

        val thisV1 = this.v1
        val thatV1 = that.v1
        if (thisV1 eq thatV1) {
            if (useThis) this
            else if (useThat) that
            else new Locals2(newV0, thisV1)
        } else {
            val newV = onDiff(thisV1, thatV1)
            if ((newV eq thatV1) && useThat) that
            else if ((newV eq thisV1) && useThis) this
            else new Locals2(newV0, newV)
        }
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals2[X] = {
        new Locals2[X](f(v0), f(v1))
    }

    override def transform(f: T ⇒ T): Locals2[T] = {
        val thisV0 = v0
        val newV0 = f(thisV0)
        val thisV1 = v1
        val newV1 = f(thisV1)
        if ((newV0 eq thisV0) && (newV1 eq thisV1))
            this
        else
            new Locals2(newV0, newV1)
    }

    override def foreach(f: T ⇒ Unit): Unit = { f(v0); f(v1) }
}

private[mutable] final class Locals3[T >: Null <: AnyRef](
        private var v0: T = null,
        private var v1: T = null,
        private var v2: T = null) extends LocalsX[T] {

    final override def size = 3

    override def apply(index: Int): T = {
        index match {
            case 0 ⇒ v0
            case 1 ⇒ v1
            case 2 ⇒ v2
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def set(index: Int, value: T): Unit = {
        (index: @scala.annotation.switch) match {
            case 0 ⇒ v0 = value
            case 1 ⇒ v1 = value
            case 2 ⇒ v2 = value
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def update(f: (T) ⇒ T): Unit = {
        this.v0 = f(this.v0)
        this.v1 = f(this.v1)
        this.v2 = f(this.v2)
    }

    override def updated(index: Int, newValue: T): Locals3[T] = {
        (index: @scala.annotation.switch) match {
            case 0 ⇒ new Locals3(newValue, v1, v2)
            case 1 ⇒ new Locals3(v0, newValue, v2)
            case 2 ⇒ new Locals3(v0, v1, newValue)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals3[T] = {
        val that = other.asInstanceOf[Locals3[T]]
        var useThis = true
        var useThat = true

        val newV0 = {
            val thisV0 = this.v0
            val thatV0 = that.v0
            if (thisV0 eq thatV0)
                thisV0
            else {
                val newV = onDiff(thisV0, thatV0)
                if (newV ne thisV0) useThis = false
                if (newV ne thatV0) useThat = false
                newV
            }
        }
        val newV1 = {
            val thisV1 = this.v1
            val thatV1 = that.v1
            if (thisV1 eq thatV1)
                thisV1
            else {
                val newV = onDiff(thisV1, thatV1)
                if (newV ne thisV1) useThis = false
                if (newV ne thatV1) useThat = false
                newV
            }
        }
        val newV2 = {
            val thisV2 = this.v2
            val thatV2 = that.v2
            if (thisV2 eq thatV2)
                thisV2
            else {
                val newV = onDiff(thisV2, thatV2)
                if (newV ne thisV2) useThis = false
                if (newV ne thatV2) useThat = false
                newV
            }
        }

        if (useThat)
            that
        else if (useThis)
            this
        else
            new Locals3(newV0, newV1, newV2)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals3[X] = {
        new Locals3[X](f(v0), f(v1), f(v2))
    }

    override def transform(f: T ⇒ T): Locals3[T] = {
        val thisV0 = v0
        val newV0 = f(thisV0)
        val thisV1 = v1
        val newV1 = f(thisV1)
        val thisV2 = v2
        val newV2 = f(thisV2)
        if ((newV0 eq thisV0) && (newV1 eq thisV1) && (newV2 eq thisV2))
            this
        else
            new Locals3(newV0, newV1, newV2)
    }

    override def foreach(f: T ⇒ Unit): Unit = { f(v0); f(v1); f(v2) }
}

private[mutable] final class Locals4[T >: Null <: AnyRef](
        private var v0: T = null,
        private var v1: T = null,
        private var v2: T = null,
        private var v3: T = null) extends LocalsX[T] {

    final override def size = 4

    override def apply(index: Int): T = {
        (index: @scala.annotation.switch) match {
            case 0 ⇒ v0
            case 1 ⇒ v1
            case 2 ⇒ v2
            case 3 ⇒ v3
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def set(index: Int, value: T): Unit = {
        (index: @scala.annotation.switch) match {
            case 0 ⇒ v0 = value
            case 1 ⇒ v1 = value
            case 2 ⇒ v2 = value
            case 3 ⇒ v3 = value
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def update(f: (T) ⇒ T): Unit = {
        this.v0 = f(this.v0)
        this.v1 = f(this.v1)
        this.v2 = f(this.v2)
        this.v3 = f(this.v3)
    }

    override def updated(index: Int, newValue: T): Locals4[T] = {
        (index: @scala.annotation.switch) match {
            case 0 ⇒ new Locals4(newValue, v1, v2, v3)
            case 1 ⇒ new Locals4(v0, newValue, v2, v3)
            case 2 ⇒ new Locals4(v0, v1, newValue, v3)
            case 3 ⇒ new Locals4(v0, v1, v2, newValue)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals4[T] = {
        val that = other.asInstanceOf[Locals4[T]]
        var useThis = true
        var useThat = true

        val newV0 = {
            val thisV0 = this.v0
            val thatV0 = that.v0
            if (thisV0 eq thatV0)
                thisV0
            else {
                val newV = onDiff(thisV0, thatV0)
                if (newV ne thisV0) useThis = false
                if (newV ne thatV0) useThat = false
                newV
            }
        }
        val newV1 = {
            val thisV1 = this.v1
            val thatV1 = that.v1
            if (thisV1 eq thatV1)
                thisV1
            else {
                val newV = onDiff(thisV1, thatV1)
                if (newV ne thisV1) useThis = false
                if (newV ne thatV1) useThat = false
                newV
            }
        }
        val newV2 = {
            val thisV2 = this.v2
            val thatV2 = that.v2
            if (thisV2 eq thatV2)
                thisV2
            else {
                val newV = onDiff(thisV2, thatV2)
                if (newV ne thisV2) useThis = false
                if (newV ne thatV2) useThat = false
                newV
            }
        }
        val newV3 = {
            val thisV3 = this.v3
            val thatV3 = that.v3
            if (thisV3 eq thatV3)
                thisV3
            else {
                val newV = onDiff(thisV3, thatV3)
                if (newV ne thisV3) useThis = false
                if (newV ne thatV3) useThat = false
                newV
            }
        }

        if (useThat)
            that
        else if (useThis)
            this
        else
            new Locals4(newV0, newV1, newV2, newV3)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals4[X] = {
        new Locals4[X](f(v0), f(v1), f(v2), f(v3))
    }

    override def transform(f: T ⇒ T): Locals4[T] = {
        val thisV0 = v0
        val newV0 = f(thisV0)
        val thisV1 = v1
        val newV1 = f(thisV1)
        val thisV2 = v2
        val newV2 = f(thisV2)
        val thisV3 = v3
        val newV3 = f(thisV3)
        if ((newV0 eq thisV0) && (newV1 eq thisV1) && (newV2 eq thisV2) && (newV3 eq thisV3))
            this
        else
            new Locals4(newV0, newV1, newV2, newV3)
    }

    final override def foreach(f: T ⇒ Unit): Unit = { f(v0); f(v1); f(v2); f(v3) }
}

private[mutable] final class Locals5[T >: Null <: AnyRef](
        final val vs1: Locals2[T] = new Locals2[T],
        final val vs2: Locals3[T] = new Locals3[T]) extends LocalsX[T] {

    final def size = 5

    override def apply(index: Int): T = {
        if (index < 2) vs1(index) else vs2(index - 2)
    }

    override def set(index: Int, newValue: T): Unit = {
        if (index < 2) vs1.set(index, newValue) else vs2.set(index - 2, newValue)
    }

    override def update(f: (T) ⇒ T): Unit = {
        vs1.update(f)
        vs2.update(f)
    }

    override def updated(index: Int, newValue: T): Locals[T] = {
        if (index < 2)
            new Locals5(vs1.updated(index, newValue), vs2)
        else
            new Locals5(vs1, vs2.updated(index - 2, newValue))
    }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals5[T] = {
        val that = other.asInstanceOf[Locals5[T]]
        var useThis = true
        var useThat = true
        val newVs1 = {
            val thisVs1 = this.vs1
            val thatVs1 = that.vs1
            if (thisVs1 eq thatVs1)
                thisVs1
            else {
                val newVs = thisVs1.merge(thatVs1, onDiff)
                if (newVs ne thisVs1) useThis = false
                if (newVs ne thatVs1) useThat = false
                newVs
            }
        }
        val newVs2 = {
            val thisVs2 = this.vs2
            val thatVs2 = that.vs2
            if (thisVs2 eq thatVs2)
                thisVs2
            else {
                val newVs = thisVs2.merge(thatVs2, onDiff)
                if (newVs ne thisVs2) useThis = false
                if (newVs ne thatVs2) useThat = false
                newVs
            }
        }
        if (useThat)
            that
        else if (useThis)
            this
        else
            new Locals5(newVs1, newVs2)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals5[X] = {
        new Locals5[X](vs1.map(f), vs2.map(f))
    }

    override def transform(f: T ⇒ T): Locals5[T] = {
        val thisVs1 = vs1
        val newVs1 = thisVs1.transform(f)
        val thisVs2 = vs2
        val newVs2 = thisVs2.transform(f)
        if ((newVs1 eq thisVs1) && (newVs2 eq thisVs2))
            this
        else
            new Locals5(newVs1, newVs2)
    }

    override def foreach(f: T ⇒ Unit): Unit = { vs1.foreach(f); vs2.foreach(f) }
}

private[mutable] final class Locals6[T >: Null <: AnyRef](
        final val vs1: Locals3[T] = new Locals3[T],
        final val vs2: Locals3[T] = new Locals3[T]) extends LocalsX[T] {

    final def size = 6

    override def apply(index: Int): T = {
        if (index < 3) vs1(index) else vs2(index - 3)
    }

    override def set(index: Int, newValue: T): Unit = {
        if (index < 3) vs1.set(index, newValue) else vs2.set(index - 3, newValue)
    }

    override def update(f: (T) ⇒ T): Unit = {
        vs1.update(f)
        vs2.update(f)
    }

    override def updated(index: Int, newValue: T): Locals6[T] = {
        if (index < 3)
            new Locals6(vs1.updated(index, newValue), vs2)
        else
            new Locals6(vs1, vs2.updated(index - 3, newValue))
    }

    override def foreach(f: T ⇒ Unit): Unit = { vs1.foreach(f); vs2.foreach(f) }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals6[T] = {
        val that = other.asInstanceOf[Locals6[T]]
        var useThis = true
        var useThat = true
        val newVs1 = {
            val thisVs1 = this.vs1
            val thatVs1 = that.vs1
            if (thisVs1 eq thatVs1)
                thisVs1
            else {
                val newV = thisVs1.merge(thatVs1, onDiff)
                if (newV ne thisVs1) useThis = false
                if (newV ne thatVs1) useThat = false
                newV
            }
        }
        val newVs2 = {
            val thisVs2 = this.vs2
            val thatVs2 = that.vs2
            if (thisVs2 eq thatVs2)
                thisVs2
            else {
                val newV = thisVs2.merge(thatVs2, onDiff)
                if (newV ne thisVs2) useThis = false
                if (newV ne thatVs2) useThat = false
                newV
            }
        }
        if (useThat)
            that
        else if (useThis)
            this
        else
            new Locals6(newVs1, newVs2)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals6[X] = {
        new Locals6[X](vs1.map(f), vs2.map(f))
    }

    override def transform(f: T ⇒ T): Locals6[T] = {
        val thisVs1 = vs1
        val newVs1 = thisVs1.transform(f)
        val thisVs2 = vs2
        val newVs2 = thisVs2.transform(f)
        if ((newVs1 eq thisVs1) && (newVs2 eq thisVs2))
            this
        else
            new Locals6(newVs1, newVs2)
    }
}

private[mutable] final class Locals7[T >: Null <: AnyRef](
        final val vs1: Locals3[T] = new Locals3[T],
        final val vs2: Locals4[T] = new Locals4[T]) extends LocalsX[T] {

    final def size = 7

    override def apply(index: Int): T = {
        if (index < 3) vs1(index) else vs2(index - 3)
    }

    override def set(index: Int, newValue: T): Unit = {
        if (index < 3) vs1.set(index, newValue) else vs2.set(index - 3, newValue)
    }

    override def update(f: (T) ⇒ T): Unit = {
        vs1.update(f)
        vs2.update(f)
    }

    override def updated(index: Int, newValue: T): Locals7[T] = {
        if (index < 3)
            new Locals7(vs1.updated(index, newValue), vs2)
        else
            new Locals7(vs1, vs2.updated(index - 3, newValue))
    }

    override def foreach(f: T ⇒ Unit): Unit = { vs1.foreach(f); vs2.foreach(f) }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals7[T] = {
        val that = other.asInstanceOf[Locals7[T]]
        var useThis = true
        var useThat = true
        val newVs1 = {
            val thisVs1 = this.vs1
            val thatVs1 = that.vs1
            if (thisVs1 eq thatVs1)
                thisVs1
            else {
                val newV = thisVs1.merge(thatVs1, onDiff)
                if (newV ne thisVs1) useThis = false
                if (newV ne thatVs1) useThat = false
                newV
            }
        }
        val newVs2 = {
            val thisVs2 = this.vs2
            val thatVs2 = that.vs2
            if (thisVs2 eq thatVs2)
                thisVs2
            else {
                val newV = thisVs2.merge(thatVs2, onDiff)
                if (newV ne thisVs2) useThis = false
                if (newV ne thatVs2) useThat = false
                newV
            }
        }
        if (useThat)
            that
        else if (useThis)
            this
        else
            new Locals7(newVs1, newVs2)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals7[X] = {
        new Locals7[X](vs1.map(f), vs2.map(f))
    }

    override def transform(f: T ⇒ T): Locals7[T] = {
        val thisVs1 = vs1
        val newVs1 = thisVs1.transform(f)
        val thisVs2 = vs2
        val newVs2 = thisVs2.transform(f)
        if ((newVs1 eq thisVs1) && (newVs2 eq thisVs2))
            this
        else
            new Locals7(newVs1, newVs2)
    }
}

private[mutable] final class Locals8[T >: Null <: AnyRef](
        final val vs1: Locals2[T] = new Locals2[T],
        final val vs2: Locals3[T] = new Locals3[T],
        final val vs3: Locals3[T] = new Locals3[T]) extends LocalsX[T] {

    final def size = 8

    override def apply(index: Int): T = {
        (index: @scala.annotation.switch) match {
            case 0 | 1     ⇒ vs1(index)
            case 2 | 3 | 4 ⇒ vs2(index - 2)
            case 5 | 6 | 7 ⇒ vs3(index - 5)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def set(index: Int, newValue: T): Unit = {
        (index: @scala.annotation.switch) match {
            case 0 | 1     ⇒ vs1.set(index, newValue)
            case 2 | 3 | 4 ⇒ vs2.set(index - 2, newValue)
            case 5 | 6 | 7 ⇒ vs3.set(index - 5, newValue)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def update(f: (T) ⇒ T): Unit = {
        vs1.update(f)
        vs2.update(f)
        vs3.update(f)
    }

    override def updated(index: Int, newValue: T): Locals8[T] = {
        (index: @scala.annotation.switch) match {
            case 0 | 1     ⇒ new Locals8(vs1.updated(index, newValue), vs2, vs3)
            case 2 | 3 | 4 ⇒ new Locals8(vs1, vs2.updated(index - 2, newValue), vs3)
            case 5 | 6 | 7 ⇒ new Locals8(vs1, vs2, vs3.updated(index - 5, newValue))
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def foreach(f: T ⇒ Unit): Unit = {
        vs1.foreach(f); vs2.foreach(f); vs3.foreach(f)
    }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals8[T] = {
        val that = other.asInstanceOf[Locals8[T]]
        var useThis = true
        var useThat = true
        val newVs1 = {
            val thisVs1 = this.vs1
            val thatVs1 = that.vs1
            if (thisVs1 eq thatVs1)
                thisVs1
            else {
                val newV = thisVs1.merge(thatVs1, onDiff)
                if (newV ne thisVs1) useThis = false
                if (newV ne thatVs1) useThat = false
                newV
            }
        }
        val newVs2 = {
            val thisVs2 = this.vs2
            val thatVs2 = that.vs2
            if (thisVs2 eq thatVs2)
                thisVs2
            else {
                val newV = thisVs2.merge(thatVs2, onDiff)
                if (newV ne thisVs2) useThis = false
                if (newV ne thatVs2) useThat = false
                newV
            }
        }
        val newVs3 = {
            val thisVs3 = this.vs3
            val thatVs3 = that.vs3
            if (thisVs3 eq thatVs3)
                thisVs3
            else {
                val newV = thisVs3.merge(thatVs3, onDiff)
                if (newV ne thisVs3) useThis = false
                if (newV ne thatVs3) useThat = false
                newV
            }
        }
        if (useThat)
            that
        else if (useThis)
            this
        else
            new Locals8(newVs1, newVs2, newVs3)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals8[X] = {
        new Locals8[X](vs1.map(f), vs2.map(f), vs3.map(f))
    }

    override def transform(f: T ⇒ T): Locals8[T] = {
        val thisVs1 = vs1
        val newVs1 = thisVs1.transform(f)
        val thisVs2 = vs2
        val newVs2 = thisVs2.transform(f)
        val thisVs3 = vs3
        val newVs3 = thisVs3.transform(f)
        if ((newVs1 eq thisVs1) && (newVs2 eq thisVs2) && (newVs3 eq thisVs3))
            this
        else
            new Locals8(newVs1, newVs2, newVs3)
    }
}

private[mutable] final class Locals9[T >: Null <: AnyRef](
        final val vs1: Locals3[T] = new Locals3[T],
        final val vs2: Locals3[T] = new Locals3[T],
        final val vs3: Locals3[T] = new Locals3[T]) extends LocalsX[T] {

    final def size = 9

    override def apply(index: Int): T = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 ⇒ vs1(index)
            case 3 | 4 | 5 ⇒ vs2(index - 3)
            case 6 | 7 | 8 ⇒ vs3(index - 6)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def set(index: Int, newValue: T): Unit = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 ⇒ vs1.set(index, newValue)
            case 3 | 4 | 5 ⇒ vs2.set(index - 3, newValue)
            case 6 | 7 | 8 ⇒ vs3.set(index - 6, newValue)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def update(f: (T) ⇒ T): Unit = {
        vs1.update(f)
        vs2.update(f)
        vs3.update(f)
    }

    override def updated(index: Int, newValue: T): Locals9[T] = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 ⇒ new Locals9(vs1.updated(index, newValue), vs2, vs3)
            case 3 | 4 | 5 ⇒ new Locals9(vs1, vs2.updated(index - 3, newValue), vs3)
            case 6 | 7 | 8 ⇒ new Locals9(vs1, vs2, vs3.updated(index - 6, newValue))
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def foreach(f: T ⇒ Unit): Unit = {
        vs1.foreach(f); vs2.foreach(f); vs3.foreach(f)
    }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals9[T] = {
        val that = other.asInstanceOf[Locals9[T]]
        var useThis = true
        var useThat = true
        val newVs1 = {
            val thisVs1 = this.vs1
            val thatVs1 = that.vs1
            if (thisVs1 eq thatVs1)
                thisVs1
            else {
                val newV = thisVs1.merge(thatVs1, onDiff)
                if (newV ne thisVs1) useThis = false
                if (newV ne thatVs1) useThat = false
                newV
            }
        }
        val newVs2 = {
            val thisVs2 = this.vs2
            val thatVs2 = that.vs2
            if (thisVs2 eq thatVs2)
                thisVs2
            else {
                val newV = thisVs2.merge(thatVs2, onDiff)
                if (newV ne thisVs2) useThis = false
                if (newV ne thatVs2) useThat = false
                newV
            }
        }
        val newVs3 = {
            val thisVs3 = this.vs3
            val thatVs3 = that.vs3
            if (thisVs3 eq thatVs3)
                thisVs3
            else {
                val newV = thisVs3.merge(thatVs3, onDiff)
                if (newV ne thisVs3) useThis = false
                if (newV ne thatVs3) useThat = false
                newV
            }
        }
        if (useThat)
            that
        else if (useThis)
            this
        else
            new Locals9(newVs1, newVs2, newVs3)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals9[X] = {
        new Locals9[X](vs1.map(f), vs2.map(f), vs3.map(f))
    }

    override def transform(f: T ⇒ T): Locals9[T] = {
        val thisVs1 = vs1
        val newVs1 = thisVs1.transform(f)
        val thisVs2 = vs2
        val newVs2 = thisVs2.transform(f)
        val thisVs3 = vs3
        val newVs3 = thisVs3.transform(f)
        if ((newVs1 eq thisVs1) && (newVs2 eq thisVs2) && (newVs3 eq thisVs3))
            this
        else
            new Locals9(newVs1, newVs2, newVs3)
    }
}

private[mutable] final class Locals10[T >: Null <: AnyRef](
        final val vs1: Locals4[T] = new Locals4[T],
        final val vs2: Locals3[T] = new Locals3[T],
        final val vs3: Locals3[T] = new Locals3[T]) extends LocalsX[T] {

    final def size = 10

    override def apply(index: Int): T = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 | 3 ⇒ vs1(index)
            case 4 | 5 | 6     ⇒ vs2(index - 4)
            case 7 | 8 | 9     ⇒ vs3(index - 7)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }
    override def set(index: Int, newValue: T): Unit = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 | 3 ⇒ vs1.set(index, newValue)
            case 4 | 5 | 6     ⇒ vs2.set(index - 4, newValue)
            case 7 | 8 | 9     ⇒ vs3.set(index - 7, newValue)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def update(f: (T) ⇒ T): Unit = {
        vs1.update(f)
        vs2.update(f)
        vs3.update(f)
    }

    override def updated(index: Int, newValue: T): Locals10[T] = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 | 3 ⇒ new Locals10(vs1.updated(index, newValue), vs2, vs3)
            case 4 | 5 | 6     ⇒ new Locals10(vs1, vs2.updated(index - 4, newValue), vs3)
            case 7 | 8 | 9     ⇒ new Locals10(vs1, vs2, vs3.updated(index - 7, newValue))
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def foreach(f: T ⇒ Unit): Unit = {
        vs1.foreach(f); vs2.foreach(f); vs3.foreach(f)
    }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals10[T] = {
        val that = other.asInstanceOf[Locals10[T]]
        var useThis = true
        var useThat = true
        val newVs1 = {
            val thisVs1 = this.vs1
            val thatVs1 = that.vs1
            if (thisVs1 eq thatVs1)
                thisVs1
            else {
                val newV = thisVs1.merge(thatVs1, onDiff)
                if (newV ne thisVs1) useThis = false
                if (newV ne thatVs1) useThat = false
                newV
            }
        }
        val newVs2 = {
            val thisVs2 = this.vs2
            val thatVs2 = that.vs2
            if (thisVs2 eq thatVs2)
                thisVs2
            else {
                val newV = thisVs2.merge(thatVs2, onDiff)
                if (newV ne thisVs2) useThis = false
                if (newV ne thatVs2) useThat = false
                newV
            }
        }
        val newVs3 = {
            val thisVs3 = this.vs3
            val thatVs3 = that.vs3
            if (thisVs3 eq thatVs3)
                thisVs3
            else {
                val newV = thisVs3.merge(thatVs3, onDiff)
                if (newV ne thisVs3) useThis = false
                if (newV ne thatVs3) useThat = false
                newV
            }
        }
        if (useThat)
            that
        else if (useThis)
            this
        else
            new Locals10(newVs1, newVs2, newVs3)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals10[X] = {
        new Locals10[X](vs1.map(f), vs2.map(f), vs3.map(f))
    }

    override def transform(f: T ⇒ T): Locals10[T] = {
        val thisVs1 = vs1
        val newVs1 = thisVs1.transform(f)
        val thisVs2 = vs2
        val newVs2 = thisVs2.transform(f)
        val thisVs3 = vs3
        val newVs3 = thisVs3.transform(f)
        if ((newVs1 eq thisVs1) && (newVs2 eq thisVs2) && (newVs3 eq thisVs3))
            this
        else
            new Locals10(newVs1, newVs2, newVs3)
    }
}

private[mutable] final class Locals11[T >: Null <: AnyRef](
        final val vs1: Locals4[T] = new Locals4[T],
        final val vs2: Locals3[T] = new Locals3[T],
        final val vs3: Locals4[T] = new Locals4[T]) extends LocalsX[T] {

    final def size = 11

    override def apply(index: Int): T = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 | 3  ⇒ vs1(index)
            case 4 | 5 | 6      ⇒ vs2(index - 4)
            case 7 | 8 | 9 | 10 ⇒ vs3(index - 7)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }
    override def set(index: Int, newValue: T): Unit = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 | 3  ⇒ vs1.set(index, newValue)
            case 4 | 5 | 6      ⇒ vs2.set(index - 4, newValue)
            case 7 | 8 | 9 | 10 ⇒ vs3.set(index - 7, newValue)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def update(f: (T) ⇒ T): Unit = {
        vs1.update(f)
        vs2.update(f)
        vs3.update(f)
    }

    override def updated(index: Int, newValue: T): Locals11[T] = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 | 3  ⇒ new Locals11(vs1.updated(index, newValue), vs2, vs3)
            case 4 | 5 | 6      ⇒ new Locals11(vs1, vs2.updated(index - 4, newValue), vs3)
            case 7 | 8 | 9 | 10 ⇒ new Locals11(vs1, vs2, vs3.updated(index - 7, newValue))
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def foreach(f: T ⇒ Unit): Unit = {
        vs1.foreach(f); vs2.foreach(f); vs3.foreach(f)
    }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals11[T] = {
        val that = other.asInstanceOf[Locals11[T]]
        var useThis = true
        var useThat = true
        val newVs1 = {
            val thisVs1 = this.vs1
            val thatVs1 = that.vs1
            if (thisVs1 eq thatVs1)
                thisVs1
            else {
                val newV = thisVs1.merge(thatVs1, onDiff)
                if (newV ne thisVs1) useThis = false
                if (newV ne thatVs1) useThat = false
                newV
            }
        }
        val newVs2 = {
            val thisVs2 = this.vs2
            val thatVs2 = that.vs2
            if (thisVs2 eq thatVs2)
                thisVs2
            else {
                val newV = thisVs2.merge(thatVs2, onDiff)
                if (newV ne thisVs2) useThis = false
                if (newV ne thatVs2) useThat = false
                newV
            }
        }
        val newVs3 = {
            val thisVs3 = this.vs3
            val thatVs3 = that.vs3
            if (thisVs3 eq thatVs3)
                thisVs3
            else {
                val newV = thisVs3.merge(thatVs3, onDiff)
                if (newV ne thisVs3) useThis = false
                if (newV ne thatVs3) useThat = false
                newV
            }
        }
        if (useThat)
            that
        else if (useThis)
            this
        else
            new Locals11(newVs1, newVs2, newVs3)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals11[X] = {
        new Locals11[X](vs1.map(f), vs2.map(f), vs3.map(f))
    }

    override def transform(f: T ⇒ T): Locals11[T] = {
        val thisVs1 = vs1
        val newVs1 = thisVs1.transform(f)
        val thisVs2 = vs2
        val newVs2 = thisVs2.transform(f)
        val thisVs3 = vs3
        val newVs3 = thisVs3.transform(f)
        if ((newVs1 eq thisVs1) && (newVs2 eq thisVs2) && (newVs3 eq thisVs3))
            this
        else
            new Locals11(newVs1, newVs2, newVs3)
    }
}

private[mutable] final class Locals12_N[T >: Null <: AnyRef: ClassTag](
        final val vs11: Locals11[T],
        final val vs12_N: Array[T]) extends LocalsX[T] {

    def this(size: Int) {
        this(
            new Locals11[T],
            new Array[T](size - 11))
    }

    final def size = vs12_N.length + 11

    override def apply(index: Int): T =
        if (index < 11)
            vs11(index)
        else
            vs12_N(index - 11)

    override def set(index: Int, newValue: T): Unit = {
        if (index < 11) {
            vs11.set(index, newValue)
        } else {
            vs12_N(index - 11) = newValue
        }
    }

    override def update(f: (T) ⇒ T): Unit = {
        vs11.update(f)
        vs12_N.transform(f)
    }

    override def updated(index: Int, newValue: T): Locals12_N[T] = {
        if (index < 11) {
            new Locals12_N(vs11.updated(index, newValue), vs12_N)
        } else {
            val newVs12_N = new Array(vs12_N.length)
            System.arraycopy(vs12_N, 0, newVs12_N, 0, vs12_N.length)
            newVs12_N(index - 11) = newValue
            new Locals12_N(vs11, newVs12_N)
        }
    }

    override def foreach(f: T ⇒ Unit): Unit = {
        vs11.foreach(f)
        vs12_N.foreach(f)
    }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals12_N[T] = {
        val that = other.asInstanceOf[Locals12_N[T]]
        var useThis = true
        var useThat = true
        val thisVs11 = this.vs11
        val thatVs11 = that.vs11
        val newVs11 =
            if (thisVs11 eq thatVs11)
                thisVs11
            else {
                val newVs = thisVs11.merge(thatVs11, onDiff)
                if (newVs ne thisVs11) useThis = false
                if (newVs ne thatVs11) useThat = false
                newVs
            }

        val thisVs12_N = this.vs12_N
        val thatVs12_N = that.vs12_N
        val newVs12_N =
            if (thisVs12_N eq thatVs12_N)
                thisVs12_N
            else {
                val newVs12_N = new Array(vs12_N.length)
                var useThisArray = true
                var useThatArray = true
                var i = vs12_N.length - 1
                while (i >= 0) {
                    val thisAtI = thisVs12_N(i)
                    val thatAtI = thatVs12_N(i)
                    if (thisAtI eq thatAtI)
                        newVs12_N(i) = thisAtI
                    else {
                        val newV = onDiff(thisAtI, thatAtI)
                        if (newV ne thisAtI) useThisArray = false
                        if (newV ne thatAtI) useThatArray = false
                        newVs12_N(i) = newV
                    }
                    i -= 1
                }

                if (useThisArray) {
                    if (!useThatArray) useThat = false
                    thisVs12_N
                } else if (useThatArray) {
                    useThis = false
                    thatVs12_N
                } else {
                    useThis = false
                    useThat = false
                    newVs12_N
                }
            }

        if (useThat)
            that
        else if (useThis)
            this
        else
            new Locals12_N(newVs11, newVs12_N)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals12_N[X] = {
        new Locals12_N[X](vs11.map(f), vs12_N.map(f))
    }

    override def transform(f: T ⇒ T): Locals12_N[T] = {
        val thisVs11 = vs11
        val newVs11 = thisVs11.transform(f)
        val thisVs12_N = vs12_N
        var vs12_Nupdated = false
        var newVs12_N = thisVs12_N map { v ⇒
            val newV = f(v); if (newV ne v) vs12_Nupdated = true; newV
        }
        if (!vs12_Nupdated)
            newVs12_N = thisVs12_N

        if ((newVs11 eq thisVs11) && (newVs12_N eq thisVs12_N))
            this
        else
            new Locals12_N(newVs11, newVs12_N)
    }
}

object Locals {

    def empty[T >: Null <: AnyRef: ClassTag]: Locals[T] = Locals0.asInstanceOf[Locals[T]]

    def apply[T >: Null <: AnyRef: ClassTag](data: IndexedSeq[T]): Locals[T] = {
        (data.size: @scala.annotation.switch) match {
            case 0 ⇒ Locals0.asInstanceOf[Locals[T]]
            case 1 ⇒ new Locals1(data(0))
            case 2 ⇒ new Locals2(data(0), data(1))
            case 3 ⇒ new Locals3(data(0), data(1), data(2))
            case 4 ⇒ new Locals4(data(0), data(1), data(2), data(3))
            case 5 ⇒
                new Locals5(
                    new Locals2(data(0), data(1)),
                    new Locals3(data(2), data(3), data(4)))
            case 6 ⇒
                new Locals6(
                    new Locals3(data(0), data(1), data(2)),
                    new Locals3(data(3), data(4), data(5)))
            case 7 ⇒
                new Locals7(
                    new Locals3(data(0), data(1), data(2)),
                    new Locals4(data(3), data(4), data(5), data(6)))
            //                new Locals7(
            //                    new Locals2(data(0), data(1)),
            //                    new Locals2(data(2), data(3)),
            //                    new Locals3(data(4), data(5), data(6)))
            case 8 ⇒
                new Locals8(
                    new Locals2(data(0), data(1)),
                    new Locals3(data(2), data(3), data(4)),
                    new Locals3(data(5), data(6), data(7)))
            case 9 ⇒
                new Locals9(
                    new Locals3(data(0), data(1), data(2)),
                    new Locals3(data(3), data(4), data(5)),
                    new Locals3(data(6), data(7), data(8)))
            case 10 ⇒
                new Locals10(
                    new Locals4(data(0), data(1), data(2), data(3)),
                    new Locals3(data(4), data(5), data(6)),
                    new Locals3(data(7), data(8), data(9)))
            case 11 ⇒
                new Locals11(
                    new Locals4(data(0), data(1), data(2), data(3)),
                    new Locals3(data(4), data(5), data(6)),
                    new Locals4(data(7), data(8), data(9), data(10)))
            case x ⇒
                if (x > 11)
                    new Locals12_N[T](
                        new Locals11(
                            new Locals4(data(0), data(1), data(2), data(3)),
                            new Locals3(data(4), data(5), data(6)),
                            new Locals4(data(7), data(8), data(9), data(10))),
                        data.drop(11).toArray
                    )
                else
                    throw new IllegalArgumentException("size has to be >= 0")
        }
    }

    def apply[T >: Null <: AnyRef: ClassTag](size: Int): Locals[T] = {
        (size: @scala.annotation.switch) match {
            case 0  ⇒ Locals0.asInstanceOf[Locals[T]]
            case 1  ⇒ new Locals1[T]()
            case 2  ⇒ new Locals2[T]()
            case 3  ⇒ new Locals3[T]()
            case 4  ⇒ new Locals4[T]()
            case 5  ⇒ new Locals5[T]()
            case 6  ⇒ new Locals6[T]()
            case 7  ⇒ new Locals7[T]()
            case 8  ⇒ new Locals8[T]()
            case 9  ⇒ new Locals9[T]()
            case 10 ⇒ new Locals10[T]()
            case 11 ⇒ new Locals11[T]()
            case x ⇒
                if (x > 11)
                    new Locals12_N[T](x)
                else
                    throw new IllegalArgumentException("size has to be >= 0")
        }
    }
}

