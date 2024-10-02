/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package tutorial
package base

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication

/**
 * @author Michael Eichberg
 */
object AnalysisTemplate extends ProjectAnalysisApplication {

    def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        BasicReport(theProject.statistics.mkString("\n"))
    }

}
