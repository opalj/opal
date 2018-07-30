/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import scala.language.higherKinds

import java.util.Arrays

import scala.reflect.ClassTag
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
        private val data: Array[_ <: T]
) extends IndexedSeq[T] with IndexedSeqOptimized[T, ConstArray[T]] {

    // Required to ensure that "map" creates a ConstArray whenever possible.
    override def newBuilder: Builder[T, ConstArray[T]] = ConstArray.newBuilder[T]()

    override def apply(idx: Int): T = data(idx)

    final override def length: Int = data.length
    final override def size: Int = data.length
    final override def foreach[U](f: T ⇒ U): Unit = {
        val data = this.data
        var i = 0
        val max = data.length
        while (i < max) {
            f(data(i))
            i += 1
        }
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

    override lazy val hashCode: Int = Arrays.hashCode(data.asInstanceOf[Array[Object]]) * 11

    override def toString: String = data.mkString("ConstArray(", ", ", ")")

}

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

/**
 * Factory for [[ConstArray]]s.
 */
object ConstArray extends LowLevelConstArrayImplicits {

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

    final val EmptyConstArray = new ConstArray[Null](new Array[Null](0))

    def find[T <: AnyRef](sortedConstArray: ConstArray[T])(evaluate: T ⇒ Int): Option[T] = {
        findInArray(sortedConstArray.data)(evaluate)
    }

    def empty[T <: AnyRef]: ConstArray[T] = EmptyConstArray.asInstanceOf[ConstArray[T]]

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
    def from[T <: AnyRef](data: Array[T]): ConstArray[T] = new ConstArray(data)

}
