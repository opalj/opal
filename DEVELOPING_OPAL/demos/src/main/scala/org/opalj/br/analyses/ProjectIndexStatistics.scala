/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.net.URL

/**
 * Some statistics about the usage of field/method names in a project.
 *
 * @author Michael Eichberg
 */
object ProjectIndexStatistics extends ProjectAnalysisApplication {

    override def title: String = "project statistics"

    override def description: String = {
        "statistics about the usage of field/method identifiers in a project"
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        BasicReport(
            project.get(ProjectIndexKey).
                statistics().map(kv => "- "+kv._1+": "+kv._2).
                mkString("Identifier usage statistics:\n\t", "\n\t", "\n")
        )
    }
}
