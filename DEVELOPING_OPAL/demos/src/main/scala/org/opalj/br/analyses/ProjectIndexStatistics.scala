/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.io.File

import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

/**
 * Some statistics about the usage of field/method names in a project.
 *
 * @author Michael Eichberg
 */
object ProjectIndexStatistics extends ProjectsAnalysisApplication {

    protected class ProjectIndexStatisticsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects statistics about the usage of field/method identifiers"
    }

    protected type ConfigType = ProjectIndexStatisticsConfig

    protected def createConfig(args: Array[String]): ProjectIndexStatisticsConfig =
        new ProjectIndexStatisticsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ProjectIndexStatisticsConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        (
            project,
            BasicReport(
                project.get(ProjectIndexKey)
                    .statistics().map(kv => "- " + kv._1 + ": " + kv._2)
                    .mkString("Identifier usage statistics:\n\t", "\n\t", "\n")
            )
        )
    }
}
