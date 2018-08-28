/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import scala.reflect.ClassTag

import java.util.{Arrays ⇒ JArrays}
import java.lang.System.arraycopy

import scala.collection.IndexedSeqOptimized
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder
import scala.collection.mutable.ArrayBuffer

import org.opalj.control.{find ⇒ findInArray}

/**
 * Wraps an array such that the underlying array is no longer directly accessible and
 * therefore also no longer mutable if `ConstArray` is the sole owner.
 *
 * @author Michael Eichberg
 */
final class ConstArray[T <: AnyRef] private (
        private val data: Array[T]
) extends IndexedSeq[T] with IndexedSeqOptimized[T, ConstArray[T]] {

    // Required to ensure that "map" creates a ConstArray whenever possible.
    override def newBuilder: Builder[T, ConstArray[T]] = ConstArray.newBuilder[T]()

    override def apply(idx: Int): T = data(idx)

    override def length: Int = data.length
    override def size: Int = data.length
    override def foreach[U](f: T ⇒ U): Unit = {
        val data = this.data
        var i = 0
        val max = data.length
        while (i < max) {
            f(data(i))
            i += 1
        }
    }

    override def iterator: AnyRefIterator[T] = new AnyRefIterator[T] {
        private[this] var i = 0
        override def hasNext: Boolean = i < data.length
        override def next(): T = {
            val e = data(i)
            i += 1
            e
        }
    }

    /**
     * Creates a new `ConstArray` where the value at the given index is replaced by the given value.
     */
    def replaced(index: Int, e: T): ConstArray[T] = {
        val newData = JArrays.copyOf(data, data.length)
        newData(index) = e
        new ConstArray(newData)
    }

    def binarySearch(key: Comparable[T]): Int = {
        JArrays.binarySearch(data.asInstanceOf[Array[Object]], 0, data.length, key)
    }

    /**
     * Creates a new `ConstArray` where the given value is inserted at the specified
     * `insertionPoint`. If the underlying array happens to be sorted, then the insertion point can
     * easily be computed using `binarySearch`; it will be `-index -1` if the
     * returned index is less than zero.
     */
    def insertAt(insertionPoint: Int, e: T): ConstArray[T] = {
        val newData = JArrays.copyOf(data, data.length + 1)
        newData(insertionPoint) = e
        arraycopy(data, insertionPoint, newData, insertionPoint + 1, data.length - insertionPoint)
        new ConstArray(newData)
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: IndexedSeq[_] ⇒
                this.length == that.length && {
                    val thisIt = this.toIterator
                    val thatIt = that.toIterator
                    while (thisIt.hasNext && thisIt.next == thatIt.next) { /*continue*/ }
                    !thisIt.hasNext // <=> all elements are equal
                }
            case _ ⇒
                false
        }
    }

    override def canEqual(that: Any): Boolean = that.isInstanceOf[IndexedSeq[T]]

    override lazy val hashCode: Int = JArrays.hashCode(data.asInstanceOf[Array[Object]]) * 11

    override def toString: String = data.mkString("ConstArray(", ", ", ")")

}

/*
/**
 * Low level implicits to; e.g., enable a mapping of an ''arbitrary'' data structure to
 * a [[ConstArray]].
 *
 * @example {{{ val c : ConstArray[List[String]] = List("a","b").map(List(_))}}}
 */
trait LowLevelConstArrayImplicits {

    implicit def canBuildFromAnything[T <: AnyRef, X, CC[X]]: CanBuildFrom[CC[_], T, ConstArray[T]] =
        new CanBuildFrom[CC[_], T, ConstArray[T]] {
            def apply(): Builder[T, ConstArray[T]] = ConstArray.newBuilder[T]()
            def apply(from: CC[_]): Builder[T, ConstArray[T]] = ConstArray.newBuilder[T]()
        }

}
*/

/**
 * Factory for [[ConstArray]]s.
 */
object ConstArray /*extends LowLevelConstArrayImplicits*/ {

    def newBuilder[T <: AnyRef](sizeHint: Int = 8): Builder[T, ConstArray[T]] = {
        val builder = new ArrayBuffer[T](sizeHint)
        builder mapResult (r ⇒ ConstArray(r.toArray[Object].asInstanceOf[Array[T]]))
    }

    implicit def canBuildFrom[T <: AnyRef]: CanBuildFrom[ConstArray[_ <: AnyRef], T, ConstArray[T]] = {
        new CanBuildFrom[ConstArray[_], T, ConstArray[T]] {
            def apply(): Builder[T, ConstArray[T]] = newBuilder[T]()
            def apply(from: ConstArray[_]): Builder[T, ConstArray[T]] = newBuilder[T](from.size)
        }
    }

    def find[T <: AnyRef](sortedConstArray: ConstArray[T])(evaluate: T ⇒ Int): Option[T] = {
        findInArray(sortedConstArray.data)(evaluate)
    }

    def empty[T <: AnyRef: ClassTag]: ConstArray[T] = new ConstArray[T](new Array[T](0))

    def apply[T <: AnyRef: ClassTag](data: T*): ConstArray[T] = new ConstArray(data.toArray)

    /**
     * Creates a new [[ConstArray]] by cloning the given array.
     *
     * I.e., modifications to the given array will not be reflected.
     */
    def apply[T <: AnyRef](data: Array[T]): ConstArray[T] = new ConstArray(data.clone())

    /**
     * Creates a new [[ConstArray]] from the given array. Hence, changes to the underlying array
     * would be reflected! '''Only use this factory method if you have full control over all
     * aliases to the given array to ensure that the underlying array is not mutated.'''
     */
    // IMPROVE Use an ownership annotation to specify that ConstArray takes over the ownership of the array.
    def _UNSAFE_from[T <: AnyRef](data: Array[T]): ConstArray[T] = new ConstArray(data)

}
