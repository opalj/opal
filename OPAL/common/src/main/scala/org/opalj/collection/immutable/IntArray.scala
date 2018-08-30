/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import java.util.{Arrays ⇒ JArrays}

import scala.collection.AbstractIterator
import scala.collection.mutable.Builder

/**
 * Wraps an array such that the underlying array is no longer directly accessible and
 * therefore also no longer mutable if `IntArray` is the sole owner.
 *
 * @author Michael Eichberg
 */
class IntArray private (
        private val data: Array[Int]
) extends scala.collection.immutable.Seq[Int] {

    def apply(idx: Int): Int = data(idx)

    /**
     * Directly performs the map operation on the underlying array and then creates a new
     * appropriately typed `AnyRefArray[X]` object which wraps the modified array.
     *
     * '''This method is only to be used if `this` instance is no longer used afterwards!'''
     */
    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_map(f: Int ⇒ Int): this.type = {
        var i = 0
        val max = data.length
        while (i < max) {
            data(i) = f(data(i))
            i += 1
        }
        this
    }

    /**
     * Directly updates the value at the given index and then creates a new
     * appropriately typed `AnyRefArray[X]` object which wraps the modified array.
     *
     * '''This method is only to be used if `this` instance is no longer used afterwards!'''
     */
    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_replace(index: Int, e: Int): this.type = {
        data(index) = e
        this
    }

    def map[X <: AnyRef](f: Int ⇒ X): AnyRefArray[X] = {
        val newData = new Array[AnyRef](data.length)
        var i = 0
        val max = data.length
        while (i < max) {
            newData(i) = f(data(i))
            i += 1
        }
        AnyRefArray._UNSAFE_from[X](newData)
    }

    def map(f: Int ⇒ Int): IntArray = {
        val newData = new Array[Int](data.length)
        var i = 0
        val max = data.length
        while (i < max) {
            newData(i) = f(data(i))
            i += 1
        }
        new IntArray(newData)
    }

    def flatMap[X <: AnyRef](f: Int ⇒ TraversableOnce[X]): AnyRefArray[X] = {
        val b = AnyRefArray.newBuilder[X]
        var i = 0
        val max = data.length
        b.sizeHint(max)
        while (i < max) {
            b ++= f(data(i))
            i += 1
        }
        b.result()
    }

    def flatMap(f: Int ⇒ TraversableOnce[Int]): IntArray = {
        val b = IntArray.newBuilder
        var i = 0
        val max = data.length
        b.sizeHint(max)
        while (i < max) {
            b ++= f(data(i))
            i += 1
        }
        b.result()
    }

    @inline final def sorted: IntArray = {
        val newData = data.clone
        JArrays.parallelSort(newData)
        new IntArray(newData)
    }

    /**
     * Checks if the given element is stored in the array. Performs a linear sweep of the array
     * (complexity O(N)).
     * If the array happens to be sorted, consider using `binarySearch`.
     */
    def contains(v: Int): Boolean = {
        val data = this.data
        val max = data.length
        var i = 0
        while (i < max) {
            if (data(i) == v) {
                return true;
            }
            i += 1
        }
        false
    }

    override def isEmpty: Boolean = data.length == 0
    override def nonEmpty: Boolean = data.length > 0

    override def size: Int = data.length
    override def length: Int = data.length

    override def head: Int = {
        if (nonEmpty)
            data(0)
        else
            throw new NoSuchElementException
    }

    override def foreach[U](f: Int ⇒ U): Unit = {
        val data = this.data
        val max = data.length
        var i = 0
        while (i < max) {
            f(data(i))
            i += 1
        }
    }

    /** Appends the given element. */
    def :+(elem: Int): IntArray = {
        val newData = JArrays.copyOf(data, data.length + 1)
        newData(data.length) = elem
        new IntArray(newData)
    }

    def intIterator: IntIterator = new IntIterator {
        private[this] var i = 0
        override def hasNext: Boolean = i < data.length
        override def next(): Int = { val e = data(i); i += 1; e }
    }

    /** Do consider using IntIterator to avaoid (un)boxing operations! */
    override def iterator: Iterator[Int] = new AbstractIterator[Int] {
        private[this] var i = 0
        override def hasNext: Boolean = i < data.length
        override def next(): Int = { val e = data(i); i += 1; e }
    }

    override def filter(f: Int ⇒ Boolean): IntArray = {
        val b = IntArray.newBuilder
        val data = this.data
        val max = data.length
        b.sizeHint(Math.min(8, max))
        var i = 0
        while (i < max) {
            val e = data(i)
            if (f(e)) {
                b += e
            }
            i += 1
        }
        b.result()
    }

    override def filterNot(f: Int ⇒ Boolean): IntArray = filter(e ⇒ !f(e))

    /**
     * Creates a new `IntArray` where the value at the given index is replaced by
     * the given value.
     */
    def replaced(index: Int, e: Int): IntArray = {
        val newData = java.util.Arrays.copyOf(data, data.length)
        newData(index) = e
        new IntArray(newData)
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntArray ⇒ JArrays.equals(this.data, that.data)
            case _ ⇒
                false
        }
    }

    override lazy val hashCode: Int = JArrays.hashCode(data) * 11

    override def toString: String = data.mkString("AnyRefArray(", ", ", ")")

}

/**
 * Factory for [[IntArray]]s.
 */
object IntArray {

    def newBuilder: Builder[Int, IntArray] = {
        new Builder[Int, IntArray] {
            private[this] var data: Array[Int] = null
            private[this] var i = 0
            override def +=(e: Int): this.type = {
                if (data == null) {
                    data = new Array[Int](8)
                } else if (i == data.length) {
                    data = JArrays.copyOf(data, (i + 1) * 2)
                }
                data(i) = e
                i += 1
                this
            }
            override def clear(): Unit = i = 0
            override def result(): IntArray = {
                if (data == null)
                    empty
                else
                    new IntArray(
                        if (i == data.length)
                            data
                        else
                            JArrays.copyOf(data, i)
                    )
            }
            override def sizeHint(size: Int): Unit = {
                if (data == null) data = new Array[Int](size)
            }
        }
    }

    val EmptyArrayOfInt = new Array[Int](0)
    val Empty: IntArray = new IntArray(EmptyArrayOfInt)

    def empty: IntArray = Empty

    def apply(data: Int*): IntArray = new IntArray(data.toArray[Int])

    def unapplySeq(x: IntArray): Option[Seq[Int]] = Some(x)

    /**
     * Creates a new [[IntArray]] by cloning the given array.
     *
     * I.e., modifications to the given array will not be reflected.
     */
    def apply(data: Array[Int]): IntArray = {
        new IntArray(data.clone())
    }

    def from[T](data: Traversable[T])(f: T ⇒ Int): IntArray = {
        val max = data.size
        val it = data.toIterator
        val newData = new Array[Int](max)
        var i = 0; while (i < max) { newData(i) = f(it.next()); i += 1 }
        new IntArray(newData)
    }

    /**
     * Creates a new [[AnyRefArray]] from the given array. Hence, changes to the
     * underlying array would be reflected!
     *
     * '''Only use this factory method if you have full control over all
     * aliases to the given array to ensure that the underlying array is not mutated.'''
     */
    // IMPROVE Use an ownership annotation to specify that AnyRefArray takes over the ownership of the array.
    def _UNSAFE_from(data: Array[Int]): IntArray = new IntArray(data)

}
