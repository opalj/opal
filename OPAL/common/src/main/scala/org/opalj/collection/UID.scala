/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

/**
 * Identifies objects which have a – potentially context dependent – unique id.
 * I.e., this trait is implemented by objects that have – by construction -
 * unique ids in a well-defined scope. The `UIDSet` is based on comparing uids.
 *
 * @author Michael Eichberg
 */
trait UID {

    /**
     * This object's context dependent unique id.
     */
    def id: Int
}

/**
 * Identifies objects which have a – potentially context dependent – unique id.
 * I.e., this trait is implemented by objects that have – by construction -
 * unique ids in a well-defined scope.
 *
 * @note   Two objects that are ''not equal'' may have the same id, if both objects
 *         do not have the same context.
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
            case that: UID ⇒ UID.areEqual(this, that)
            case _         ⇒ false
        }
    }

    final def ===(that: UID): Boolean = UID.areEqual(this, that)

    /**
     * The unique id.
     */
    final override def hashCode: Int = id

}

/**
 * Helper methods related to data structures that have unique ids.
 */
object UID {

    final def areEqual(a: UID, b: UID): Boolean = (a eq b) || a.id == b.id

    /**
     * Returns the element stored in the given array at the position identified
     * by the [[UID]]'s unique `id` or – if no value is stored at the respective
     * position – sets the value using the value returned by `orElse` and
     * returns that value.
     */
    @inline
    final def getOrElseUpdate[T <: AnyRef](array: Array[T], uid: UID, orElse: ⇒ T): T = {
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
