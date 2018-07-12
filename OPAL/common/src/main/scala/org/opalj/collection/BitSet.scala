/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

import scala.collection.AbstractIterator

/**
 * Common interface of all BitSets provided by OPAL.
 *
 * @author Michael Eichberg
 */
trait BitSet { thisSet ⇒

    def isEmpty: Boolean

    def contains(i: Int): Boolean

    def intIterator: IntIterator

    /**
     * Standard Scala iterator provided for interoperability purposes only;
     * if possible use `intIterator`.
     */
    final def iterator: Iterator[Int] = new AbstractIterator[Int] {
        private[this] val it = thisSet.intIterator
        def hasNext: Boolean = it.hasNext
        def next(): Int = it.next()
    }

    final def mkString(pre: String, in: String, post: String): String = {
        intIterator.mkString(pre, in, post)
    }

    // + equals and hashCode
}
