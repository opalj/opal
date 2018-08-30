/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import java.util.{Arrays ⇒ JArrays}

import scala.collection.mutable.Builder

import org.opalj.collection.mutable.AnyRefArrayBuffer
import org.opalj.control.{find ⇒ findInArray}

/**
 * Wraps an array such that the underlying array is no longer directly accessible and
 * therefore also no longer mutable if `AnyRefArray` is the sole owner.
 *
 * @note Compared to `ConstArray`, `AnyRefArray` does not provide an efficient
 *       `toArray` method. However, `AnyRefArray`s are covariant.
 *
 * @author Michael Eichberg
 */
class AnyRefArray[+T /* "<: AnyRef" this constraint is ONLY enforced by the factory methods to facilitate integration with the scala collection API */ ] private (
        private var data: Array[AnyRef]
) extends scala.collection.immutable.Seq[T] { // TODO [Scala 2.13] make it extend IndexedSeq.

    /**
     * Appends the given element to the underlying array; will cause havoc if this object
     * is not under full control of the caller.
     */
    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_add[X >: T <: AnyRef](elem: X): AnyRefArray[X] = {
        val newData = JArrays.copyOf(data, data.length + 1)
        newData(data.length) = elem
        data = newData
        this.asInstanceOf[AnyRefArray[X]]
    }

    def _UNSAFE_addAll[X >: T <: AnyRef](that: AnyRefArray[X]): AnyRefArray[X] = {
        val newData = JArrays.copyOf(data, this.data.length + that.data.length)
        System.arraycopy(that.data, 0, newData, this.data.length, that.data.length)
        data = newData
        this.asInstanceOf[AnyRefArray[X]]
    }

    def ++[X >: T <: AnyRef](that: AnyRefArray[X]): AnyRefArray[X] = {
        val newData = JArrays.copyOf(data, this.data.length + that.data.length)
        System.arraycopy(that.data, 0, newData, this.data.length, that.data.length)
        new AnyRefArray(newData)
    }

    /**
     * Directly performs the map operation on the underlying array and then creates a new
     * appropriately typed `AnyRefArray[X]` object which wraps the modified array.
     *
     * '''This method is only to be used if `this` instance is no longer used afterwards!'''
     */
    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_map[X <: AnyRef](f: T ⇒ X): AnyRefArray[X] = {
        var i = 0
        val max = data.length
        while (i < max) {
            data(i) = f(data(i).asInstanceOf[T])
            i += 1
        }
        new AnyRefArray[X](data)
    }

    /**
     * Directly performs the sort operation on the underlying array.
     *
     * '''This method is only to be used if `this` instance is no longer used afterwards!'''
     */
    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_sortWith(compare: (AnyRef, AnyRef) ⇒ Boolean): this.type = {
        JArrays.parallelSort[AnyRef](data, Ordering.fromLessThan(compare))
        this
    }

    /**
     * Directly updates the value at the given index and then creates a new
     * appropriately typed `AnyRefArray[X]` object which wraps the modified array.
     *
     * '''This method is only to be used if `this` instance is no longer used afterwards!'''
     */
    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_replace[X >: T <: AnyRef](index: Int, e: X): AnyRefArray[X] = {
        data(index) = e
        new AnyRefArray(data)
    }

    /**
     * Returns a new AnyRefArray where the values are sorted based on their natural ordering.
     *
     * @example
     * {{{
     *     AnyRefArray("c","a").sorted[String]
     * }}}
     */
    def _UNSAFE_sorted[X >: T](implicit ev: T <:< Comparable[X]): this.type = {
        ignore(ev) // <= HACK... should have no effect at runtime.
        JArrays.parallelSort[AnyRef](data, null)
        this
    }

    def map[X <: AnyRef](f: T ⇒ X): AnyRefArray[X] = {
        val newData = new Array[AnyRef](data.length)
        var i = 0
        val max = data.length
        while (i < max) {
            newData(i) = f(data(i).asInstanceOf[T])
            i += 1
        }
        new AnyRefArray[X](newData)
    }

    def map(f: T ⇒ Int): IntArray = {
        val newData = new Array[Int](data.length)
        var i = 0
        val max = data.length
        while (i < max) {
            newData(i) = f(data(i).asInstanceOf[T])
            i += 1
        }
        IntArray._UNSAFE_from(newData)
    }

    def flatMap[X <: AnyRef](f: T ⇒ TraversableOnce[X]): AnyRefArray[X] = {
        val b = AnyRefArray.newBuilder[X]
        var i = 0
        val max = data.length
        b.sizeHint(max)
        while (i < max) {
            b ++= f(data(i).asInstanceOf[T])
            i += 1
        }
        b.result()
    }

    def forallEquals(v: AnyRef): Boolean = {
        var i = 0
        val max = data.length
        while (i < max) {
            if (v != data(i)) return false;
            i += 1
        }
        true
    }

    def apply(idx: Int): T = data(idx).asInstanceOf[T]

    def sortWith[X >: T](compare: (X, X) ⇒ Boolean): AnyRefArray[T] = {
        val newData = data.clone
        JArrays.parallelSort(newData, Ordering.fromLessThan(compare).asInstanceOf[Ordering[AnyRef]])
        new AnyRefArray[T](newData)
    }

    /** The following method is "just" required to shut-up the compiler. */
    @inline final private[this] def ignore(x: AnyRef): AnyRef = x

    /**
     * Returns a new AnyRefArray where the values are sorted based on their natural ordering.
     *
     * @example
     * {{{
     *     AnyRefArray("c","a").sorted[String]
     * }}}
     */
    @inline final def sorted[X >: T](implicit ev: T <:< Comparable[X]): AnyRefArray[T] = {
        ignore(ev) // <= HACK... should have no effect at runtime.
        val newData = data.clone
        JArrays.parallelSort[AnyRef](newData, null)
        new AnyRefArray[T](newData)
    }

    /**
     * Checks if the given element is stored in the array. Performs a linear sweep of the array
     * (complexity O(N)).
     * If the array happens to be sorted, consider using `binarySearch`.
     */
    override def contains[X >: T](v: X): Boolean = {
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
    def length: Int = data.length

    override def head: T = {
        if (nonEmpty)
            data(0).asInstanceOf[T]
        else
            throw new NoSuchElementException
    }

    /**
     * Computes a slice.
     *
     * @param from The index of the first element (inclusive)
     * @param until The index of the last element (exclusive); if the last element is beyond the
     *              size of the underlying data-structure, null values will be added.
     * @return The sliced array.
     */
    override def slice(from: Int, until: Int): AnyRefArray[T] = {
        new AnyRefArray(JArrays.copyOfRange(data, from, until))
    }

    override def foreach[U](f: T ⇒ U): Unit = {
        val data = this.data
        val max = data.length
        var i = 0
        while (i < max) {
            f(data(i).asInstanceOf[T])
            i += 1
        }
    }

    override def partition(p: T ⇒ Boolean): (AnyRefArray[T], AnyRefArray[T]) = {
        val max = data.length
        val left = AnyRefArrayBuffer.withInitialSize[AnyRef](Math.max(8, max / 2))
        val right = AnyRefArrayBuffer.withInitialSize[AnyRef](Math.min(8, max / 2))
        var i = 0
        while (i < max) {
            val e = data(i)
            if (p(e.asInstanceOf[T])) {
                left += e
            } else {
                right += e
            }
            i += 1
        }
        (new AnyRefArray(left.toArray), new AnyRefArray(right.toArray))
    }

    def partitionByType[X <: AnyRef](clazz: Class[X]): (AnyRefArray[X], AnyRefArray[T]) = {
        val max = data.length
        val left = AnyRefArrayBuffer.withInitialSize[AnyRef](Math.max(8, max / 2))
        val right = AnyRefArrayBuffer.withInitialSize[AnyRef](Math.min(8, max / 2))
        var i = 0
        while (i < max) {
            val e = data(i)
            if (clazz.isInstance(e)) {
                left += e
            } else {
                right += e
            }
            i += 1
        }
        (new AnyRefArray[X](left.toArray), new AnyRefArray[T](right.toArray))
    }

    /** Appends the given element. */
    def :+[X >: T <: AnyRef](elem: X): AnyRefArray[X] = {
        val newData = JArrays.copyOf(data, data.length + 1)
        newData(data.length) = elem
        new AnyRefArray[X](newData)
    }

    override def iterator: AnyRefIterator[T] = new AnyRefIterator[T] {
        private[this] var i = 0
        override def hasNext: Boolean = i < data.length
        override def next(): T = { val e = data(i).asInstanceOf[T]; i += 1; e }
    }

    override def filter(f: T ⇒ Boolean): AnyRefArray[T] = {
        val b = AnyRefArray.newBuilder[T]
        val data = this.data
        val max = data.length
        var c = 0 // counts the number of filtered (retained) values
        b.sizeHint(Math.min(8, max))
        var i = 0
        while (i < max) {
            val e = data(i).asInstanceOf[T]
            if (f(e)) {
                b += e
                c += 1
            }
            i += 1
        }
        if (c == max)
            this
        else
            b.result()
    }

    def foldLeft(z: Int)(op: (Int, T) ⇒ Int): Int = iterator.foldLeft(z)(op)

    override def foldLeft[X](z: X)(op: (X, T) ⇒ X): X = {
        var result = z
        var i = 0
        val max = data.length
        while (i < max) {
            result = op(result, data(i).asInstanceOf[T])
            i += 1
        }
        result
    }

    def filterNonNull: AnyRefArray[T] = {
        var b: Builder[T, AnyRefArray[T]] = null // we initialize the builder only on demand
        val data = this.data
        val max = data.length
        var i = 0
        while (i < max) {
            val e = data(i)
            if (e == null && b == null) {
                b = AnyRefArray.newBuilder[T]
                b.sizeHint(max - 1)
                var p = 0
                while (p < i) {
                    b += data(p).asInstanceOf[T]
                    p += 1
                }
            } else if (b != null) {
                b += e.asInstanceOf[T]
            }
            i += 1
        }
        if (b == null)
            this
        else
            b.result()
    }

    override def filterNot(f: T ⇒ Boolean): AnyRefArray[T] = filter(e ⇒ !f(e))

    /**
     * Creates a new `AnyRefArray` where the value at the given index is replaced by
     * the given value.
     */
    def updated[X >: T](index: Int, e: X): AnyRefArray[X] = {
        val newData = java.util.Arrays.copyOf(data, data.length)
        newData(index) = e.asInstanceOf[AnyRef]
        new AnyRefArray(newData)
    }

    def binarySearch(comparator: T ⇒ Int): Option[T] = {
        findInArray(data)(e ⇒ comparator(e.asInstanceOf[T])).asInstanceOf[Option[T]]
    }

    def binarySearch[X >: T <: Comparable[X]](key: X): Int = {
        JArrays.binarySearch(data, 0, data.length, key.asInstanceOf[Object])
    }

    /**
     * Creates a new `AnyRefArray` where the given value is inserted at the specified
     * `insertionPoint`. If the underlying array happens to be sorted, then the insertion point can
     * easily be computed using `binarySearch`; it will be `-index -1` if the
     * returned index is less than zero; otherwise the value was already found in the array.
     */
    def insertedAt[X >: T <: AnyRef](insertionPoint: Int, e: X): AnyRefArray[X] = {
        val newData = JArrays.copyOf(data, data.length + 1)
        newData(insertionPoint) = e
        System.arraycopy(data, insertionPoint, newData, insertionPoint + 1, data.length - insertionPoint)
        new AnyRefArray(newData)
    }

    def zipWithIndex: AnyRefArray[(T, Int)] = {
        val max = data.length
        val newData = new Array[AnyRef](max)
        var i = 0
        while (i < max) {
            newData(i) = (data(i), i)
            i += 1
        }
        new AnyRefArray(newData)
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: AnyRefArray[_] ⇒ JArrays.equals(this.data, that.data)
            case _ ⇒
                false
        }
    }

    override lazy val hashCode: Int = JArrays.hashCode(data) * 11

    override def toString: String = data.mkString("AnyRefArray(", ", ", ")")

}

