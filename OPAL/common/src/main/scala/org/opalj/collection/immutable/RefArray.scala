/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import java.util.{Arrays ⇒ JArrays}

import scala.collection.mutable.Builder

import org.opalj.collection.mutable.RefArrayBuffer
import org.opalj.control.{find ⇒ findInArray}

/**
 * Wraps an array such that the underlying array is no longer directly accessible and
 * therefore also no longer mutable if `RefArray` is the sole owner.
 *
 * @note Compared to `ConstArray`, `RefArray` does not provide an efficient
 *       `toArray` method. However, `RefArray`s are covariant.
 *
 * @author Michael Eichberg
 */
class RefArray[+T /* "<: AnyRef" this constraint is ONLY enforced by the factory methods to facilitate integration with the scala collection API */ ] private[collection] (
        private var data: Array[AnyRef]
) extends scala.collection.immutable.Seq[T] { // TODO [Scala 2.13] make it extend IndexedSeq.

    /**
     * Appends the given element to the underlying array; will cause havoc if this object
     * is not under full control of the caller.
     */
    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_added[X >: T <: AnyRef](elem: X): RefArray[X] = {
        val newData = JArrays.copyOf(data, data.length + 1)
        newData(data.length) = elem
        data = newData
        this.asInstanceOf[RefArray[X]]
    }

    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_addedAll[X >: T <: AnyRef](that: RefArray[X]): RefArray[X] = {
        val newData = JArrays.copyOf(data, this.data.length + that.data.length)
        System.arraycopy(that.data, 0, newData, this.data.length, that.data.length)
        data = newData
        this.asInstanceOf[RefArray[X]]
    }

    /**
     * Directly performs the map operation on the underlying array and then creates a new
     * appropriately typed `RefArray[X]` object which wraps the modified array. The
     * return value can be ignored, if `X == T`.
     *
     * '''This method is only to be used if `this` instance is no longer used afterwards!'''
     */
    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_mapped[X <: AnyRef](f: T ⇒ X): RefArray[X] = {
        var i = 0
        val max = data.length
        while (i < max) {
            data(i) = f(data(i).asInstanceOf[T])
            i += 1
        }
        new RefArray[X](data)
    }

    /**
     * Directly performs the sort operation on the underlying array.
     *
     * '''This method is only to be used if `this` instance is no longer used afterwards!'''
     */
    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_sortedWith(compare: (AnyRef, AnyRef) ⇒ Boolean): this.type = {
        JArrays.parallelSort[AnyRef](data, Ordering.fromLessThan(compare))
        this
    }

    /**
     * Directly updates the value at the given index and then creates a new
     * appropriately typed `RefArray[X]` object which wraps the modified array. The returned
     * type can be ignored if the type X == T.
     *
     * '''This method is only to be used if `this` instance is no longer used afterwards!'''
     */
    // IMPROVE Design annotation (+Analysis) that ensures that this operation is only performed if – after the usage of this method - the reference to this data-structure will not be used anymore.
    def _UNSAFE_replaced[X >: T <: AnyRef](index: Int, e: X): RefArray[X] = {
        data(index) = e
        new RefArray(data)
    }

    /**
     * Returns a new RefArray where the values are sorted based on their natural ordering.
     *
     * @example
     * {{{
     *     RefArray("c","a").sorted[String]
     * }}}
     */
    def _UNSAFE_sorted[X >: T](implicit ev: T <:< Comparable[X]): this.type = {
        ignore(ev) // <= HACK... should have no effect at runtime.
        JArrays.parallelSort[AnyRef](data, null)
        this
    }

    //
    //
    // SAFE OPERATIONS, THAT DO NOT MANIPULATE THE DATA-STRUCTURE IN PLACE
    //
    //

    override def dropRight(n: Int): RefArray[T] = {
        if (n == 0)
            return this;

        val newLength = data.length - n
        if (newLength <= 0)
            return RefArray.empty;

        new RefArray(JArrays.copyOf(data, newLength))
    }

    def ++[X >: T <: AnyRef](that: RefArray[X]): RefArray[X] = {
        val newData = JArrays.copyOf(data, this.data.length + that.data.length)
        System.arraycopy(that.data, 0, newData, this.data.length, that.data.length)
        new RefArray(newData)
    }

    def ++[X >: T <: AnyRef](that: Seq[X]): RefArray[X] = {
        val thisLength = this.data.length
        val newData = JArrays.copyOf(data, thisLength + that.length)
        var i = thisLength
        that.foreach { v ⇒
            newData(i) = v
            i += 1
        }
        new RefArray(newData)
    }

    def +:[X >: T <: AnyRef](e: X): RefArray[X] = {
        val thisLength = this.data.length
        val newData = new Array[AnyRef](thisLength + 1)
        newData(0) = e
        System.arraycopy(this.data, 0, newData, 1, thisLength)
        new RefArray(newData)
    }

    def map[X <: AnyRef](f: T ⇒ X): RefArray[X] = {
        val newData = new Array[AnyRef](data.length)
        var i = 0
        val max = data.length
        while (i < max) {
            newData(i) = f(data(i).asInstanceOf[T])
            i += 1
        }
        new RefArray[X](newData)
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

    def flatMap[X <: AnyRef](f: T ⇒ TraversableOnce[X]): RefArray[X] = {
        val b = RefArray.newBuilder[X]
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

    def sortWith[X >: T](compare: (X, X) ⇒ Boolean): RefArray[T] = {
        val newData = data.clone
        JArrays.parallelSort(newData, Ordering.fromLessThan(compare).asInstanceOf[Ordering[AnyRef]])
        new RefArray[T](newData)
    }

    /** The following method is "just" required to shut-up the compiler. */
    @inline final private[this] def ignore(x: AnyRef): AnyRef = x

    /**
     * Returns a new RefArray where the values are sorted based on their natural ordering.
     *
     * @example
     * {{{
     *     RefArray("c","a").sorted[String]
     * }}}
     */
    @inline final def sorted[X >: T](implicit ev: T <:< Comparable[X]): RefArray[T] = {
        ignore(ev) // <= HACK... should have no effect at runtime.
        val newData = data.clone
        JArrays.parallelSort[AnyRef](newData, null)
        new RefArray[T](newData)
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
    override def slice(from: Int, until: Int): RefArray[T] = {
        new RefArray(JArrays.copyOfRange(data, from, until))
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

    override def partition(p: T ⇒ Boolean): (RefArray[T], RefArray[T]) = {
        val max = data.length
        val left = RefArrayBuffer.withInitialSize[AnyRef](Math.max(8, max / 2))
        val right = RefArrayBuffer.withInitialSize[AnyRef](Math.min(8, max / 2))
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
        (new RefArray(left.toArray), new RefArray(right.toArray))
    }

    def partitionByType[X <: AnyRef](clazz: Class[X]): (RefArray[X], RefArray[T]) = {
        val max = data.length
        val left = RefArrayBuffer.withInitialSize[AnyRef](Math.max(8, max / 2))
        val right = RefArrayBuffer.withInitialSize[AnyRef](Math.min(8, max / 2))
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
        (new RefArray[X](left.toArray), new RefArray[T](right.toArray))
    }

    /** Appends the given element. */
    def :+[X >: T <: AnyRef](elem: X): RefArray[X] = {
        val newData = JArrays.copyOf(data, data.length + 1)
        newData(data.length) = elem
        new RefArray[X](newData)
    }

    override def iterator: RefIterator[T] = new RefIterator[T] {
        private[this] var i = 0
        override def hasNext: Boolean = i < data.length
        override def next(): T = { val e = data(i).asInstanceOf[T]; i += 1; e }
    }

    override def filter(f: T ⇒ Boolean): RefArray[T] = {
        // IMPROVE Only create new array if required!
        val b = RefArray.newUnconstrainedBuilder[T]
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

    def filterNonNull: RefArray[T] = {
        var b: Builder[T, RefArray[T]] = null // we initialize the builder only on demand
        val data = this.data
        val max = data.length
        var i = 0
        while (i < max) {
            val e = data(i)
            if (e == null) {
                // let's ignore "e"
                if (b == null) {
                    b = RefArray.newUnconstrainedBuilder[T]
                    b.sizeHint(max - 1)
                    var p = 0
                    while (p < i) {
                        b += data(p).asInstanceOf[T]
                        p += 1
                    }
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

    override def filterNot(f: T ⇒ Boolean): RefArray[T] = filter(e ⇒ !f(e))

    /**
     * Creates a new `RefArray` where the value at the given index is replaced by
     * the given value.
     */
    def updated[X >: T](index: Int, e: X): RefArray[X] = {
        val newData = java.util.Arrays.copyOf(data, data.length)
        newData(index) = e.asInstanceOf[AnyRef]
        new RefArray(newData)
    }

    def binarySearch(comparator: T ⇒ Int): Option[T] = {
        findInArray(data)(e ⇒ comparator(e.asInstanceOf[T])).asInstanceOf[Option[T]]
    }

    def binarySearch[X >: T <: Comparable[X]](key: X): Int = {
        JArrays.binarySearch(data, 0, data.length, key.asInstanceOf[Object])
    }

    /**
     * Creates a new `RefArray` where the given value is inserted at the specified
     * `insertionPoint`. If the underlying array happens to be sorted, then the insertion point can
     * easily be computed using `binarySearch`; it will be `-index -1` if the
     * returned index is less than zero; otherwise the value was already found in the array.
     */
    def insertedAt[X >: T <: AnyRef](insertionPoint: Int, e: X): RefArray[X] = {
        val newData = JArrays.copyOf(data, data.length + 1)
        newData(insertionPoint) = e
        System.arraycopy(data, insertionPoint, newData, insertionPoint + 1, data.length - insertionPoint)
        new RefArray(newData)
    }

    def zipWithIndex: RefArray[(T, Int)] = {
        val max = data.length
        val newData = new Array[AnyRef](max)
        var i = 0
        while (i < max) {
            newData(i) = (data(i), i)
            i += 1
        }
        new RefArray(newData)
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: RefArray[_] ⇒ JArrays.equals(this.data, that.data)
            case _ ⇒
                false
        }
    }

    override lazy val hashCode: Int = JArrays.hashCode(data) * 11

    override def toString: String = data.mkString("RefArray(", ", ", ")")

}

/**
 * Factory for [[RefArray]]s.
 */
object RefArray {

    /**
     * @note   We can't enforce the type bound `<: AnyRef` here, because the builder is also
     *         used by `RefArray` where we can't enforce the type bound without giving
     *         up the the advantageous of inheriting from the standard scala collection types.
     *         However, the type bound is still in place and `T` is expected to be a subtype of
     *         `AnyRef`.
     */
    private[collection] def newUnconstrainedBuilder[T]: Builder[T, RefArray[T]] = {
        new Builder[T, RefArray[T]] {
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
            override def result(): RefArray[T] = {
                if (data == null)
                    Empty
                else {
                    new RefArray[T](
                        if (nextIndex == data.length) data else JArrays.copyOf(data, nextIndex)
                    )
                }
            }
            override def sizeHint(size: Int): Unit = {
                if (data == null) data = new Array[AnyRef](size)
            }
        }
    }

    def newBuilder[T <: AnyRef]: Builder[T, RefArray[T]] = newUnconstrainedBuilder[T]

    val Empty: RefArray[Nothing] = new RefArray(new Array[AnyRef](0))

    def empty[T <: AnyRef]: RefArray[T] = Empty.asInstanceOf[RefArray[T]]

    def apply[T <: AnyRef](e: T): RefArray[T] = new RefArray(Array[AnyRef](e))

    def apply[T <: AnyRef](e1: T, e2: T): RefArray[T] = new RefArray(Array[AnyRef](e1, e2))

    def apply[T <: AnyRef](e1: T, e2: T, e3: T): RefArray[T] = {
        new RefArray(Array[AnyRef](e1, e2, e3))
    }

    def fill[T <: AnyRef](n: Int)(f: ⇒ T): RefArray[T] = {
        val data = new Array[AnyRef](n)
        var i = 0
        while (i < n) {
            data(i) = f
            i += 1
        }
        new RefArray[T](data)
    }

    def apply[T <: AnyRef](e1: T, e2: T, e3: T, data: T*): RefArray[T] = {
        val max = data.length
        val newData = new Array[AnyRef](data.length + 3)
        newData(0) = e1
        newData(1) = e2
        newData(2) = e3
        var i = 0
        while (i < max) {
            newData(i + 3) = data(i)
            i += 1
        }
        new RefArray(newData)
    }

    def unapplySeq[T <: AnyRef](x: RefArray[T]): Option[Seq[T]] = Some(x)

    def withSize[T <: AnyRef](size: Int): RefArray[T] = new RefArray(new Array[AnyRef](size))

    /**
     * Creates a new [[RefArray]] by cloning the given array.
     *
     * I.e., modifications to the given array will not be reflected.
     */
    def from[T <: AnyRef](data: Array[AnyRef]): RefArray[T] = {
        new RefArray(data.clone())
    }

    def from[T <: AnyRef](take: Int, it: Iterator[AnyRef]): RefArray[T] = {
        val data = new Array[AnyRef](take)
        var i = 0
        while (i < take && it.hasNext) {
            data(i) = it.next()
            i += 1
        }
        new RefArray(data)
    }

    def from[T, X <: AnyRef](data: IndexedSeq[T])(f: T ⇒ X): RefArray[X] = {
        val max = data.size
        val newData = new Array[AnyRef](max)
        var i = 0; while (i < max) { newData(i) = f(data(i)); i += 1 }
        new RefArray[X](newData)
    }

    def from[T <: AnyRef](data: Array[Int])(f: Int ⇒ T): RefArray[T] = {
        val max = data.length
        val newData = new Array[AnyRef](max)
        var i = 0; while (i < max) { newData(i) = f(data(i)); i += 1 }
        new RefArray[T](newData)
    }

    /**
     * Creates a new [[RefArray]] from the given array. Hence, changes to the
     * underlying array would be reflected!
     *
     * '''Only use this factory method if you have full control over all
     * aliases to the given array to ensure that the underlying array is not mutated.'''
     */
    // IMPROVE Use an ownership annotation to specify that RefArray takes over the ownership of the array.
    def _UNSAFE_from[T <: AnyRef](data: Array[AnyRef]): RefArray[T] = new RefArray(data)

}
