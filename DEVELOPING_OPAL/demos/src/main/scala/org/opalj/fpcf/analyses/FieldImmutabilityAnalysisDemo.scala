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
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.LazyL0FieldReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.br.fpcf.properties.FieldImmutability

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Calendar
import org.opalj.bytecode.JRELibraryFolder
import java.io.IOException

/**
 * Runs the EagerL0FieldImmutabilityAnalysis including analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object FieldImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "runs the EagerL0FieldImmutabilityAnalysis"

    override def description: String =
        "runs the EagerL0FieldImmutabilityAnalysis"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val result = analyze(project)
        BasicReport(result)
    }

    def analyze(project: Project[URL]): String = {

        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        analysesManager.project.get(RTACallGraphKey)
        time {
            propertyStore = analysesManager
                .runAll(
                    LazyL0FieldReferenceImmutabilityAnalysis,
                    LazyUnsoundPrematurelyReadFieldsAnalysis,
                    LazyL2PurityAnalysis_new,
                    EagerL0FieldImmutabilityAnalysis,
                    LazyLxClassImmutabilityAnalysis_new,
                    LazyLxTypeImmutabilityAnalysis_new,
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

        val allFieldsInProjectClassFiles = project.allProjectClassFiles.toIterator.flatMap { _.fields }.toSet

        val groupedResults = propertyStore.entities(FieldImmutability.key).
            filter(field ⇒ allFieldsInProjectClassFiles.contains(field.asInstanceOf[Field])).
            toTraversable.groupBy(_.toFinalEP.p)

        val order = (eps1: EPS[Entity, FieldImmutability], eps2: EPS[Entity, FieldImmutability]) ⇒
            eps1.e.toString < eps2.e.toString
        val mutableFields = groupedResults(MutableField).toSeq.sortWith(order)
        val shallowImmutableFields = groupedResults(ShallowImmutableField).toSeq.sortWith(order)
        val dependentImmutableFields = groupedResults(DependentImmutableField).toSeq.sortWith(order)
        val deepImmutableFields = groupedResults(DeepImmutableField).toSeq.sortWith(order)

        val output =
            s"""
             | Mutable Fields:
             | ${mutableFields.mkString(" | Mutable Field \n")}
             |
             | Shallow Immutable Fields:
             | ${shallowImmutableFields.mkString(" | Shallow Immutable Field \n")}
             |
             | Dependent Immutable Fields:
             | ${dependentImmutableFields.mkString(" | Dependent Immutable Field\n")}
             |
             | Deep Immutable Fields:
             | ${deepImmutableFields.mkString(" | Deep Immutable Field\n")}
             |
             |
             | mutable fields: ${mutableFields.size}
             | shallow immutable fields: ${shallowImmutableFields.size}
             | dependent immutable fields: ${dependentImmutableFields.size}
             | deep immutable fields: ${deepImmutableFields.size}
             |
             | count: ${mutableFields.size + shallowImmutableFields.size + dependentImmutableFields.size + deepImmutableFields.size}
             |
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
            case e: IOException ⇒ println(s"could not write file ${file.getName}"); throw e;
            case _: Throwable   ⇒
        }
        println("JRELibraryFolder: "+JRELibraryFolder)

        s"took : $analysisTime seconds"

    }
}
