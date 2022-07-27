/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.util.Calendar

import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableType
import org.opalj.br.fpcf.properties.immutability.MutableType
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TypeImmutability
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerTypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.field_assignability.LazyL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import java.io.IOException
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Entity

/**
 * Runs the EagerL1TypeImmutabilityAnalysis as well as all analyses needed for improving the result
 *
 * @author Tobias Roth
 */
object TypeImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "determines the immutability of types respecting all possible subtypes"

    override def description: String = "identifies types that are transitively immutable "+
        "respecting all possible subtypes"

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
                    LazyL0FieldImmutabilityAnalysis,
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

        } { t =>
            analysisTime = t.toSeconds
        }

        val allProjectClassTypes = project.allProjectClassFiles.map(_.thisType).toSet

        val groupedResults = propertyStore.entities(TypeImmutability.key).
            filter(x => allProjectClassTypes.contains(x.asInstanceOf[ObjectType])).iterator.to(Iterable).groupBy(_.e)

        val order = (eps1: EPS[Entity, TypeImmutability], eps2: EPS[Entity, TypeImmutability]) =>
            eps1.e.toString < eps2.e.toString

        val mutableTypes = groupedResults(MutableType).toSeq.sortWith(order)

        val nonTransitivelyImmutableTypes = groupedResults(NonTransitivelyImmutableType).toSeq.sortWith(order)

        val dependentImmutableTypes = groupedResults(DependentlyImmutableType).toSeq.sortWith(order)

        val transitivelyImmutableTypes = groupedResults(TransitivelyImmutableType).toSeq.sortWith(order)

        val output =
            s"""
           | Mutable Types:
           | ${mutableTypes.mkString(" | Mutable Type\n")}
           |
           | Shallow Immutable Types:
           | ${nonTransitivelyImmutableTypes.mkString(" | Shallow Immutable Type\n")}
           |
           | Dependent Immutable Types:
           | ${dependentImmutableTypes.mkString(" | Dependent Immutable Type\n")}
           |
           | Transitively Immutable Types:
           | ${transitivelyImmutableTypes.mkString(" | Deep Immutable Type\n")}
           |
           |
           | Mutable Types: ${mutableTypes.size}
           | Non-Transitively Immutable Types: ${nonTransitivelyImmutableTypes.size}
           | Dependent Immutable Types: ${dependentImmutableTypes.size}
           | Transitively Immutable Types: ${transitivelyImmutableTypes.size}
           |
           | sum: ${
                mutableTypes.size + nonTransitivelyImmutableTypes.size + dependentImmutableTypes.size +
                    transitivelyImmutableTypes.size
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
