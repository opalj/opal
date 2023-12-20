/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.br.Field
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerL2FieldAssignabilityAnalysis including all analysis needed for improving the result.
 *
 * @author Tobias Roth
 */
object FieldAssignabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "Determiens field Assignability the assignability of static and instce fields"

    override def description: String = "Identifies non assignable fields"

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
                    EagerFieldAccessInformationAnalysis,
                    EagerL2FieldAssignabilityAnalysis,
                    LazyFieldImmutabilityAnalysis,
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

        val groupedResults = propertyStore.entities(FieldAssignability.key)
            .filter(eps => allFieldsInProjectClassFiles.contains(eps.e.asInstanceOf[Field]))
            .iterator.to(Iterable).groupBy(_.toFinalEP.p).withDefaultValue(Seq.empty)

        val order = (eps1: EPS[Entity, FieldAssignability], eps2: EPS[Entity, FieldAssignability]) =>
            eps1.e.toString < eps2.e.toString
        val assignableFields = groupedResults(Assignable).toSeq.sortWith(order)
        val unsafelyLazilyInitializedFields = groupedResults(UnsafelyLazilyInitialized).toSeq.sortWith(order)
        val lazilyInitializedFields = groupedResults(LazilyInitialized).toSeq.sortWith(order)
        val EffectivelynonAssignableFields = groupedResults(EffectivelyNonAssignable).toSeq.sortWith(order)
        val NonAssignableFields = groupedResults(NonAssignable).toSeq.sortWith(order)

        s"""
          |
          | Assignable Fields: ${assignableFields.size}
          | Unsafely Lazily Initialized Fields : ${unsafelyLazilyInitializedFields.size}
          | Lazily Initialized Fields: ${lazilyInitializedFields.size}
          | Effectively Non Assignable Fields: ${EffectivelynonAssignableFields.size}
          | Non Assignable Fields: ${NonAssignableFields.size}
          |
          | total Fields: ${
            assignableFields.size + unsafelyLazilyInitializedFields.size + lazilyInitializedFields.size +
                EffectivelynonAssignableFields.size + NonAssignableFields.size
        }
          | took : $analysisTime seconds
          |""".stripMargin
    }
}
