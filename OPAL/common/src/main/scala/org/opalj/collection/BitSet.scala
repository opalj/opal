/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

/**
 * Common interface of all BitSet based collections provided by OPAL.
 *
 * @author Michael Eichberg
 */
trait BitSet {

    def isEmpty: Boolean

    def contains(i: Int): Boolean

    /**
     * Iterator over the values stored in this set. In general, iteration over bit sets
     * is an expensive operation and other data structures such as IntTrieSets may be
     * reasonable alternatives.
     */
    def iterator: IntIterator

    final def mkString(pre: String, in: String, post: String): String = {
        iterator.mkString(pre, in, post)
    }

    // + equals and hashCode
}
