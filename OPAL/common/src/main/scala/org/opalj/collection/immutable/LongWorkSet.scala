/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

/**
 * A set of longs which supports (reasonable) efficient `getAndRemove` operations.
 *
 * @author Michael Eichberg
 */
trait LongWorkSet[T <: LongWorkSet[T]] { longSet: T ⇒

    /**
     * Gets a value and returns the new set without that value.
     */
    def getAndRemove: LongAnyRefPair[T]

}
