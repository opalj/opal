/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package tutorial
package base

import java.io.File

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

/**
 * Basic template for a command-line runner for an analysis.
 *
 * @author Michael Eichberg
 */
object AnalysisTemplate extends ProjectsAnalysisApplication {

    /* Mix in further config traits to set up further necessary components such as a property store, abstract interpretation or call graph */
    protected class AnalysisConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects basic size metrics about a project"
    }

    protected type ConfigType = AnalysisConfig

    protected def createConfig(args: Array[String]): AnalysisConfig = new AnalysisConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: AnalysisConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        /* The actual analysis goes here */
        (project, BasicReport(project.statistics.mkString("\n")))
    }

}
