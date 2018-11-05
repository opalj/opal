/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package mutable

import scala.collection.mutable
import scala.collection.generic

/**
 * An array based implementation of a mutable stack of `ref` values which has a
 * given initial size. If the stack is non-empty, the index of the top value is `0` and the
 * index of the bottom value is (`length-1`).
 *
 * @note __This stack generally keeps all references and is only intended to be used to
 *       store elements that outlive the stack. Otherwise, garbage collection may be prevented.__
 *
 * @param data The array containing the values.
 * @param size0 The number of stored values.
 * @author Michael Eichberg
 */
final class RefArrayStack[N >: Null <: AnyRef] private (
        private var data:  Array[AnyRef],
        private var size0: Int
) extends mutable.IndexedSeq[N]
    with mutable.IndexedSeqLike[N, RefArrayStack[N]]
    with mutable.Cloneable[RefArrayStack[N]]
    with Serializable { stack ⇒

    def this(initialSize: Int = 4) { this(new Array[AnyRef](initialSize), 0) }

    def this(e: N, initialSize: Int) {
        this(new Array[AnyRef](Math.max(initialSize, 1)), 1)
        data(0) = e
    }

    /**
     * Resets the size of the stack, but does not clear the underlying array; hence,
     * the stack may prevent the garbage collection of the still referenced values.
     * This is generally not a problem, if the stack is only used locally and the
     * referenced objects outlive the lifetime of the stack!
     */
    def resetSize(): Unit = size0 = 0

    override def size: Int = size0
    override def length: Int = size0
    override def isEmpty: Boolean = size0 == 0
    override def nonEmpty: Boolean = size0 > 0

    /**
     * Returns the n-th value of the stack; where n = 0 identifies the top level value!
     */
    override def apply(index: Int): N = {
        val size0 = this.size0
        val valueIndex = size0 - 1 - index
        if (valueIndex < 0 || valueIndex >= size0)
            throw new IndexOutOfBoundsException(s"$index (size: $size0)");

        data(valueIndex).asInstanceOf[N]
    }

    /**
     * Returns the n-th value of the stack; where n = 0 identifies the bottom value!
     */
    def fromBottom(index: Int): N = {
        if (index < 0 || index >= this.size0)
            throw new IndexOutOfBoundsException(s"$index (from bottom) (size: $size0)");
        data(index).asInstanceOf[N]
    }

    override def update(index: Int, v: N): Unit = data(size0 - 1 - index) = v

    override def newBuilder: mutable.Builder[N, RefArrayStack[N]] = RefArrayStack.newBuilder[N]

    /** The same as push but additionally returns `this`. */
    final def +=(i: N): this.type = {
        push(i)
        this
    }

    final def ++=(is: TraversableOnce[N]): this.type = {
        is foreach { push }
        this
    }

    def push(i: N): Unit = {
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
    }

    /**
     * Pushes the value of the given stack on this stack while maintaining the order
     * in which the values were pushed on the given stack. I.e.,
     * if this contains the values `[1|2->` and the given one the values `[3,4->`
     * then the resulting stack will contain the values `[1|2|3|4...`.
     *
     * @note In case of `++` the order of the values is reversed.
     */
    def push(that: RefArrayStack[N]): Unit = {
        val thatSize = that.size0

        if (thatSize == 0) {
            return ;
        }

        val thisSize = this.size0
        var thisData = this.data

        val newSize = thisSize + thatSize
        if (newSize > thisData.length) {
            val newData = new Array[AnyRef](newSize + 10)
            System.arraycopy(thisData, 0, newData, 0, thisSize)
            thisData = newData
            this.data = thisData
        }

        System.arraycopy(that.data, 0, thisData, thisSize, thatSize)

        this.size0 = newSize
    }

    /**
     * Returns and virtually removes the top most value from the stack.
     *
     * @note If the stack is empty a `NoSuchElementException` will be thrown.
     */
    def pop(): N = {
        val index = this.size0 - 1
        if (index < 0)
            throw new NoSuchElementException("the stack is empty");

        val i = this.data(index)
        this.size0 = index
        i.asInstanceOf[N]
    }

    /**
     * Returns the stack's top-most value.
     *
     * @note If the stack is empty a `NoSuchElementException` will be thrown.
     */
    def top: N = {
        val index = this.size0 - 1
        if (index < 0)
            throw new NoSuchElementException("the stack is empty");

        this.data(index).asInstanceOf[N]
    }

    /** @see `top` */
    final def peek: N = top

    /**
     * Same as `top`.
     */
    override /*TraversableLike*/ def head: N = top

    override /*TraversableLike*/ def last: N = {
        if (this.size0 == 0)
            throw new NoSuchElementException("the stack is empty");

        this.data(0).asInstanceOf[N]
    }

    override def foreach[U](f: N ⇒ U): Unit = {
        val data = this.data
        var i = this.size0 - 1
        while (i >= 0) {
            f(data(i).asInstanceOf[N])
            i -= 1
        }
    }

    override def foldLeft[B](z: B)(f: (B, N) ⇒ B): B = {
        val data = this.data
        var v = z
        var i = this.size0 - 1
        while (i >= 0) {
            v = f(v, data(i).asInstanceOf[N])
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
    override def iterator: RefIterator[N] = new RefIterator[N] {
        private[this] var currentIndex = stack.size0 - 1
        def hasNext: Boolean = currentIndex >= 0

        def next(): N = {
            val currentIndex = this.currentIndex
            val r = stack.data(currentIndex)
            this.currentIndex = currentIndex - 1
            r.asInstanceOf[N]
        }
    }

    override def clone(): RefArrayStack[N] = new RefArrayStack(data.clone(), size0)

    override def toString: String = {
        s"RefArrayStack(/*size=$size0;*/data=${data.take(size0).mkString("[", ",", "→")})"
    }
}

/**
 * Factory to create [[RefArrayStack]]s.
 */
object RefArrayStack {

    implicit def canBuildFrom[N >: Null <: AnyRef]: generic.CanBuildFrom[RefArrayStack[N], N, RefArrayStack[N]] = {
        new generic.CanBuildFrom[RefArrayStack[N], N, RefArrayStack[N]] {
            def apply(): mutable.Builder[N, RefArrayStack[N]] = newBuilder
            def apply(from: RefArrayStack[N]): mutable.Builder[N, RefArrayStack[N]] = newBuilder
        }
    }

    def newBuilder[N >: Null <: AnyRef]: mutable.Builder[N, RefArrayStack[N]] = {
        new mutable.ArrayBuffer[N] mapResult fromSeq
    }

    /**
     * Creates a new stack based on a given sequence. The last value of the sequence will
     * be the top value of the stack.
     */
    def fromSeq[N >: Null <: AnyRef](seq: TraversableOnce[N]): RefArrayStack[N] = {
        seq.foldLeft(new RefArrayStack[N](8))(_ += _)
    }

    def empty[N >: Null <: AnyRef]: RefArrayStack[N] = new RefArrayStack
}

