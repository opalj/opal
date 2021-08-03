/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

/**
 * A set of longs which supports (reasonable) efficient `headAndTail` operations.
 *
 * @author Michael Eichberg
 */
trait LongWorkSet[T <: LongWorkSet[T]] { this: T =>

    /**
     * Gets a value and returns the new set without that value.
     */
    def headAndTail: LongRefPair[T]

}
