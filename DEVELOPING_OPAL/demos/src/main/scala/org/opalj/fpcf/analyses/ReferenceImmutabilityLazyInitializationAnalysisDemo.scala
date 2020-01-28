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
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.LazyInitialization
import org.opalj.br.fpcf.properties.NoLazyInitialization
import org.opalj.br.fpcf.properties.NotThreadSafeLazyInitialization
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerL0ReferenceImmutabilityLazyInitializationAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerL0ReferenceImmutabilityAnalysis including all analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object ReferenceImmutabilityLazyInitializationAnalysisDemo extends ProjectAnalysisApplication {

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
                    EagerL0ReferenceImmutabilityLazyInitializationAnalysis,
                    LazyL0ReferenceImmutabilityAnalysis,
                    LazyL2FieldMutabilityAnalysis,
                    LazyUnsoundPrematurelyReadFieldsAnalysis,
                    LazyL2PurityAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyTypeImmutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyFieldLocalityAnalysis,
                    LazyReturnValueFreshnessAnalysis
                )
                ._1
            propertyStore.waitOnPhaseCompletion();
        } { t ⇒
            analysisTime = t.toSeconds
        }
        /*def printResult(string: String, property: Property)(
            implicit
            propertyStore: PropertyStore
        ): Unit = {} **/
        val sb: StringBuilder = new StringBuilder()

        sb.append("Not initialized References: \n")
        sb.append(
            propertyStore
                .finalEntities(NoLazyInitialization)
                .toList
                .map(x ⇒ x.toString+"\n")
                .toString()
        )
        sb.append("\nNot threadsafe Lazy Initialization: \n")
        sb.append(
            propertyStore
                .finalEntities(NotThreadSafeLazyInitialization)
                .toList
                .map(x ⇒ x.toString+"\n")
                .toString()
        )
        sb.append("\nLazy Initialization: \n")
        sb.append(
            propertyStore
                .finalEntities(LazyInitialization)
                .toList
                .map(x ⇒ x.toString+"\n")
                .toString()
        )

        /**
         * "Not lazy initialized References: " + propertyStore
         * .finalEntities(NoLazyInitialization)
         * .toList
         * .toString() + "\n" +
         * "Not thread safe lazy initialization: " + propertyStore
         * .finalEntities(NotThreadSafeLazyInitialization)
         * .toList
         * .toString() + "\n" +
         * "Lazy Initialization: " + propertyStore
         * .finalEntities(LazyInitialization)
         * .toList
         * .toString()*
         */
        val dateString: String = Calendar.getInstance().get(Calendar.MILLISECOND).toString
        val file = new File("C:/MA/results/refDCLImm"+dateString+".txt")
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(sb.toString())
        bw.close()

        " took : "+analysisTime+" seconds"
        s"took $analysisTime seconds "
    }
}
