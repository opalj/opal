/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package dependency

import java.io.File
import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.de.DependencyCountingDependencyProcessor
import org.opalj.de.DependencyExtractor
import org.opalj.de.FilterSelfDependencies
import org.opalj.log.OPALLogger
import org.opalj.util.PerformanceEvaluation.time

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * Counts the number of dependencies found in a project.
 *
 * @author Michael Eichberg
 */
object DependencyCount extends ProjectsAnalysisApplication {

    protected class DependencyCountConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Counts the number of inter-source element dependencies"
    }

    protected type ConfigType = DependencyCountConfig

    protected def createConfig(args: Array[String]): DependencyCountConfig = new DependencyCountConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: DependencyCountConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val counter = time {
            val counter = new DependencyCountingDependencyProcessor with FilterSelfDependencies
            val extractor = new DependencyExtractor(counter)
            // process the class files in parallel to speed up the collection process
            project.allClassFiles.par foreach (extractor.process)
            counter
        } { t => OPALLogger.info("analysis progress", s"Counting the dependencies took $t")(using project.logContext) }

        (
            project,
            BasicReport(
                (f"Number of inter source-element dependencies: ${counter.currentDependencyCount}%,9d%n") +
                    f"Number of dependencies on primitive types:   ${counter.currentDependencyOnPrimitivesCount}%,9d%n" +
                    f"Number of dependencies on array types:       ${counter.currentDependencyOnArraysCount}%,9d%n"
            )
        )
    }
}
