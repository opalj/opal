/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

/**
 * Defines a partial order for issues. The issues are sorted
 * first by the relevance, then by their locations, then by the categories, then
 * by the kinds, then by the analyses' ids and at last by the summary.
 *
 * @author Michael Eichberg
 */
object IssueOrdering extends scala.math.Ordering[Issue] {

    def compare(x: Set[String], y: Set[String]): Int = {
        if (x.size != y.size) {
            x.size - y.size
        } else {
            val xUniques = (x -- y)
            if (xUniques.isEmpty) {
                0
            } else {
                val xUniqueHead = xUniques.toSeq.sorted.head
                val yUniqueHead = (y -- x).toSeq.sorted.head
                xUniqueHead compare yUniqueHead
            }
        }
    }

    def compare(x: Issue, y: Issue): Int = {
        if (x.relevance.value < y.relevance.value) return -1;
        if (x.relevance.value > y.relevance.value) return 1;

        if (x.locations.size != y.locations.size) return x.locations.size - y.locations.size;
        else {
            x.locations.zip(y.locations).collectFirst {
                case (l1, l2) if (l1 compareTo l2) != 0 => l1 compareTo l2
            } match {
                case Some(result) => result
                case _            => // let's continue the comparison
            }
        }

        val categoriesComparison = compare(x.categories, y.categories)
        if (categoriesComparison != 0) return categoriesComparison;

        val kindsComparison = compare(x.kinds, y.kinds)
        if (kindsComparison != 0) return kindsComparison;

        if (x.analysis < y.analysis) return -1;
        if (x.analysis > y.analysis) return 1;

        // last resort...
        x.summary compare (y.summary)
    }

}
