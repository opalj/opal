/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.immutability.EagerTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableType
import org.opalj.br.fpcf.properties.immutability.MutableType
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TypeImmutability
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerTypeImmutabilityAnalysis as well as all analyses needed for improving the result
 *
 * @author Tobias Roth
 */
object TypeImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "Determines type immutability"

    override def description: String = "identifies transitively immutable types"

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
                    LazyL2PurityAnalysis,
                    LazyL2FieldAssignabilityAnalysis,
                    LazyFieldImmutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    EagerTypeImmutabilityAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyFieldLocalityAnalysis
                )
                ._1

            propertyStore.waitOnPhaseCompletion();

        } { t => analysisTime = t.toSeconds }

        val allProjectClassTypes = project.allProjectClassFiles.map(_.thisType).toSet

        val groupedResults = propertyStore.entities(TypeImmutability.key).filter(x =>
            allProjectClassTypes.contains(x.asInstanceOf[ObjectType])
        ).iterator.to(Iterable).groupBy(_.e)

        val order = (eps1: EPS[Entity, TypeImmutability], eps2: EPS[Entity, TypeImmutability]) =>
            eps1.e.toString < eps2.e.toString

        val mutableTypes = groupedResults(MutableType).toSeq.sortWith(order)

        val nonTransitivelyImmutableTypes = groupedResults(NonTransitivelyImmutableType).toSeq.sortWith(order)

        val dependentlyImmutableTypes = groupedResults(DependentlyImmutableType).toSeq.sortWith(order)

        val transitivelyImmutableTypes = groupedResults(TransitivelyImmutableType).toSeq.sortWith(order)

        s"""
           |
           | Mutable Types: ${mutableTypes.size}
           | Non-Transitively Immutable Types: ${nonTransitivelyImmutableTypes.size}
           | Dependently Immutable Types: ${dependentlyImmutableTypes.size}
           | Transitively Immutable Types: ${transitivelyImmutableTypes.size}
           |
           | total fields: ${
                mutableTypes.size + nonTransitivelyImmutableTypes.size + dependentlyImmutableTypes.size +
                    transitivelyImmutableTypes.size
            }
           | took : $analysisTime seconds
           |""".stripMargin
    }
}
