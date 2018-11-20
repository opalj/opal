/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

/**
 * Identifies a collection as being (guaranteed) complete or as being potentially incomplete.
 *
 * This class is typically used by analyses that derive some results and which are also able to
 * do so of in cases incomplete information. But in latter cases the analyses may not be able
 * to determine whether the derived information is complete or not. For example, imagine you
 * are analyzing some library (but not the JDK). In this case the class hierarchy will be incomplete
 * and every analysis using it may compute incomplete information.
 *
 * @author Michael Eichberg
 */
sealed trait QualifiedCollection[+S] {

    /**
     * The underlying collection.
     */
    def s: S

    /**
     * Returns `true` if the underlying collection is guaranteed to contain all elements with
     * respect to some query/analysis. I.e., if the analysis is not conclusive, then `false`
     * is returned. However, it may still be the case that the underlying collection contains
     * some or all elements, but that cannot be finally deduced.
     */
    def isComplete: Boolean

    /**
     * Returns `true` if the underlying collection is not guaranteed to contain all elements (w.r.t.
     * some query/analysis/...
     */
    final def isIncomplete: Boolean = !isComplete
}

case class CompleteCollection[+S](s: S) extends QualifiedCollection[S] {
    final def isComplete: Boolean = true
}

case class IncompleteCollection[+S](s: S) extends QualifiedCollection[S] {
    final def isComplete: Boolean = false
}
