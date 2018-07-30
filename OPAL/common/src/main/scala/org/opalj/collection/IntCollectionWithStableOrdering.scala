/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

/**
 * Can be mixed in if the iteration order of the underlying data-structure is independent
 * of the insertion order. This is generally the case if the values are (pseudo-) sorted and
 * is never the case for, e.g., linked lists.
 *
 * @author Michael Eichberg
 */
trait IntCollectionWithStableOrdering[T <: IntSet[T]] { intSet: T ⇒

    def subsetOf(other: T): Boolean = {
        var thisSize = this.size
        var otherSize = other.size
        if (thisSize > otherSize)
            return false;

        val thisIt = this.intIterator
        val otherIt = other.intIterator
        while (thisIt.hasNext && otherIt.hasNext) {
            val thisV = thisIt.next()
            thisSize -= 1
            var otherV = otherIt.next()
            otherSize -= 1
            while (thisSize <= otherSize && otherIt.hasNext && otherV != thisV) {
                otherV = otherIt.next()
                otherSize -= 1
            }
            if (thisSize > otherSize)
                return false; // there are definitively not enough remaining elements
            if (thisV != otherV)
                return false; // ... we reach this point when we did not find a match for the last element
        }
        !thisIt.hasNext
    }

}