/**
 * Factory for [[AnyRefArray]]s.
 */
object AnyRefArray {

    /**
     * @note We can't enforce the type bound `<: AnyRef` here, because the builder is also
     *        used by the `AnyRefArray` where we can't enforce the type bound without giving
     *        up the the advantageous of inheriting from the standard scala collection types.
     *        However, the type bound is still in place and `T` is expected to be a subtype of
     *        `AnyRef`.
     */
    private[collection] def newBuilder[T]: Builder[T, AnyRefArray[T]] = {
        new Builder[T, AnyRefArray[T]] {
            private[this] var data: Array[AnyRef] = null
            private[this] var nextIndex = 0
            override def +=(e: T): this.type = {
                if (data == null) {
                    data = new Array[AnyRef](8)
                } else if (nextIndex == data.length) {
                    data = JArrays.copyOf(data, (nextIndex + 1) * 2)
                }
                data(nextIndex) = e.asInstanceOf[AnyRef]
                nextIndex += 1
                this
            }
            override def clear(): Unit = nextIndex = 0
            override def result(): AnyRefArray[T] = {
                if (data == null)
                    Empty
                else {
                    new AnyRefArray[T](
                        if (nextIndex == data.length) data else JArrays.copyOf(data, nextIndex)
                    )
                }
            }
            override def sizeHint(size: Int): Unit = {
                if (data == null) data = new Array[AnyRef](size)
            }
        }
    }

