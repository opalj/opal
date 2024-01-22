/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL
import scala.collection.immutable.SortedSet

import org.opalj.ai.domain
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.Field
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableField
import org.opalj.br.fpcf.properties.immutability.FieldImmutability
import org.opalj.br.fpcf.properties.immutability.MutableField
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableField
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerFieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerL0FieldImmutabilityAnalysis including all analyses needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object FieldImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "Determines field immutability"

    override def description: String = "Identifies (non-)transitively immutable fields"

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

        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>
            Set[Class[_ <: AnyRef]](classOf[domain.l2.DefaultPerformInvocationsDomainWithCFG[URL]])
        }

        time {
            propertyStore = analysesManager
                .runAll(
                    LazyL2FieldAssignabilityAnalysis,
                    LazyL2PurityAnalysis,
                    EagerFieldImmutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyFieldLocalityAnalysis,
                    EagerFieldAccessInformationAnalysis
                )
                ._1
            propertyStore.waitOnPhaseCompletion();
        } { t => analysisTime = t.toSeconds }

        val allFieldsInProjectClassFiles = project.allProjectClassFiles.iterator.flatMap { _.fields }.toSet

        val groupedResults = propertyStore
            .entities(FieldImmutability.key)
            .filter(field => allFieldsInProjectClassFiles.contains(field.e.asInstanceOf[Field]))
            .iterator.to(Iterable)
            .groupBy(_.toFinalEP.p)

        val order = (eps1: EPS[Entity, FieldImmutability], eps2: EPS[Entity, FieldImmutability]) =>
            eps1.e.toString < eps2.e.toString
        val mutableFields = groupedResults.getOrElse(MutableField, Seq.empty).toSeq.sortWith(order)
        val nonTransitivelyImmutableFields =
            groupedResults.getOrElse(NonTransitivelyImmutableField, Seq.empty).toSeq.sortWith(order)
        val dependentImmutableFields =
            groupedResults.getOrElse(DependentlyImmutableField(SortedSet.empty[String]), Seq.empty).toSeq.sortWith(order)
        val transitivelyImmutableFields =
            groupedResults.getOrElse(TransitivelyImmutableField, Seq.empty).toSeq.sortWith(order)

        s"""
           |
           | Mutable Fields: ${mutableFields.size}
           | Non Transitively Immutable Fields: ${nonTransitivelyImmutableFields.size}
           | Dependent Immutable Fields: ${dependentImmutableFields.size}
           | Transitively Immutable Fields: ${transitivelyImmutableFields.size}
           |
           | total fields: ${
                mutableFields.size + nonTransitivelyImmutableFields.size +
                    dependentImmutableFields.size + transitivelyImmutableFields.size
            }
           |
           | took : $analysisTime seconds
           |
           | level: ${project.getProjectInformationKeyInitializationData(AIDomainFactoryKey)}
           |propertyStore: ${propertyStore.getClass}
           |""".stripMargin
    }
}
