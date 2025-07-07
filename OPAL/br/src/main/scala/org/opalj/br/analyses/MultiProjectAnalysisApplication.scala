/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.language.implicitConversions

import java.io.File
import java.util.Calendar

import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.cli.ProjectBasedCommandLineConfig
import org.opalj.cli.ExecutionsArg
import org.opalj.log.LogMessage
import org.opalj.log.OPALLogger
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

import org.rogach.scallop.ScallopConf

/**
 * Default command-line runner implementation for analyses to be applied to a single projects
 *
 * @author Dominik Helm
 */
abstract class SingleProjectAnalysisApplication {

    type ConfigType <: ScallopConf with ProjectBasedCommandLineConfig

    def evaluate(project: SomeProject, projectTime: Seconds, analysisConfig: ConfigType, execution: Int): Unit = {
        val report = analyze(project, projectTime, analysisConfig, execution)
        OPALLogger.log(LogMessage.plainInfo(report.toConsoleString))(project.logContext)
    }

    def analyze(
        project:        SomeProject,
        projectTime:    Seconds,
        analysisConfig: ConfigType,
        execution:      Int
    ): ReportableAnalysisResult = ???

    def createConfig(args: Array[String]): ConfigType

    def main(args: Array[String]): Unit = {

        val analysisConfig: ConfigType = createConfig(args)

        val begin = Calendar.getInstance()
        Console.println(begin.getTime)

        for (i <- 0 until analysisConfig(ExecutionsArg)) {
            val (project, projectTime) = analysisConfig.setupProject()

            time {
                evaluate(project, projectTime, analysisConfig, i)
            } { t => OPALLogger.info("analysis progress", s"analysis finished in ${t.toSeconds} ")(project.logContext) }
        }

        val end = Calendar.getInstance()
        Console.println(end.getTime)
    }

}

/**
 * Default command-line runner implementation for analyses to be applied to one or more projects
 *
 * @author Dominik Helm
 */
abstract class MultiProjectAnalysisApplication {

    protected type ConfigType <: ScallopConf with MultiProjectAnalysisConfig[ConfigType]

    protected def evaluate(
        cp:             Iterable[File],
        analysisConfig: ConfigType,
        execution:      Int
    ): Unit = {
        val (project, report) = analyze(cp, analysisConfig, execution)
        OPALLogger.log(LogMessage.plainInfo(report.toConsoleString))(project.logContext)
    }

    protected def analyze(cp: Iterable[File], analysisConfig: ConfigType, execution: Int): (
        SomeProject,
        ReportableAnalysisResult
    ) =
        ???

    protected def createConfig(args: Array[String]): ConfigType

    def main(args: Array[String]): Unit = {

        val analysisConfig: ConfigType = createConfig(args)

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
