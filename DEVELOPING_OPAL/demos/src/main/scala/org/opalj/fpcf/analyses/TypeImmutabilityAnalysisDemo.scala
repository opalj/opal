/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.util.Calendar

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.MutableType_new
import org.opalj.br.fpcf.properties.ShallowImmutableType
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis

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
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        analysesManager.project.get(RTACallGraphKey)
        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        time {
            propertyStore = analysesManager
                .runAll(
                    LazyUnsoundPrematurelyReadFieldsAnalysis,
                    LazyL2PurityAnalysis,
                    LazyL0ReferenceImmutabilityAnalysis,
                    LazyL0FieldImmutabilityAnalysis,
                    EagerLxTypeImmutabilityAnalysis_new,
                    LazyLxClassImmutabilityAnalysis_new,
                    LazyFieldLocalityAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    LazyL1FieldMutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis
                )
                ._1
            propertyStore.waitOnPhaseCompletion();
        } { t ⇒
            analysisTime = t.toSeconds
        }
        val sb: StringBuilder = new StringBuilder
        sb.append("\nMutableTypes: \n")
        val mutableTypes = propertyStore
            .finalEntities(MutableType_new)
            .toList
        sb.append(
            mutableTypes
                .map(x ⇒ x.toString+"  |Mutable Type\n")
                .toString
        )
        sb.append("\nShallow Immutable Types:\n")
        val shallowImmutableTypes = propertyStore.finalEntities(ShallowImmutableType).toList
        sb.append(shallowImmutableTypes.map(x ⇒ x.toString+" |Shallow Immutable Type\n"))
        sb.append("\nDependent Immutable Types: \n")
        val dependentImmutableTypes = propertyStore.finalEntities(DependentImmutableType).toList
        sb.append(
            dependentImmutableTypes.map(x ⇒ x.toString+" |Dependent Immutable Type\n")
        )
        sb.append("\nDeep Immutable Types:\n")
        val deepImmutableTypes = propertyStore.finalEntities(DeepImmutableType).toList
        sb.append(deepImmutableTypes.map(x ⇒ x.toString+"  |Deep Immutable Type\n"))
        sb.append(s"\nType immutability analysis took: $analysisTime on average")

        sb.append("\n\n")
        sb.append(
            s"""
          | mutable types: ${mutableTypes.size}
          | shallow immutable types: ${shallowImmutableTypes.size}
          | dependent immutable types: ${dependentImmutableTypes.size}
          | deep immutable types: ${deepImmutableTypes.size}
          |""".stripMargin
        )

        val dateString: String = Calendar.getInstance().get(Calendar.MILLISECOND).toString
        val file = new File("C:/MA/results/typeImm"+dateString+".txt")
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(sb.toString())
        bw.close()

        " took : "+analysisTime+" seconds"
    }
}
