/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds

import scala.language.existentials

import java.io.File
import java.io.PrintWriter

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.cli.OutputFileArg
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.IFDSAnalysis
import org.opalj.ifds.IFDSAnalysisScheduler
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.util.Milliseconds
import org.opalj.util.PerformanceEvaluation.time

/**
 * Setup to run different Evaluations of IFDS Analyses
 *
 * @author Marc Clement
 */
abstract class IFDSEvaluationRunner extends ProjectsAnalysisApplication {

    protected val additionalDescription: String

    protected class IFDSRunnerConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with CGBasedCommandLineConfig {
        val description = s"Performs an IFDS analysis for $additionalDescription"

        args(
            OutputFileArg
        )
    }

    protected type ConfigType <: IFDSRunnerConfig

    protected def createConfig(args: Array[String]): ConfigType

    protected def analysisClass(analysisConfig: ConfigType): IFDSAnalysisScheduler[_, _, _, _]

    protected def printAnalysisResults(analysis: IFDSAnalysis[_, _, _, _], ps: PropertyStore): Unit = ()

    protected type AdditionalResultsType

    override protected def evaluate(
        cp:             Iterable[File],
        analysisConfig: ConfigType,
        execution:      Int
    ): Unit = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (ps, _) = analysisConfig.setupPropertyStore(project)
        analysisConfig.setupCallGaph(project)

        var analysisTime: Milliseconds = Milliseconds.None
        println("Start: " + new java.util.Date)
        org.opalj.util.gc()
        val analysis =
            time {
                project.get(FPCFAnalysesManagerKey).runAll(analysisClass(analysisConfig))._2
            }(t => analysisTime = t.toMilliseconds).collect {
                case (_, a: IFDSAnalysis[_, _, _, _]) => a
            }.head

        printAnalysisResults(analysis, ps)
        println(s"The analysis took $analysisTime.")
        println(
            ps.statistics.iterator
                .map(_.toString())
                .toList
                .sorted
                .mkString("PropertyStore Statistics:\n\t", "\n\t", "\n")
        )

        val statistics = analysis.statistics
        val additionalEvaluationResults = additionalEvaluationResult(analysis)

        if (analysisConfig.get(OutputFileArg).isDefined) {
            val pw = new PrintWriter(analysisConfig(OutputFileArg))
            pw.println(s"Time: ${analysisTime}")
            pw.println(s"Calls of normalFlow: ${statistics.normalFlow}")
            pw.println(s"Calls of callFlow: ${statistics.callFlow}")
            pw.println(s"Calls of returnFlow: ${statistics.returnFlow}")
            pw.println(s"Calls of callToReturnFlow: ${statistics.callToReturnFlow}")
            if (additionalEvaluationResults.nonEmpty)
                writeAdditionalEvaluationResultsToFile(pw, additionalEvaluationResults.get)
            pw.close()
        }
    }

    protected def additionalEvaluationResult(analysis: IFDSAnalysis[_, _, _, _]): Option[AdditionalResultsType] = None

    protected def writeAdditionalEvaluationResultsToFile(
        writer:                      PrintWriter,
        additionalEvaluationResults: AdditionalResultsType
    ): Unit = {}

    protected def canBeCalledFromOutside(
        method:        DeclaredMethod,
        propertyStore: PropertyStore
    ): Boolean =
        propertyStore(method, Callers.key) match {
            // This is the case, if the method may be called from outside the library.
            case FinalEP(_, p: Callers) => p.hasCallersWithUnknownContext
            case _                      =>
                throw new IllegalStateException(
                    "call graph mut be computed before the analysis starts"
                )
        }

}

abstract class BasicIFDSEvaluationRunner extends IFDSEvaluationRunner {

    protected type ConfigType = IFDSRunnerConfig

    protected def createConfig(args: Array[String]): IFDSRunnerConfig = new IFDSRunnerConfig(args)

    protected type AdditionalResultsType = Nothing
}
