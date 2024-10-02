/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds

import scala.language.existentials

import java.io.File
import java.io.PrintWriter

import com.typesafe.config.ConfigValueFactory

import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.bytecode
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.ifds.IFDSAnalysis
import org.opalj.ifds.IFDSAnalysisScheduler
import org.opalj.ifds.Statistics
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.util.Milliseconds
import org.opalj.util.PerformanceEvaluation.time

/**
 * Setup to run different Evaluations of IFDS Analyses
 *
 * @author Marc Clement
 */
abstract class IFDSEvaluationRunner {

    protected def analysisClass: IFDSAnalysisScheduler[_, _, _]

    protected def printAnalysisResults(analysis: IFDSAnalysis[_, _, _], ps: PropertyStore): Unit = ()

    protected def run(
        debug:                    Boolean,
        useL2:                    Boolean,
        evalSchedulingStrategies: Boolean,
        evaluationFile:           Option[File]
    ): Unit = {

        if (debug) {
            PropertyStore.updateDebug(true)
        }

        def evalProject(p: SomeProject): (Milliseconds, Statistics, Option[Object]) = {
            if (useL2) {
                p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
                    case None => Set(classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]])
                    case Some(requirements) =>
                        requirements + classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
                }
            } else {
                p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
                    case None               => Set(classOf[PrimitiveTACAIDomain])
                    case Some(requirements) => requirements + classOf[PrimitiveTACAIDomain]
                }
            }

            val ps = p.get(PropertyStoreKey)
            var analysisTime: Milliseconds = Milliseconds.None
            p.get(RTACallGraphKey)
            println("Start: " + new java.util.Date)
            org.opalj.util.gc()
            val analysis =
                time {
                    p.get(FPCFAnalysesManagerKey).runAll(analysisClass)._2
                }(t => analysisTime = t.toMilliseconds).collect {
                    case (_, a: IFDSAnalysis[_, _, _]) => a
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
            (
                analysisTime,
                analysis.statistics,
                additionalEvaluationResult(analysis)
            )
        }

        val p = Project(bytecode.RTJar)

        if (evalSchedulingStrategies) {
            val results = for {
                i <- 1 to IFDSEvaluationRunner.NUM_EXECUTIONS_EVAL_SCHEDULING_STRATEGIES
                strategy <- PKESequentialPropertyStore.Strategies
            } yield {
                println(s"Round: $i - $strategy")
                val strategyValue = ConfigValueFactory.fromAnyRef(strategy)
                val newConfig =
                    p.config.withValue(PKESequentialPropertyStore.TasksManagerKey, strategyValue)
                val evaluationResult = evalProject(Project.recreate(p, newConfig))
                org.opalj.util.gc()
                (i, strategy, evaluationResult._1, evaluationResult._2)
            }
            println(results.mkString("AllResults:\n\t", "\n\t", "\n"))
            if (evaluationFile.nonEmpty) {
                val pw = new PrintWriter(evaluationFile.get)
                PKESequentialPropertyStore.Strategies.foreach { strategy =>
                    val strategyResults = results.filter(_._2 == strategy)
                    val averageTime = strategyResults.map(_._3.timeSpan).sum / strategyResults.size
                    val (normalFlow, callToStart, exitToReturn, callToReturn) =
                        computeAverageStatistics(strategyResults.map(_._4))
                    pw.println(s"Strategy $strategy:")
                    pw.println(s"Average time: ${averageTime}ms")
                    pw.println(s"Average calls of normalFlow: $normalFlow")
                    pw.println(s"Average calls of callToStart: $callToStart")
                    pw.println(s"Average calls of exitToReturn: $exitToReturn")
                    pw.println(s"Average calls of callToReturn: $callToReturn")
                    pw.println()
                }
                pw.close()
            }
        } else {
            var times = Seq.empty[Milliseconds]
            var statistics = Seq.empty[Statistics]
            var additionalEvaluationResults = Seq.empty[Object]
            for {
                _ <- 1 to IFDSEvaluationRunner.NUM_EXECUTIONS
            } {
                val evaluationResult = evalProject(Project.recreate(p))
                val additionalEvaluationResult = evaluationResult._3
                times :+= evaluationResult._1
                statistics :+= evaluationResult._2
                if (additionalEvaluationResult.isDefined)
                    additionalEvaluationResults :+= additionalEvaluationResult.get
            }
            if (evaluationFile.nonEmpty) {
                val (normalFlow, callFlow, returnFlow, callToReturnFlow) = computeAverageStatistics(
                    statistics
                )
                val time = times.map(_.timeSpan).sum / times.size
                val pw = new PrintWriter(evaluationFile.get)
                pw.println(s"Average time: ${time}ms")
                pw.println(s"Average calls of normalFlow: $normalFlow")
                pw.println(s"Average calls of callFlow: $callFlow")
                pw.println(s"Average calls of returnFlow: $returnFlow")
                pw.println(s"Average calls of callToReturnFlow: $callToReturnFlow")
                if (additionalEvaluationResults.nonEmpty)
                    writeAdditionalEvaluationResultsToFile(pw, additionalEvaluationResults)
                pw.close()
            }
        }
    }

    protected def additionalEvaluationResult(analysis: IFDSAnalysis[_, _, _]): Option[Object] = None

    protected def writeAdditionalEvaluationResultsToFile(
        writer:                      PrintWriter,
        additionalEvaluationResults: Seq[Object]
    ): Unit = {}

    protected def canBeCalledFromOutside(
        method:        DeclaredMethod,
        propertyStore: PropertyStore
    ): Boolean =
        propertyStore(method, Callers.key) match {
            // This is the case, if the method may be called from outside the library.
            case FinalEP(_, p: Callers) => p.hasCallersWithUnknownContext
            case _ =>
                throw new IllegalStateException(
                    "call graph mut be computed before the analysis starts"
                )
        }

    private def computeAverageStatistics(statistics: Seq[Statistics]): (Int, Int, Int, Int) = {
        val length = statistics.length
        val normalFlow = statistics.map(_.normalFlow).sum / length
        val callFlow = statistics.map(_.callFlow).sum / length
        val returnFlow = statistics.map(_.returnFlow).sum / length
        val callToReturnFlow = statistics.map(_.callToReturnFlow).sum / length
        (normalFlow, callFlow, returnFlow, callToReturnFlow)
    }
}

object IFDSEvaluationRunner {
    var NUM_EXECUTIONS = 10
    var NUM_EXECUTIONS_EVAL_SCHEDULING_STRATEGIES = 2
}
