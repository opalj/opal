/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * An analysis that performs all computations in one step. Only very short-running
 * analyses should use this interface as reporting progress is not supported.
 *
 * @author Michael Eichberg
 */
trait OneStepAnalysis[Source, +AnalysisResult] extends Analysis[Source, AnalysisResult] {

    /*abstract*/ def doAnalyze(
        project:       Project[Source],
        parameters:    Seq[String]     = List.empty,
        isInterrupted: () => Boolean
    ): AnalysisResult

    final override def analyze(
        project:                Project[Source],
        parameters:             Seq[String]               = List.empty,
        initProgressManagement: Int => ProgressManagement = ProgressManagement.None
    ): AnalysisResult = {

        val pm = initProgressManagement(1 /* number of steps */ )
        pm.progress(1, ProgressEvents.Start, Some(title))
        var wasKilled = false
        val result = doAnalyze(
            project, parameters, () => { wasKilled = pm.isInterrupted(); wasKilled }
        )

        if (wasKilled)
            pm.progress(-1, ProgressEvents.Killed, None)
        else
            pm.progress(1, ProgressEvents.End, None)

        result
    }

}
