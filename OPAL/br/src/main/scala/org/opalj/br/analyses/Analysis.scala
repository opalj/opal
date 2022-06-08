/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * Common trait that analyses can inherit from when they want to use the general
 * analysis framework [[AnalysisApplication]].
 *
 * ==Conceptual Idea==
 * An analysis is basically a mapping of a `Project`'s resources to some result.
 *
 * Each analysis can produce (optionally) some result. E.g., a text describing a
 * scenario that leads to a bug, a graph, a report that identifies a specific
 * line or a combination thereof.
 *
 * However, an analysis should never rely on the location of a resource. If an analysis
 * needs access to further resources, it should use the `Project` class.
 *
 * @author Michael Eichberg
 */
trait Analysis[Source, +AnalysisResult] {

    // ------------------------------------------------------------------------------------------
    //
    // Meta-data
    //

    /**
     * A textual description of this analysis.
     *
     * The description should discuss:
     *  - the goal of the analysis
     *  - weaknesses of the analysis; i.e., whether the analysis may report false
     *    positives or may not report existing bugs (i.e., whether the analysis is
     *    subject to false negatives.)
     *  - if applicable, it should discuss what the developer could/should do in general
     *    to remedy the situation
     *  - if applicable it should discuss the severeness of the found results. I.e.,
     *    whether immediate action is required because a bug was found that will
     *    show up at runtime or if it is a security bug.
     *  - if applicable it should give an example. I.e., what the expected result is given
     *    a project with certain resources.
     */
    def description: String = "See project documentation."

    /**
     * A URL at which documentation about this analysis can be found. This allows user
     * interfaces to show a link for the user to click on, as a way to access further
     * documentation about this analysis.
     *
     * For example, for a command line interface, outputting the entire `description` to
     * the console may not be desirable, and it could show this URL instead.
     *
     * This is just a `String`, not a `java.net.URL`, because we do not intend to use it
     * as an URL internally. It is just a text string that can be shown to the user.
     */
    def documentationUrl: Option[String] = None

    /**
     * The copyright statement which contains less than 124 character and no line-breaks.
     */
    def copyright: String = "See project documentation."

    /**
     * A short descriptive title which should contain less than 64 characters and no
     * line-breaks.
     *
     * The default is the simple name of the class implementing the analysis.
     */
    def title: String = {
        val name =
            if (this.getClass.isAnonymousClass || this.getClass.isLocalClass) {
                val declaringClass = this.getClass.getDeclaringClass
                if (declaringClass != null)
                    declaringClass.getSimpleName
                else
                    this.getClass.getEnclosingClass.getSimpleName
            } else {
                this.getClass.getSimpleName
            }

        if (name.endsWith("$")) {
            name.init
        } else
            name
    }

    // ------------------------------------------------------------------------------------------
    //
    // The analysis
    //

    /**
     * Analyzes the given project and reports the result(s).
     *
     * @param initProgressManagement Returns a [[org.opalj.br.analyses.ProgressManagement]] object.
     *      The function is called by the analysis for each major analysis with the
     *      number of steps (Int) that will be performed. The analysis will subsequently use the
     *      returned object to report status information (related to that part of the analysis)
     *      and to check the interrupted status.
     *      The number of steps is at least 1.
     *      The analysis may call this function multiple times. However, the '''last `End`
     *      event always has to be signaled using the first `ProgressManagement` object'''.
     *      In other words, logically nested calls are supported, but chaining is not.
     *      A legal call sequence could be:
     *      {{{
     *      val pouter = initProgressManagement(2)
     *      pouter.progress(1,Start,Some("call graph analysis"))
     *          // ... construct call graph
     *      pouter.progress(1,End,None)
     *      pouter.progress(2,Start,Some("analyzing class files"))
     *          val p2 = initProgressManagement(500)
     *          // SEVERAL CLASS FILES ARE ANALYZED IN PARALLEL:
     *          p2.progress(1,Start,Some("java.lang.Object"))
     *          p2.progress(2,Start,Some("java.util.ArrayList"))
     *          p2.progress(3,Start,Some("java.lang.String"))
     *          p2.progress(2,End,Some("java.util.ArrayList"))
     *          p2.progress(4,Start,Some("java.util.Date"))
     *          ...
     *          p2.progress(500,End,None)
     *      pouter.progress(2,End,None)
     *      }}}
     *
     * @return The analysis' result. If the analysis was aborted/killed, the analysis
     *      should return an appropriate result (which might be `null`) and this
     *      has to be specified/documented by the analysis.
     */
    def analyze(
        project:                Project[Source],
        parameters:             Seq[String]               = Seq.empty,
        initProgressManagement: Int => ProgressManagement
    ): AnalysisResult

}
