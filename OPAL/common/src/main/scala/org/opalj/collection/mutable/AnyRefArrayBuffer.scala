/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.mutable

import scala.collection.AbstractIterator
import scala.collection.mutable
import scala.reflect.ClassTag

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
final class AnyRefArrayBuffer[N >: Null <: AnyRef] private (
        private var data:  Array[AnyRef],
        private var size0: Int
) { buffer ⇒

    def this(initialSize: Int = 4) { this(new Array[AnyRef](initialSize), 0) }

    /**
     * Resets the size of the buffer, but does not clear the underlying array; hence,
     * the array may prevent the garbage collection of the still referenced values.
     * This is generally not a problem, if the array is only used locally and the
     * referenced (dead) objects outlive the lifetime of the buffer!
     */
    def resetSize(): Unit = size0 = 0

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

    def toSet[T >: N]: Set[T] = {
        val b = Set.newBuilder[T]
        this.foreach(e ⇒ b += e)
        b.result()
    }

    def slice(startIndex: Int, endIndex: Int = size0 - 1): IndexedSeq[N] = {
        val r = new mutable.ArrayBuffer[N]((endIndex - startIndex) + 1)
        iterator(startIndex, endIndex).foreach(e ⇒ r += e)
        r
    }

    def ++=(is: Traversable[N]): this.type = {
        is.foreach(+=)
        this
    }

    def ++=(is: Iterator[N]): this.type = {
        is.foreach(+=)
        this
    }

    def ++=(other: AnyRefArrayBuffer[N]): this.type = {
        if (data.length - size0 >= other.data.length) {
            System.arraycopy(other.data, 0, this.data, size0, other.data.length)

        } else {
            val newData = new Array[AnyRef](this.size0 + other.size0 + 8)
            System.arraycopy(data, 0, newData, 0, size0)
            System.arraycopy(other.data, 0, newData, this.size0, other.size0)
            data = newData
            this.data = newData
        }
        this.size0 = this.size0 + other.size0
        this
    }

    def +=(i: N): this.type = {
        val size0 = this.size0
        var data = this.data
        if (data.length == size0) {
            val newData = new Array[AnyRef]((size0 + 1) * 2)
            System.arraycopy(data, 0, newData, 0, size0)
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

        this.data(0).asInstanceOf[N]
    }

    def last: N = {
        if (this.size0 == 0)
            throw new NoSuchElementException("the buffer is empty");

        this.data(size0 - 1).asInstanceOf[N]
    }

    def foreach[U](f: N ⇒ U): Unit = {
        val data = this.data
        val size = this.size0
        var i = 0
        while (i < size) {
            f(data(i).asInstanceOf[N])
            i += 1
        }
    }

    def iteratorFrom(startIndex: Int): Iterator[N] = new AbstractIterator[N] {
        var currentIndex = startIndex
        override def hasNext: Boolean = currentIndex < size0
        override def next(): N = {
            val r = data(currentIndex)
            currentIndex += 1
            r.asInstanceOf[N]
        }
    }

    /**
     * Returns an iterator which iterates over the values in the specified range.
     *
     * @note    The `next` method will throw an `IndexOutOfBoundsException`
     *          when all elements are already returned.
     *
     * @param startIndex index of the first element that will be returned (inclusive)
     * @param endIndex index of the last element that will be returned.
     */
    def iterator(startIndex: Int = 0, endIndex: Int = buffer.size0 - 1): Iterator[N] = {
        val lastIndex = Math.min(endIndex, buffer.size0 - 1)
        new AbstractIterator[N] {

            var currentIndex = startIndex

            def hasNext: Boolean = currentIndex <= lastIndex

            def next(): N = {
                val currentIndex = this.currentIndex
                val r = buffer.data(currentIndex)
                this.currentIndex = currentIndex + 1
                r.asInstanceOf[N]
            }

        }
    }

    override def toString: String = {
        s"AnyRefArrayBuffer(size=$size0; data=${data.take(size0).mkString("[", ",", "]")})"
    }
}

