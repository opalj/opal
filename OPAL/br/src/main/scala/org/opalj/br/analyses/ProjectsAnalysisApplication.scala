/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.io.File
import java.net.URL
import java.util.Calendar

import org.rogach.scallop.exceptions.IncompleteBuildException

import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.ExecutionsArg
import org.opalj.log.LogMessage
import org.opalj.log.OPALLogger
import org.opalj.util.PerformanceEvaluation.time

/**
 * Default command-line runner implementation for analyses to be applied to one or more projects.
 *
 * Subclasses can either implement [[analyze]], creating a [[ReportableAnalysisResult]], or override [[evaluate]].
 *
 * @author Dominik Helm
 */
abstract class ProjectsAnalysisApplication {

    protected type ConfigType <: MultiProjectAnalysisConfig

    protected def createConfig(args: Array[String]): ConfigType

    protected def evaluate(
        cp:             Iterable[File],
        analysisConfig: ConfigType,
        execution:      Int
    ): Unit = {
        val (project, report) = analyze(cp, analysisConfig, execution)
        OPALLogger.log(LogMessage.plainInfo(report.toConsoleString))(using project.logContext)
    }

    protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ConfigType,
        execution:      Int
    ): (Project[URL], ReportableAnalysisResult) = ???

    def main(args: Array[String]): Unit = {

        val analysisConfig: ConfigType = createConfig(args)

        // This allows implementations to not call `init` themselves if they don't require early access to parameters
        try {
            analysisConfig.assertVerified()
        } catch {
            case _: IncompleteBuildException => analysisConfig.init()
        }

        val begin = Calendar.getInstance()
        Console.println(begin.getTime)

        for (i <- 0 until analysisConfig(ExecutionsArg)) {
            time {
                analysisConfig.foreachProject(evaluate, i)
            }(t => println("evaluation time: " + t.toSeconds))
        }

        val end = Calendar.getInstance()
        Console.println(end.getTime)
    }
}
