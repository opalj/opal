/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

/**
 * Can be mixed in if the iteration order is always that same independent of the insertion order.
 * This is typically the case if the values are (pseudo-)sorted.
 *
 * @author Michael Eichberg
 */
trait IntCollectionWithStableOrdering[T <: IntSet[T]] { intSet: T =>

    def subsetOf(other: T): Boolean = {
        var thisSize = this.size
        var otherSize = other.size
        if (thisSize > otherSize)
            return false;

        val thisIt = this.iterator
        val otherIt = other.iterator
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
