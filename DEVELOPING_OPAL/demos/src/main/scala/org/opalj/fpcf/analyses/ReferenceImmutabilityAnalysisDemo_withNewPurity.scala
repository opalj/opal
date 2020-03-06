/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io._
import java.net.URL
import java.util.Calendar

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.br.fpcf.properties.LazyInitializedReference
import org.opalj.br.fpcf.properties.MutableReference
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerL0ReferenceImmutabilityAnalysis including all analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object ReferenceImmutabilityAnalysisDemo_withNewPurity extends ProjectAnalysisApplication {

    override def title: String = "runs the EagerL0ReferenceImmutabilityAnalysis"

    override def description: String =
        "runs the EagerL0ReferenceImmutabilityAnalysis"

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
                    EagerL0ReferenceImmutabilityAnalysis,
                    LazyUnsoundPrematurelyReadFieldsAnalysis,
                    LazyL2PurityAnalysis_new,
                    LazyInterProceduralEscapeAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyFieldLocalityAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyL1FieldMutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis
                )
                ._1
            propertyStore.waitOnPhaseCompletion();
        } { t ⇒
            analysisTime = t.toSeconds
        }
        var sb: StringBuilder = new StringBuilder()
        sb = sb.append("Mutable References: \n")
        val mutableReferences = propertyStore.finalEntities(MutableReference).toList
        sb = sb.append(
            mutableReferences.map(x ⇒ x.toString+"\n").toString()
        )

        sb = sb.append("\n Lazy Initialized Reference: \n")
        val lazyInitializedReferences = propertyStore
            .finalEntities(LazyInitializedReference)
            .toList
        sb = sb.append(
            lazyInitializedReferences
                .map(x ⇒ x.toString+"\n")
                .toString()
        )

        /**
         * .toList
         * .toString() + "\n" +*
         */
        sb = sb.append("\nImmutable References: \n")
        val immutableReferences = propertyStore.finalEntities(ImmutableReference).toList
        sb = sb.append(
            immutableReferences.map(x ⇒ x.toString+"\n").toString()
        )
        sb.append("\n\n")
        sb.append(" took : "+analysisTime+" seconds\n")
        sb.append(
            s""" mutable References: ${mutableReferences.size}
            | lazy initialized References: ${lazyInitializedReferences.size}
            | immutable References: ${immutableReferences.size}
            |""".stripMargin

        )

        val dateString: String = Calendar.getInstance().get(Calendar.MILLISECOND).toString
        val file = new File("C:/MA/results/refImm"+dateString+".txt")
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(sb.toString())
        bw.close()

        " took : "+analysisTime+" seconds"
    }
}
