/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import scala.reflect.ClassTag

import java.util.{Arrays ⇒ JArrays}

import org.opalj.control.{find ⇒ findInArray}

/**
 * Wraps an array such that the underlying array is no longer directly accessible and
 * therefore also no longer mutable if `ConstCovariantArray` is the sole owner.
 *
 * @author Michael Eichberg
 */
final class ConstCovariantArray[+T <: AnyRef] private (private[this] val data: Array[_ <: T]) {

    def size: Int = data.length

    def foreach[U](f: T ⇒ U): Unit = {
        val data = this.data
        var i = 0
        val max = data.length
        while (i < max) {
            f(data(i))
            i += 1
        }
    }

    def binarySearch(comparator: T ⇒ Int): Option[T] = findInArray(data)(comparator)

    def iterator: AnyRefIterator[T] = new AnyRefIterator[T] {
        private[this] var i = 0
        override def hasNext: Boolean = i < data.length
        override def next(): T = { val e = data(i); i += 1; e }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: ConstCovariantArray[_] ⇒
                this.size == that.size && {
                    val thisIt = this.iterator
                    val thatIt = that.iterator
                    while (thisIt.hasNext && thisIt.next == thatIt.next) { /*continue*/ }
                    !thisIt.hasNext // <=> all elements are equal
                }
            case _ ⇒
                false
        }
    }

    override lazy val hashCode: Int = JArrays.hashCode(data.asInstanceOf[Array[Object]]) * 11

    override def toString: String = data.mkString("ConstCovariantArray(", ", ", ")")

}

/**
 * Factory for [[ConstCovariantArray]]s.
 */
object ConstCovariantArray {

    private[this] val Empty = new ConstCovariantArray[Null](new Array[Null](0))

    def empty[T >: Null <: AnyRef]: ConstCovariantArray[T] = {
        Empty.asInstanceOf[ConstCovariantArray[T]]
    }

    def apply[T <: AnyRef: ClassTag](data: T*): ConstCovariantArray[T] = new ConstCovariantArray(data.toArray)

    /**
     * Creates a new [[ConstArray]] by cloning the given array.
     *
     * I.e., modifications to the given array will not be reflected.
     */
    def apply[T <: AnyRef](data: Array[T]): ConstCovariantArray[T] = new ConstCovariantArray(data.clone())

    /**
     * Creates a new [[ConstCovariantArray]] from the given array. Hence, changes to the underlying array
     * would be reflected! '''Only use this factory method if you have full control over all
     * aliases to the given array to ensure that the underlying array is not mutated.'''
     */
    // IMPROVE Use an ownership annotation to specify that ConstCovariantArray takes over the ownership of the array.
    def unsafeCreate[T <: AnyRef](data: Array[T]): ConstCovariantArray[T] = new ConstCovariantArray(data)

}
