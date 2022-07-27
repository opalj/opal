/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.analyses.ProjectAnalysisApplication

import java.util.Calendar
import java.io.IOException
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
import java.net.URL
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.field_assignability.EagerL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.Field
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized

/**
 * Runs the EagerL0FieldAssignabilityAnalysis including all analysis needed for improving the result.
 *
 * @author Tobias Roth
 */
object FieldAssignabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "determines the assignability of static and instce fields"

    override def description: String = "identifies non assignable fields"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        val result = analyze(project)
        BasicReport(result)
    }

    def analyze(project: Project[URL]): String = {

        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        project.get(RTACallGraphKey)

        time {
            propertyStore = analysesManager
                .runAll(
                    EagerL2FieldAssignabilityAnalysis,
                    LazyL0FieldImmutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis,
                    LazyL2PurityAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyFieldLocalityAnalysis
                )
                ._1

            propertyStore.waitOnPhaseCompletion()

        } { t =>
            analysisTime = t.toSeconds
        }

        val allFieldsInProjectClassFiles = project.allProjectClassFiles.iterator.flatMap { _.fields }.toSet

        val groupedResults = propertyStore.entities(FieldAssignability.key).
            filter(field => allFieldsInProjectClassFiles.contains(field.asInstanceOf[Field]))
            .iterator.to(Iterable).groupBy(_.toFinalEP.p)

        val order = (eps1: EPS[Entity, FieldAssignability], eps2: EPS[Entity, FieldAssignability]) =>
            eps1.e.toString < eps2.e.toString
        val assignableFields = groupedResults(Assignable).toSeq.sortWith(order)
        val notThreadSafeLazyInitializedFieldReferences = groupedResults(UnsafelyLazilyInitialized).
            toSeq.sortWith(order)
        val threadSafeLazyInitializedFieldReferences = groupedResults(LazilyInitialized).toSeq.
            sortWith(order)
        val immutableReferences = groupedResults(EffectivelyNonAssignable).toSeq.sortWith(order)
        val output =
            s"""
          | Assignable Field:
          | ${assignableFields.mkString(" | Assignable \n")}
          |
          |  Lazy Initialized Not Thread Safe Field:
          | ${notThreadSafeLazyInitializedFieldReferences.mkString(" | Not Thread Safe Lazy Initialization \n")}
          |
          | lazy Initialized Thread Safe Field:
          | ${threadSafeLazyInitializedFieldReferences.mkString(" | Lazy initialized thread safe field reference \n")}
          |
          | Effectively Non Assignable Field
          |
          | Non Assignable Field:
          | ${immutableReferences.mkString(" | Immutable Field Reference \n")}
          |
          |
          | Mutable References: ${assignableFields.size}
           Lazy Initialized References Not Thread : ${notThreadSafeLazyInitializedFieldReferences.size}
          | Lazy Initialized Thread Safe References: ${threadSafeLazyInitializedFieldReferences.size}
          | Immutable References: ${immutableReferences.size}
          |
          | sum: ${
                assignableFields.size + notThreadSafeLazyInitializedFieldReferences.size +
                    threadSafeLazyInitializedFieldReferences.size +
                    immutableReferences.size
            }
          | took : $analysisTime seconds
          |""".stripMargin

        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        val file = new File(s"""${day}_${month}_${year}_${hour}_${minute}_${seconds}.txt""")

        val bw = new BufferedWriter(new FileWriter(file))

        try {
            bw.write(output)
            bw.close()
        } catch {
            case e: IOException => println(
                s""" Could not write file: ${file.getName}
               | ${e.getMessage}
               |""".stripMargin
            )
        } finally {
            bw.close()
        }

        output
    }
}
