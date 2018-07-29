/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.tutorial.base

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project

/**
 * @author Michael Eichberg
 */
object AnalysisTemplate extends DefaultOneStepAnalysis {

    def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        BasicReport(theProject.statistics.mkString("\n"))
    }

}

