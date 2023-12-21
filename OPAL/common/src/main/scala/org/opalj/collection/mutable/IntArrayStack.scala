/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package mutable

import scala.collection.SeqFactory
import scala.collection.SpecificIterableFactory
import scala.collection.mutable

/**
 * An array based implementation of a mutable stack of `int` values which has a
 * given initial size. If the stack is non-empty, the index of the top value is `0` and the
 * index of the bottom value is (`length-1`).
 *
 * @param data The array containing the values.
 * @param size0 The number of stored values.
 * @author Michael Eichberg
 */
final class IntArrayStack private (
        private var data:  Array[Int],
        private var size0: Int
) extends mutable.IndexedSeq[Int]
    with mutable.IndexedSeqOps[Int, mutable.Stack, IntArrayStack]
    with mutable.Cloneable[IntArrayStack]
    with Serializable { stack =>

    def this(initialSize: Int = 4) = this(new Array[Int](initialSize), 0)

    override def length: Int = size0
    override def isEmpty: Boolean = size0 == 0

    override def apply(index: Int): Int = {
        val size0 = this.size0
        val valueIndex = size0 - 1 - index
        if (valueIndex < 0 || valueIndex >= size0)
            throw new IndexOutOfBoundsException(s"$index (size: $size0)");

        data(valueIndex)
    }

    override def empty: IntArrayStack = IntArrayStack.empty
    override def iterableFactory: SeqFactory[mutable.Stack] = mutable.Stack(this).iterableFactory
    override def fromSpecific(coll: IterableOnce[Int]): IntArrayStack = IntArrayStack.fromSpecific(coll)
    override protected def newSpecificBuilder: mutable.Builder[Int, IntArrayStack] = IntArrayStack.newBuilder

    override def update(index: Int, v: Int): Unit = data(size0 - 1 - index) = v

    override def reverse: IntArrayStack = {
        val newData = new Array[Int](size0)
        val max = size0 - 1
        var i = 0
        while (i <= max) {
            newData(max - i) = data(i)
            i += 1
        }
        new IntArrayStack(newData, size0)
    }

    /** The same as push but additionally returns `this`. */
    def +=(i: Int): this.type = {
        push(i)
        this
    }

    def push(i: Int): Unit = {
        val size0 = this.size0
        var data = this.data
        if (data.length == size0) {
            val newData = new Array[Int]((size0 + 1) * 2)
            System.arraycopy(data, 0, newData, 0, size0)
            data = newData
            this.data = newData
        }

        data(size0) = i
        this.size0 = size0 + 1
    }

    /**
     * Pushes the value of the given stack on this stack while maintaining the order
     * in which the values were pushed on the given stack. I.e.,
     * if this contains the values `[1|2->` and the given one the values `[3,4->`
     * then the resulting stack will contain the values `[1|2|3|4...`.
     *
     * @note In case of `++` the order of the values is reversed.
     */
    def push(that: IntArrayStack): Unit = {
        val thatSize = that.size0

        if (thatSize == 0) {
            return ;
        }

        val thisSize = this.size0
        var thisData = this.data

        val newSize = thisSize + thatSize
        if (newSize > thisData.length) {
            val newData = new Array[Int](newSize + 10)
            System.arraycopy(thisData, 0, newData, 0, thisSize)
            thisData = newData
            this.data = thisData
        }

        System.arraycopy(that.data, 0, thisData, thisSize, thatSize)

        this.size0 = newSize
    }

    /**
     * Returns and removes the top most value from the stack.
     *
     * @note If the stack is empty a `NoSuchElementException` will be thrown.
     */
    def pop(): Int = {
        val index = this.size0 - 1
        if (index < 0)
            throw new NoSuchElementException("the stack is empty");

        val i = this.data(index)
        this.size0 = index
        i
    }

    /**
     * Returns the stack's top-most value.
     *
     * @note If the stack is empty a `NoSuchElementException` will be thrown.
     */
    def top(): Int = {
        val index = this.size0 - 1
        if (index < 0)
            throw new NoSuchElementException("the stack is empty");

        this.data(index)
    }

    /**
     * Same as `top()`, but potentially less efficient due to (un)boxing if the head method
     * of the supertype is called.
     */
    override /*TraversableLike*/ def head: Int = top()

    override /*TraversableLike*/ def last: Int = {
        if (this.size0 == 0)
            throw new NoSuchElementException("the stack is empty");

        this.data(0)
    }

    override def foreach[U](f: Int => U): Unit = {
        val data = this.data
        var i = this.size0 - 1
        while (i >= 0) {
            f(data(i))
            i -= 1
        }
    }

    def foreachReverse[U](f: Int => U): Unit = {
        val data = this.data
        val max = this.size0 - 1
        var i = 0
        while (i <= max) {
            f(data(i))
            i += 1
        }
    }

    override def foldLeft[B](z: B)(f: (B, Int) => B): B = {
        val data = this.data
        var v = z
        var i = this.size0 - 1
        while (i >= 0) {
            v = f(v, data(i))
            i -= 1
        }
        v
    }

    /**
     * Returns an iterator which produces the values in LIFO order.
     *
     * @note    The `next` method will throw an `IndexOutOfBoundsException`
     *          when all elements are already returned.
     */
    override def iterator: IntIterator = new IntIterator {
        var currentIndex = stack.size0 - 1
        def hasNext: Boolean = currentIndex >= 0

        def next(): Int = {
            val currentIndex = this.currentIndex
            val r = stack.data(currentIndex)
            this.currentIndex = currentIndex - 1
            r
        }

    }

    def toArray: Array[Int] = java.util.Arrays.copyOfRange(data, 0, size0)

    override def clone(): IntArrayStack = new IntArrayStack(data.clone(), size0)

    override def toString: String = {
        s"IntArrayStack(/*size=$size0;*/data=${data.take(size0).mkString("[", ",", "->")})"
    }
}

/**
 * Factory to create [[IntArrayStack]]s.
 */
object IntArrayStack extends SpecificIterableFactory[Int, IntArrayStack] {

    class IntArrayStackBuilder(var stack: IntArrayStack) extends mutable.Builder[Int, IntArrayStack] {
        override def addOne(elem: Int): this.type = {
            stack += elem
            this
        }
        override def clear(): Unit = stack = empty
        override def result(): IntArrayStack = stack
    }

    override def empty: IntArrayStack = new IntArrayStack

    override def fromSpecific(it: IterableOnce[Int]): IntArrayStack = {
        val builder = newBuilder
        val iterator = it.iterator
        while (iterator.hasNext)
            builder.addOne(iterator.next())
        builder.result()
    }

    override def newBuilder =
        new IntArrayStackBuilder(empty)

    /**
     * Creates a new stack based on a given sequence. The last value of the sequence will
     * be the top value of the stack.
     */
    def fromSeq(seq: IterableOnce[Int]): IntArrayStack = fromSpecific(seq)

    def apply(value: Int): IntArrayStack = {
        val initialArray = new Array[Int](10)
        initialArray(0) = value
        new IntArrayStack(initialArray, 1)
    }
}
