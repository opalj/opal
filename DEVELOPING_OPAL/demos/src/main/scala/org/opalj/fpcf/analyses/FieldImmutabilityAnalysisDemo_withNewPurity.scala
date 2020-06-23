/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
import java.net.URL
import java.util.Calendar

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
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.reference.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerL0FieldImmutabilityAnalysis including analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object FieldImmutabilityAnalysisDemo_withNewPurity extends ProjectAnalysisApplication {

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
                    LazyL0ReferenceImmutabilityAnalysis,
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
        val sb: StringBuilder = new StringBuilder
        sb.append("Mutable Fields: \n")
        val allfieldsInProjectClassFiles = project.allProjectClassFiles.toIterator.flatMap { _.fields }
            //.filter(f ⇒ (!f.isTransient && !f.isSyn)) // for ReImComparison
            .toSet
        val mutableFields = propertyStore
            .finalEntities(MutableField)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList

        sb.append(
            mutableFields
                .map(x ⇒ x.toString+" |Mutable Field\n")
                .toString()
        )
        sb.append("\nShallow Immutable Fields: \n")
        val shallowImmutableFields = propertyStore
            .finalEntities(ShallowImmutableField)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList
        sb.append(
            shallowImmutableFields
                .map(x ⇒ x.toString+" |Shallow Immutable Field\n")
                .toString()
        )
        sb.append("\nDependet Immutable Fields: \n")
        val dependentImmutableFields = propertyStore
            .finalEntities(DependentImmutableField)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList
        sb.append(
            dependentImmutableFields
                .map(x ⇒ x.toString+" |Dependent Immutable Field\n")
                .toString()
        )
        sb.append("Deep Immutable Fields: ")
        val deepImmutableFields = propertyStore
            .finalEntities(DeepImmutableField)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList
        sb.append(
            deepImmutableFields
                .map(x ⇒ x.toString+" |Deep Immutable Field\n")
                .toString
        )
        sb.append("\n\n")
        sb.append(
            s""" mutable fields: ${mutableFields.size}
         | shallow immutable fields: ${shallowImmutableFields.size}
         | dependent immutable fields: ${dependentImmutableFields.size}
         | deep immutable fields: ${deepImmutableFields.size}
         | 
         | count: ${mutableFields.size + shallowImmutableFields.size + dependentImmutableFields.size + deepImmutableFields.size}
         | 
         | took : $analysisTime seconds
         |""".stripMargin
        )
        val calendar = Calendar.getInstance()
        val file = new File(
            s"C:/MA/results/fieldImm_withNewPurity_${calendar.get(Calendar.YEAR)}_"+
                s"${calendar.get(Calendar.MONTH)}_${calendar.get(Calendar.DAY_OF_MONTH)}_"+
                s"${calendar.get(Calendar.HOUR_OF_DAY)}_${calendar.get(Calendar.MINUTE)}_"+
                s"${calendar.get(Calendar.MILLISECOND)}.txt"
        )

        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(sb.toString())
        bw.close()

        s"""
       | took : $analysisTime seconds
       |""".stripMargin
    }
}
