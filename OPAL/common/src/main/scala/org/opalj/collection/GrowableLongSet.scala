/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection

/**
 * A set of long values.
 *
 * @author Michael Eichberg
 */
trait GrowableLongSet[T <: GrowableLongSet[T]] extends AnyRef { longSet: T ⇒

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

    /**
     * The size of the set; may not be a constant operation; if possible use isEmpty, nonEmpty,
     * etc.; or lookup the complexity in the concrete data structures.
     */
    def size: Int

    def contains(value: Long): Boolean

    def foreach[U](f: Long ⇒ U): Unit

    def iterator: LongIterator

}
