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
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.MutableType_new
import org.opalj.br.fpcf.properties.ShallowImmutableType
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.LazyL0FieldReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new
import java.io.IOException

/**
 * Runs the EagerLxClassImmutabilityAnalysis_new as well as analysis needed for improving the result
 *
 * @author Tobias Peter Roth
 */
object TypeImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "run EagerLxTypeImmutabilityAnalysis_new"

    override def description: String = "run EagerLxTypeImmutabilityAnalysis_new"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val result = analyze(project)
        BasicReport(result)
    }

    def analyze(project: Project[URL]): String = {
        import org.opalj.br.fpcf.properties.TypeImmutability_new
        import org.opalj.fpcf.EPS
        import org.opalj.fpcf.Entity
        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        analysesManager.project.get(RTACallGraphKey)

        time {
            propertyStore = analysesManager
                .runAll(
                    LazyUnsoundPrematurelyReadFieldsAnalysis,
                    LazyL2PurityAnalysis_new,
                    LazyL0FieldReferenceImmutabilityAnalysis,
                    LazyL0FieldImmutabilityAnalysis,
                    LazyLxClassImmutabilityAnalysis_new,
                    EagerLxTypeImmutabilityAnalysis_new,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyFieldLocalityAnalysis
                )
                ._1
            propertyStore.waitOnPhaseCompletion();
        } { t ⇒
            analysisTime = t.toSeconds
        }

        val allProjectClassTypes = project.allProjectClassFiles.map(_.thisType).toSet

        val groupedResults = propertyStore.entities(TypeImmutability_new.key).
            filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType])).toTraversable.groupBy(_.e)

        val order = (eps1: EPS[Entity, TypeImmutability_new], eps2: EPS[Entity, TypeImmutability_new]) ⇒
            eps1.e.toString < eps2.e.toString
        val mutableTypes = groupedResults(MutableType_new).toSeq.sortWith(order)
        val shallowImmutableTypes = groupedResults(ShallowImmutableType).toSeq.sortWith(order)
        val dependentImmutableTypes = groupedResults(DependentImmutableType).toSeq.sortWith(order)
        val deepImmutableTypes = groupedResults(DeepImmutableType).toSeq.sortWith(order)

        val output =
            s"""
           | Mutable Types:
           | ${mutableTypes.mkString(" | Mutable Type\n")}
           |
           | Shallow Immutable Types:
           | ${shallowImmutableTypes.mkString(" | Shallow Immutable Type\n")}
           |
           | Dependent Immutable Types:
           | ${dependentImmutableTypes.mkString(" | Dependent Immutable Type\n")}
           |
           | Deep Immutable Types:
           | ${deepImmutableTypes.mkString(" | Deep Immutable Type\n")}
           |
           |
           | mutable types: ${mutableTypes.size}
           | shallow immutable types: ${shallowImmutableTypes.size}
           | dependent immutable types: ${dependentImmutableTypes.size}
           | deep immutable types: ${deepImmutableTypes.size}
           |
           | sum: ${mutableTypes.size + shallowImmutableTypes.size + dependentImmutableTypes.size + deepImmutableTypes.size}
           | took : $analysisTime seconds
           |""".stripMargin

        val file = new File(
            s"${Calendar.getInstance().formatted("dd_MM_yyyy_hh_mm_ss")}.txt"
        )

        try {
            val bw = new BufferedWriter(new FileWriter(file))
            bw.write(output)
            bw.close()
        } catch {
            case e: IOException ⇒
                println(s"could not write file: ${file.getName}"); throw e
            case _: Throwable ⇒
        }

        s" took : $analysisTime seconds"
    }
}
