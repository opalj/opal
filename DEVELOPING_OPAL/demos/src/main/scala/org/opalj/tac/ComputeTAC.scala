/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.io.File
import java.net.URL

import org.opalj.ai.cli.AIBasedCommandLineConfig
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.util.PerformanceEvaluation.time

/**
 * Shows how to get the 3-address code in the most efficient manner if it is required for
 * all methods and no property based computations should be carried out.
 *
 * @author Michael Eichberg
 */
object ComputeTAC extends ProjectsAnalysisApplication {

    protected class TACConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with AIBasedCommandLineConfig {
        val description = "Prints the 3-address code for all methods of all classes"
    }

    protected type ConfigType = TACConfig

    protected def createConfig(args: Array[String]): TACConfig = new TACConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: TACConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {

        val (project, tacProvider) = time {
            val (p, _) = analysisConfig.setupProject(cp)
            (p, p.get(EagerDetachedTACAIKey))
        } { t => println("Loading the project and computing the tac for all methods took: " + t.toSeconds) }

        // Now, you can use the TACProvider to get the TAC for a specific method.
        (project, BasicReport(tacProvider.asInstanceOf[scala.collection.Map[?, ?]].size.toString))
    }
}