    val Empty: AnyRefArray[Nothing] = new AnyRefArray(new Array[AnyRef](0))

    def empty[T <: AnyRef]: AnyRefArray[T] = Empty.asInstanceOf[AnyRefArray[T]]

    def apply[T <: AnyRef](data: T*): AnyRefArray[T] = new AnyRefArray(data.toArray[AnyRef])

    def unapplySeq[T <: AnyRef](x: AnyRefArray[T]): Option[Seq[T]] = Some(x)

    /**
     * Creates a new [[AnyRefArray]] by cloning the given array.
     *
     * I.e., modifications to the given array will not be reflected.
     */
    def from[T <: AnyRef](data: Array[AnyRef]): AnyRefArray[T] = {
        new AnyRefArray(data.clone())
    }

    def from[T, X <: AnyRef](data: IndexedSeq[T])(f: T ⇒ X): AnyRefArray[X] = {
        val max = data.size
        val newData = new Array[AnyRef](max)
        var i = 0; while (i < max) { newData(i) = f(data(i)); i += 1 }
        new AnyRefArray[X](newData)
    }

    def from[T <: AnyRef](data: Array[Int])(f: Int ⇒ T): AnyRefArray[T] = {
        val max = data.length
        val newData = new Array[AnyRef](max)
        var i = 0; while (i < max) { newData(i) = f(data(i)); i += 1 }
        new AnyRefArray[T](newData)
    }

    /**
     * Creates a new [[AnyRefArray]] from the given array. Hence, changes to the
     * underlying array would be reflected!
     *
     * '''Only use this factory method if you have full control over all
     * aliases to the given array to ensure that the underlying array is not mutated.'''
     */
    // IMPROVE Use an ownership annotation to specify that AnyRefArray takes over the ownership of the array.
    def _UNSAFE_from[T <: AnyRef](data: Array[AnyRef]): AnyRefArray[T] = new AnyRefArray(data)

}
