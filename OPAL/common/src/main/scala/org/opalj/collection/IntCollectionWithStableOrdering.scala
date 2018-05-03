/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
