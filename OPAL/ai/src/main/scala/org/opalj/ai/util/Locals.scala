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
package util

import scala.annotation.tailrec
import scala.reflect.ClassTag

/**
 * An immutable vector that is heavily optimized for small(er) collections (up to 10
 * elements) that are frequently compared and updated and where sharing is beneficial.
 *
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

    /* ABSTRACT */ def updated(index: Int, value: T): Locals[T]

    /* ABSTRACT */ def foreach(f: T ⇒ Unit): Unit

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
     */
    def mapToVector[X](f: T ⇒ X): scala.collection.immutable.Vector[X] = {
        var newLocals = scala.collection.immutable.Vector.empty[X]
        foreach { e ⇒ newLocals :+ f(e) }
        newLocals
    }

    def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals[X]

    /**
     * Performs a fold left over all elements of this set.
     */
    def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = {
        var result: B = b
        foreach { elem ⇒ result = op(result, elem) }
        result
    }

    /**
     * Converts this set into a sequence. The elements are sorted in ascending order
     * using the unique ids of the elements.
     */
    def toSeq: Seq[T] = {
        var seq = List.empty[T]
        foreach { e ⇒ seq = e :: seq }
        seq
    }

    def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals[T]

    /*ABSTRACT*/ override def equals(other: Any): Boolean

    override def hashCode: Int = {
        var hc = 1
        foreach { e ⇒ hc * 41 + e.hashCode }
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

    override def toString: String = mkString("org.opalj.collection.immutable.Locals(", ",", ")")

}

private object Locals0 extends Locals[Null] {

    final override val size = 0

    final override val isEmpty = true

    final override val nonEmpty = false

    override def apply(index: Int): Nothing =
        throw new IndexOutOfBoundsException("the vector has size 0")

    override def updated(index: Int, newValue: Null): Locals0.type =
        throw new IndexOutOfBoundsException("the vector has size 0")

    def merge(other: Locals[Null], onDiff: (Null, Null) ⇒ Null): this.type =
        if (this eq other)
            this
        else
            // thrown to make the exception homogenous
            throw new ClassCastException("other vector cannot be cast to an empty vector")

    override def foreach(f: Null ⇒ Unit): Unit = { /*nothing to do*/ }

    override def map[X >: Null <: AnyRef: ClassTag](f: Null ⇒ X): Locals[X] = {
        this.asInstanceOf[Locals[X]]
    }

    override def equals(other: Any): Boolean = {
        other match {
            case Locals0 ⇒ true
            case _       ⇒ false
        }
    }
}

private sealed abstract class LocalsX[T >: Null <: AnyRef] extends Locals[T] {

    final override def isEmpty = false

    final override def nonEmpty = true

