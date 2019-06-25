/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection

/**
 * A set of long values.
 *
 * @author Michael Eichberg
 */
trait GrowableLongSet[T <: GrowableLongSet[T]] { longSet: T ⇒

    /**
     * Adds the given value to this set; returns `this` if this set already contains the value
     * otherwise a new set is returned.
     */
    def +(i: Long): T

    def isEmpty: Boolean

    def nonEmpty: Boolean = !isEmpty

    /**
     * Tests if this set has exactly one element (complexity: O(1)).
     */
    def isSingletonSet: Boolean

    def contains(value: Long): Boolean

    def foreach[U](f: Long ⇒ U): Unit

    def iterator: LongIterator

}
