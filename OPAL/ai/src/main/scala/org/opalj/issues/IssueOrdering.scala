/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package issues

/**
 * Defines a partial order for issues. The issues are sorted
 * first by the relevance, then by their locations, then by the categories then
 * by the kinds then by the analyses ids and at last by the summary.
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
                case (l1, l2) if (l1 compareTo l2) != 0 ⇒ l1 compareTo l2
            } match {
                case Some(result) ⇒ result
                case _            ⇒ // let's continue the comparison
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