    override def equals(other: Any): Boolean = {
        println("equals called")
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

private class Locals1[T >: Null <: AnyRef]( final val v: T) extends LocalsX[T] {

    final override def size = 1

    override def apply(index: Int): T = {
        if (index != 0)
            throw new IndexOutOfBoundsException("invalid index("+index+")")
        v
    }

    override def updated(index: Int, newValue: T): Locals1[T] = {
        if (index != 0)
            throw new IndexOutOfBoundsException("invalid index("+index+")")

        new Locals1(newValue)
    }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals1[T] = {
        if (other eq this)
            return this

        val that = other.asInstanceOf[Locals1[T]]

        val thisV = this.v
        val thatV = that.v
        if (thisV eq thatV)
            this
        else {
            val newV = onDiff(thisV, thatV)
            if (newV eq thisV)
                this
            else if (newV eq thatV)
                that
            else
                new Locals1(newV)
        }
    }

    override def foreach(f: T ⇒ Unit): Unit = { f(v) }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals1[X] = {
        new Locals1[X](f(v))
    }
}

private class Locals2[T >: Null <: AnyRef](
        final val v0: T,
        final val v1: T) extends LocalsX[T] {

    final override def size = 2

    override def apply(index: Int): T = {
        (index: @scala.annotation.switch) match {
            case 0 ⇒ v0
            case 1 ⇒ v1
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
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
        if (useThis)
            this
        else if (useThat)
            that
        else
            new Locals2(newV0, newV1)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals2[X] = {
        new Locals2[X](f(v0), f(v1))
    }

    override def foreach(f: T ⇒ Unit): Unit = { f(v0); f(v1) }
}

private class Locals3[T >: Null <: AnyRef](
        final val v0: T,
        final val v1: T,
        final val v2: T) extends LocalsX[T] {

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

        if (useThis)
            this
        else if (useThat)
            that
        else
            new Locals3(newV0, newV1, newV2)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals3[X] = {
        new Locals3[X](f(v0), f(v1), f(v2))
    }

    override def foreach(f: T ⇒ Unit): Unit = { f(v0); f(v1); f(v2) }
}

private class Locals4[T >: Null <: AnyRef](
        final val v0: T,
        final val v1: T,
        final val v2: T,
        final val v3: T) extends LocalsX[T] {

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

        if (useThis)
            this
        else if (useThat)
            that
        else
            new Locals4(newV0, newV1, newV2, newV3)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals4[X] = {
        new Locals4[X](f(v0), f(v1), f(v2), f(v3))
    }

    final override def foreach(f: T ⇒ Unit): Unit = { f(v0); f(v1); f(v2); f(v3) }
}

private trait NullLocals extends Locals[AnyRef] {

    require(size > 0)

    final override def apply(index: Int): Null = null

}

private object NullLocals1 extends Locals1(null)
private object NullLocals2 extends Locals2(null, null)
private object NullLocals3 extends Locals3(null, null, null)
private object NullLocals4 extends Locals4(null, null, null, null)

private class Locals5[T >: Null <: AnyRef](
        final val vs1: Locals2[T],
        final val vs2: Locals3[T]) extends LocalsX[T] {

    def this() {
        this(NullLocals2.asInstanceOf[Locals2[T]], NullLocals3.asInstanceOf[Locals3[T]])
    }

    final def size = 5

    override def apply(index: Int): T = {
        (index: @scala.annotation.switch) match {
            case 0 | 1     ⇒ vs1(index)
            case 2 | 3 | 4 ⇒ vs2(index - 2)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def updated(index: Int, newValue: T): Locals[T] = {
        (index: @scala.annotation.switch) match {
            case 0 | 1     ⇒ new Locals5(vs1.updated(index, newValue), vs2)
            case 2 | 3 | 4 ⇒ new Locals5(vs1, vs2.updated(index - 2, newValue))
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
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
        if (useThis)
            this
        else if (useThat)
            that
        else
            new Locals5(newVs1, newVs2)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals5[X] = {
        new Locals5[X](vs1.map(f), vs2.map(f))
    }

    override def foreach(f: T ⇒ Unit): Unit = { vs1.foreach(f); vs2.foreach(f) }
}

private class Locals6[T >: Null <: AnyRef](
        final val vs1: Locals3[T],
        final val vs2: Locals3[T]) extends LocalsX[T] {

    def this() {
        this(NullLocals3.asInstanceOf[Locals3[T]], NullLocals3.asInstanceOf[Locals3[T]])
    }

    final def size = 6

    override def apply(index: Int): T = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 ⇒ vs1(index)
            case 3 | 4 | 5 ⇒ vs2(index - 3)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def updated(index: Int, newValue: T): Locals6[T] = {
        (index: @scala.annotation.switch) match {
            case 0 | 1 | 2 ⇒ new Locals6(vs1.updated(index, newValue), vs2)
            case 3 | 4 | 5 ⇒ new Locals6(vs1, vs2.updated(index - 3, newValue))
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
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
        if (useThis)
            this
        else if (useThat)
            that
        else
            new Locals6(newVs1, newVs2)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals6[X] = {
        new Locals6[X](vs1.map(f), vs2.map(f))
    }
}

private class Locals7[T >: Null <: AnyRef](
        final val vs1: Locals2[T],
        final val vs2: Locals2[T],
        final val vs3: Locals3[T]) extends LocalsX[T] {

    def this() {
        this(NullLocals2.asInstanceOf[Locals2[T]],
            NullLocals2.asInstanceOf[Locals2[T]],
            NullLocals3.asInstanceOf[Locals3[T]])
    }

    final def size = 7

    override def apply(index: Int): T = {
        (index: @scala.annotation.switch) match {
            case 0 | 1     ⇒ vs1(index)
            case 2 | 3     ⇒ vs2(index - 2)
            case 4 | 5 | 6 ⇒ vs3(index - 4)
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def updated(index: Int, newValue: T): Locals[T] = {
        (index: @scala.annotation.switch) match {
            case 0 | 1     ⇒ new Locals7(vs1.updated(index, newValue), vs2, vs3)
            case 2 | 3     ⇒ new Locals7(vs1, vs2.updated(index - 2, newValue), vs3)
            case 4 | 5 | 6 ⇒ new Locals7(vs1, vs2, vs3.updated(index - 4, newValue))
            case _ ⇒
                throw new IndexOutOfBoundsException("invalid index("+index+")")
        }
    }

    override def foreach(f: T ⇒ Unit): Unit = {
        vs1.foreach(f); vs2.foreach(f); vs3.foreach(f)
    }

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
        if (useThis)
            this
        else if (useThat)
            that
        else
            new Locals7(newVs1, newVs2, newVs3)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals7[X] = {
        new Locals7[X](vs1.map(f), vs2.map(f), vs3.map(f))
    }
}

private class Locals8[T >: Null <: AnyRef](
        final val vs1: Locals2[T],
        final val vs2: Locals3[T],
        final val vs3: Locals3[T]) extends LocalsX[T] {

    def this() {
        this(NullLocals2.asInstanceOf[Locals2[T]],
            NullLocals3.asInstanceOf[Locals3[T]],
            NullLocals3.asInstanceOf[Locals3[T]])
    }

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
        if (useThis)
            this
        else if (useThat)
            that
        else
            new Locals8(newVs1, newVs2, newVs3)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals8[X] = {
        new Locals8[X](vs1.map(f), vs2.map(f), vs3.map(f))
    }
}

private class Locals9[T >: Null <: AnyRef](
        final val vs1: Locals3[T],
        final val vs2: Locals3[T],
        final val vs3: Locals3[T]) extends LocalsX[T] {

    def this() {
        this(NullLocals3.asInstanceOf[Locals3[T]],
            NullLocals3.asInstanceOf[Locals3[T]],
            NullLocals3.asInstanceOf[Locals3[T]])
    }

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
        if (useThis)
            this
        else if (useThat)
            that
        else
            new Locals9(newVs1, newVs2, newVs3)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals9[X] = {
        new Locals9[X](vs1.map(f), vs2.map(f), vs3.map(f))
    }
}

private class Locals10[T >: Null <: AnyRef](
        final val vs1: Locals4[T],
        final val vs2: Locals3[T],
        final val vs3: Locals3[T]) extends LocalsX[T] {

    def this() {
        this(NullLocals4.asInstanceOf[Locals4[T]],
            NullLocals3.asInstanceOf[Locals3[T]],
            NullLocals3.asInstanceOf[Locals3[T]])
    }

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
        if (useThis)
            this
        else if (useThat)
            that
        else
            new Locals10(newVs1, newVs2, newVs3)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals10[X] = {
        new Locals10[X](vs1.map(f), vs2.map(f), vs3.map(f))
    }
}

import scala.reflect.ClassTag

private class Locals11_N[T >: Null <: AnyRef: ClassTag](
        final val vs10: Locals10[T],
        final val vs11_N: Array[T]) extends LocalsX[T] {

    def this(size: Int) {
        this(new Locals10(), new Array[T](size - 10))
    }

    final def size = vs11_N.length + 10

    override def apply(index: Int): T =
        if (index < 10)
            vs10(index)
        else
            vs11_N(index - 10)

    override def updated(index: Int, newValue: T): Locals11_N[T] = {
        if (index < 10) {
            new Locals11_N(vs10.updated(index, newValue), vs11_N)
        } else {
            val newVs11_31 = new Array(vs11_N.length)
            System.arraycopy(vs11_N, 0, newVs11_31, 0, vs11_N.length)
            newVs11_31(index - 10) = newValue
            new Locals11_N(vs10, newVs11_31)
        }
    }

    override def foreach(f: T ⇒ Unit): Unit = {
        vs10.foreach(f)
        vs11_N.foreach(f)
    }

    override def merge(other: Locals[T], onDiff: (T, T) ⇒ T): Locals11_N[T] = {
        val that = other.asInstanceOf[Locals11_N[T]]
        var useThis = true
        var useThat = true
        val thisVs10 = this.vs10
        val thatVs10 = that.vs10
        val newVs10 =
            if (thisVs10 eq thatVs10)
                thisVs10
            else {
                val newVs = thisVs10.merge(thatVs10, onDiff)
                if (newVs ne thisVs10) useThis = false
                if (newVs ne thatVs10) useThat = false
                newVs
            }

        val thisVs11_N = this.vs11_N
        val thatVs11_N = that.vs11_N
        val newVs11_N =
            if (thisVs11_N eq thatVs11_N)
                thisVs11_N
            else {
                val newVs11_N = new Array(vs11_N.length)
                var useThisArray = true
                var useThatArray = true
                var i = vs11_N.length - 1
                while (i >= 0) {
                    val thisAtI = thisVs11_N(i)
                    val thatAtI = thatVs11_N(i)
                    if (thisAtI eq thatAtI)
                        newVs11_N(i) = thisAtI
                    else {
                        val newV = onDiff(thisAtI, thatAtI)
                        if (newV ne thisAtI) useThisArray = false
                        if (newV ne thatAtI) useThatArray = false
                        newVs11_N(i) = newV
                    }
                    i -= 1
                }

                if (useThisArray) {
                    if (!useThatArray) useThat = false
                    thisVs11_N
                } else if (useThatArray) {
                    useThis = false
                    thatVs11_N
                } else {
                    useThis = false
                    useThat = false
                    newVs11_N
                }
            }

        if (useThis)
            this
        else if (useThat)
            that
        else
            new Locals11_N(newVs10, newVs11_N)
    }

    override def map[X >: Null <: AnyRef: ClassTag](f: T ⇒ X): Locals11_N[X] = {
        new Locals11_N[X](vs10.map(f), vs11_N.map(f))
    }
}

private object NullLocals5 extends Locals5[Null](NullLocals2, NullLocals3)
private object NullLocals6 extends Locals6[Null](NullLocals3, NullLocals3)
private object NullLocals7 extends Locals7[Null](NullLocals2, NullLocals2, NullLocals3)
private object NullLocals8 extends Locals8[Null](NullLocals2, NullLocals3, NullLocals3)
private object NullLocals9 extends Locals9[Null](NullLocals3, NullLocals3, NullLocals3)
private object NullLocals10 extends Locals10[Null](NullLocals4, NullLocals3, NullLocals3)

object Locals {

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
                    new Locals2(data(0), data(1)),
                    new Locals2(data(2), data(3)),
                    new Locals3(data(4), data(5), data(6)))
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
            case x ⇒
                if (x > 10)
                    new Locals11_N[T](
                        new Locals10(
                            new Locals4(data(0), data(1), data(2), data(3)),
                            new Locals3(data(4), data(5), data(6)),
                            new Locals3(data(7), data(8), data(9))),
                        data.drop(10).toArray
                    )
                else
                    throw new IllegalArgumentException("the size has to be larger than zero")
        }
    }

    def apply[T >: Null <: AnyRef: ClassTag](size: Int): Locals[T] = {
        (size: @scala.annotation.switch) match {
            case 0  ⇒ Locals0.asInstanceOf[Locals[T]]
            case 1  ⇒ NullLocals1.asInstanceOf[Locals[T]]
            case 2  ⇒ NullLocals2.asInstanceOf[Locals[T]]
            case 3  ⇒ NullLocals3.asInstanceOf[Locals[T]]
            case 4  ⇒ NullLocals4.asInstanceOf[Locals[T]]
            case 5  ⇒ NullLocals5.asInstanceOf[Locals[T]]
            case 6  ⇒ NullLocals6.asInstanceOf[Locals[T]]
            case 7  ⇒ NullLocals7.asInstanceOf[Locals[T]]
            case 8  ⇒ NullLocals8.asInstanceOf[Locals[T]]
            case 9  ⇒ NullLocals9.asInstanceOf[Locals[T]]
            case 10 ⇒ NullLocals10.asInstanceOf[Locals[T]]
            case x ⇒
                if (x > 10)
                    new Locals11_N[T](x)
                else
                    throw new IllegalArgumentException("the size has to be larger than zero")
        }
    }
}

