/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
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
import org.opalj.br.fpcf.analyses.immutability.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableClass
import org.opalj.br.fpcf.properties.immutability.MutableClass
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableClass
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
 * Runs the EagerL1ClassImmutabilityAnalysis as well as analyses needed for improving the result.
 *
 * @author Tobias Roth
 */
object ClassImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "Determines class immutability"

    override def description: String = "Identifies classes that are (non-)transitively immutable"

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
                    LazyTypeImmutabilityAnalysis,
                    EagerClassImmutabilityAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyFieldLocalityAnalysis
                )
                ._1

            propertyStore.waitOnPhaseCompletion()

        } { t =>
            analysisTime = t.toSeconds
        }

        val allProjectClassTypes = project.allProjectClassFiles.map(_.thisType).toSet

        val groupedResults = propertyStore.entities(ClassImmutability.key).
            filter(x => allProjectClassTypes.contains(x.asInstanceOf[ObjectType])).iterator.to(Iterable).groupBy(_.e)

        val order = (eps1: EPS[Entity, ClassImmutability], eps2: EPS[Entity, ClassImmutability]) =>
            eps1.e.toString < eps2.e.toString

        val mutableClasses =
            groupedResults(MutableClass).toSeq.sortWith(order)

        val nonTransitivelyImmutableClasses =
            groupedResults(NonTransitivelyImmutableClass).toSeq.sortWith(order)

        val dependentlyImmutableClasses =
            groupedResults(DependentlyImmutableClass).toSeq.sortWith(order)

        val transitivelyImmutableClassesOrInterfaces = groupedResults(TransitivelyImmutableClass).toSeq.sortWith(order)

        val allInterfaces = project.allProjectClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet

        val transitivelyImmutableInterfaces = transitivelyImmutableClassesOrInterfaces
            .filter(eps => allInterfaces.contains(eps.asInstanceOf[ObjectType])).sortWith(order)

        val transitivelyImmutableClasses = transitivelyImmutableClassesOrInterfaces
            .filter(eps => !allInterfaces.contains(eps.asInstanceOf[ObjectType])).sortWith(order)

        s"""
             |
             | Mutable Classes: ${mutableClasses.size}
             | Non Transitively Immutable Classes: ${nonTransitivelyImmutableClasses.size}
             | Dependently Immutable Classes: ${dependentlyImmutableClasses.size}
             | Transitively Immutable Classes: ${transitivelyImmutableClasses.size}
             | Transitively Immutable Interfaces: ${transitivelyImmutableInterfaces.size}
             | Transitively Immutables Classes or Interfaces: ${transitivelyImmutableClassesOrInterfaces.size}
             |
             | total classes or interfaces: ${
            mutableClasses.size + nonTransitivelyImmutableClasses.size + dependentlyImmutableClasses.size +
                transitivelyImmutableClasses.size + transitivelyImmutableInterfaces.size
        }
             | analysis took : $analysisTime seconds
             |"""".stripMargin
    }
}
