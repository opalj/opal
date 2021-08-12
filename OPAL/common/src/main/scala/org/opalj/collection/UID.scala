/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

/**
 * Identifies objects which have - in the scope where the objects are used - unique ids.
 * I.e., this trait is implemented by objects that have – by construction -
 * unique ids in a well-defined scope. The `UIDSet` is based on comparing uids.
 *
 * @author Michael Eichberg
 */
trait UID extends AnyRef {

    /**
     * This object's unique id.
     */
    def id: Int
}

/**
 * Identifies objects which have a – potentially context dependent – unique id.
 * I.e., this trait is implemented by objects that have – by construction -
 * unique ids in a well-defined scope.
 *
 * @note   Two objects that are ''not equal'' may still have the same id, if both objects
 *         are guaranteed to never be compared against each other.
 *
 * @author Michael Eichberg
 */
trait UIDValue extends UID {

    /**
     * Two objects with a unique id are considered equal if they have the same unique id;
     * all other properties will be ignored!
     */
    final override def equals(other: Any): Boolean = {
        other match {
            case that: UID => (this eq that) || this.id == that.id
            case _         => false
        }
    }

    final def equals(that: UID): Boolean = (this eq that) || ((that ne null) && this.id == that.id)

    /**
     * The unique id.
     */
    final override def hashCode: Int = id

}

/**
 * Helper methods related to data structures that have unique ids.
 */
object UID {

    /**
     * Returns the element stored in the given array at the position identified
     * by the [[UID]]'s unique `id` or – if no value is stored at the respective
     * position – sets the value using the value returned by `orElse` and
     * returns that value.
     */
    @inline
    final def getOrElseUpdate[T <: AnyRef](array: Array[T], uid: UID, orElse: => T): T = {
        val id = uid.id
        val t = array(id)
        if (t eq null) {
            val result = orElse
            array(id) = result
            result
        } else {
            t
        }
    }

    /**
     * Ordering for sorted collections of elements of type `UID`.
     *
     * @author Michael Eichberg
     */
    implicit object UIDBasedOrdering extends Ordering[UID] {

        def compare(a: UID, b: UID): Int = a.id - b.id

    }

}
