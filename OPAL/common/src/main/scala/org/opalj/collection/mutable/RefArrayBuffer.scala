/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.mutable

import scala.reflect.ClassTag

import java.util.{Arrays ⇒ JArrays}

import scala.collection.mutable.WrappedArray

import org.opalj.collection.RefIterator

/**
 * An array based implementation of a mutable buffer. This implementation offers highly
 * optimized, but very unsafe methods and is therefore __not__ a general purpose data-structure.
 * In general, this buffer should only be used to reference objects which outlive the life time
 * of the buffer AND where the buffer is only used locally. To foster a local usage only, we do
 * not inherit from any standard collection classes.
 *
 * @note This data structure is not thread safe.
 *
 * @param data The array containing the values.
 * @param size0 The number of stored values.
 * @author Michael Eichberg
 */
final class RefArrayBuffer[N >: Null <: AnyRef] private (
        private var data:  Array[N],
        private var size0: Int
) { buffer ⇒

    /**
     * Resets the size of the buffer, but does not clear the underlying array; hence,
     * the array may prevent the garbage collection of the still referenced values.
     * This is generally not a problem if the array is only used locally and the
     * referenced (dead) objects outlive the lifetime of the buffer!
     */
    def _UNSAFE_resetSize(): Unit = size0 = 0

    def size: Int = size0
    def length: Int = size0
    def isEmpty: Boolean = size0 == 0
    def nonEmpty: Boolean = size0 > 0

    def apply(index: Int): N = {
        if (index < 0 || index >= size0)
            throw new IndexOutOfBoundsException(s"$index (size: $size0)");

        data(index).asInstanceOf[N]
    }

    def toArray[T >: N: ClassTag]: Array[T] = {
        val target = new Array[T](size0)
        System.arraycopy(data, 0, target, 0, size0)
        target
    }

    /**
     * Returns a reference to the underlying mutable array if it is (by chance) completely
     * full; otherwise a new array which just contains the valid entries is returned.
     */
    // IMPROVE Design an annotation|analysis that ensures that the buffer is no longer used afterwards!
    def _UNSAFE_toArray: Array[N] = {
        if (size0 == data.length)
            data
        else
            JArrays.copyOf[N](data, size0)
    }

    def toSet[T >: N <: AnyRef]: Set[T] = {
        val b = Set.newBuilder[T]
        this.foreach(e ⇒ b += e)
        b.result()
    }

    /**
     * Extracts the slice of the given size.
     *
     * @param from the index of the first item (inclusive)
     * @param until the index of the last item (exclusive)
     */
    def slice(from: Int, until: Int = size0): IndexedSeq[N] = {
        if (until > size0)
            throw new IndexOutOfBoundsException(s"$until > $size0(size of buffer)")

        new WrappedArray.ofRef(java.util.Arrays.copyOfRange(data, from, until))
    }

    def ++=(is: Traversable[N]): this.type = {
        is.foreach(+=)
        this
    }

    def ++=(is: Iterator[N]): this.type = {
        is.foreach(+=)
        this
    }

    def ++=(other: RefArrayBuffer[N]): this.type = {
        if (data.length - size0 >= other.data.length) {
            System.arraycopy(other.data, 0, this.data, size0, other.data.length)
        } else {
            val newData = java.util.Arrays.copyOf(data, this.size0 + other.size0 + 8)
            System.arraycopy(other.data, 0, newData, this.size0, other.size0)
            data = newData
            this.data = newData
        }
        this.size0 = this.size0 + other.size0
        this
    }

    /**
     * Copies all values from the given array to this buffer in one step.
     */
    def ++=(other: Array[N]): this.type = {
        if (data.length - size0 >= other.length) {
            System.arraycopy(other, 0, this.data, size0, other.length)
        } else {
            val newData = java.util.Arrays.copyOf(data, this.size0 + other.length + 8)
            System.arraycopy(other, 0, newData, this.size0, other.length)
            data = newData
            this.data = newData
        }
        this.size0 = this.size0 + other.length
        this
    }

    def +=(i: N): this.type = {
        val size0 = this.size0
        var data = this.data
        if (data.length == size0) {
            val newData = java.util.Arrays.copyOf(data, (size0 + 1) * 2)
            data = newData
            this.data = newData
        }

        data(size0) = i
        this.size0 = size0 + 1
        this
    }

    def head: N = {
        if (this.size0 == 0)
            throw new NoSuchElementException("the buffer is empty");

        this.data(0)
    }

    def last: N = {
        if (this.size0 == 0)
            throw new NoSuchElementException("the buffer is empty");

        this.data(size0 - 1)
    }

    def foreach[U](f: N ⇒ U): Unit = {
        val data = this.data
        val size = this.size0
        var i = 0
        while (i < size) {
            f(data(i))
            i += 1
        }
    }

    /**
     * Returns an iterator which iterates over the values starting with the value
     * at the given `startIndex`.
     * '''The iterator will not check for updates of the underlying collection.'''
     *
     * @param startIndex index of the first element that will be returned (inclusive)
     */
    def iteratorFrom(startIndex: Int): RefIterator[N] = new RefIterator[N] {
        private[this] var index = startIndex
        override def hasNext: Boolean = index < size0
        override def next(): N = {
            val r = data(index)
            index += 1
            r.asInstanceOf[N]
        }
    }

    /**
     * Returns an iterator which iterates over the values in the specified range.
     * '''The iterator will not check for updates of the underlying collection.'''
     *
     * @note    The `next` method will throw an `IndexOutOfBoundsException`
     *          when all elements are already returned.
     *
     * @param from index of the first element that will be returned (inclusive)
     * @param until index of the last element (exclusive)
     */
    def iterator(from: Int = 0, until: Int = buffer.size0): RefIterator[N] = {
        val lastIndex = Math.min(until, buffer.size0)
        new RefIterator[N] {
            private[this] var index = from
            def hasNext: Boolean = index < lastIndex
            def next(): N = {
                val currentIndex = this.index
                val r = buffer.data(currentIndex)
                this.index = currentIndex + 1
                r.asInstanceOf[N]
            }
        }
    }

    override def toString: String = {
        s"RefArrayBuffer(size=$size0; data=${data.take(size0).mkString("[", ",", "]")})"
    }
}

object RefArrayBuffer {

    def empty[N >: Null <: AnyRef: ClassTag]: RefArrayBuffer[N] = withInitialSize[N](4)

    def withInitialSize[N >: Null <: AnyRef: ClassTag](initialSize: Int): RefArrayBuffer[N] = {
        new RefArrayBuffer[N](new Array[N](initialSize), 0)
    }

}
