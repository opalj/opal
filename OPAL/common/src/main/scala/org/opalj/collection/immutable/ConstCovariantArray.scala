/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

import scala.reflect.ClassTag

import java.util.Arrays

import org.opalj.control.{find ⇒ findInArray}

/**
 * Wraps an array such that the underlying array is no longer directly accessible and
 * therefore also no longer mutable if `ConstCovariantArray` is the sole owner.
 *
 * @author Michael Eichberg
 */
final class ConstCovariantArray[+T <: AnyRef] private (
        private val data: Array[_ <: T]
) {

    def length: Int = data.length
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

    def iterator: Iterator[T] = data.iterator

    override def equals(other: Any): Boolean = {
        other match {
            case that: ConstCovariantArray[_] ⇒
                this.length == that.length && {
                    val thisIt = this.iterator
                    val thatIt = that.iterator
                    while (thisIt.hasNext && thisIt.next == thatIt.next) { /*continue*/ }
                    !thisIt.hasNext // <=> all elements are equal
                }
            case _ ⇒
                false
        }
    }

    override lazy val hashCode: Int = Arrays.hashCode(data.asInstanceOf[Array[Object]]) * 11

    override def toString: String = data.mkString("ConstCovariantArray(", ", ", ")")

}

/**
 * Factory for [[ConstCovariantArray]]s.
 */
object ConstCovariantArray {

    val EmptyConstCovariantArray = new ConstCovariantArray[Null](new Array(0))

    def find[T <: AnyRef](sortedConstArray: ConstCovariantArray[T])(evaluate: T ⇒ Int): Option[T] = {
        findInArray(sortedConstArray.data)(evaluate)
    }

    def empty[T >: Null <: AnyRef]: ConstCovariantArray[T] = {
        EmptyConstCovariantArray.asInstanceOf[ConstCovariantArray[T]]
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
    def from[T <: AnyRef](data: Array[T]): ConstCovariantArray[T] = new ConstCovariantArray(data)

}
