/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * A set of integers which supports (reasonable) efficient `headAndTail` operations.
 *
 * @author Michael Eichberg
 */
trait IntWorkSet[T <: IntWorkSet[T]] { intSet: T =>

    /**
     * Gets a value and returns the new set without that value.
     */
    def headAndTail: IntRefPair[T]

}
