/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

/**
 * Collection of predefined issue categories where each category describes
 * '''the quality property of the software that is affected'''
 * by the issue.
 *
 * @author Michael Eichberg
 */
object IssueCategory {

    final val AllCategories = {
        Set(Correctness, Performance, Comprehensibility)
    }

    /**
     * Code that may be incorrect.
     */
    final val Correctness = "correctness"

    /**
     * Performance issues are generally related to code that does things that
     * are superfluous; i.e., which - except from using time and memory - have
     * no meaningful sideeffect compared to a simpler solution.
     */
    final val Performance = "performance"

    /**
     * Code that most likely does what the developer wanted it to do, but
     * which is too clumsy and can be shortened.
     */
    final val Comprehensibility = "comprehensibility"

}
