/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import java.util.Calendar
import org.opalj.br.Field
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL3FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL1ClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL1TypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.EagerL0FieldReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import java.io.IOException
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
import org.opalj.br.fpcf.properties.FieldReferenceImmutability

/**
 * Runs the EagerL0ReferenceImmutabilityAnalysis including all analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object FieldReferenceImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

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
        /*import org.opalj.br.fpcf.properties.ImmutableFieldReference
        import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicFieldReference
        import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeFieldReference
        import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeFieldReference */
        import org.opalj.br.fpcf.properties.ImmutableFieldReference
        import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicFieldReference
        import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeFieldReference
        import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeFieldReference
        import org.opalj.br.fpcf.properties.MutableFieldReference

        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None

        val analysesManager = project.get(FPCFAnalysesManagerKey)
        analysesManager.project.get(RTACallGraphKey)
        time {
            propertyStore = analysesManager
                .runAll(
                    EagerL0FieldReferenceImmutabilityAnalysis,
                    LazyL3FieldImmutabilityAnalysis,
                    LazyL1ClassImmutabilityAnalysis,
                    LazyL1TypeImmutabilityAnalysis,
                    LazyUnsoundPrematurelyReadFieldsAnalysis,
                    LazyL2PurityAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyFieldLocalityAnalysis
                )
                ._1

            propertyStore.waitOnPhaseCompletion()

        } { t ⇒
            analysisTime = t.toSeconds
        }

        val allFieldsInProjectClassFiles = project.allProjectClassFiles.toIterator.flatMap { _.fields }.toSet

        val groupedResults = propertyStore.entities(FieldReferenceImmutability.key).
            filter(field ⇒ allFieldsInProjectClassFiles.contains(field.asInstanceOf[Field])).
            toTraversable.groupBy(_.toFinalEP.p)

        val order = (eps1: EPS[Entity, FieldReferenceImmutability], eps2: EPS[Entity, FieldReferenceImmutability]) ⇒
            eps1.e.toString < eps2.e.toString
        val mutableReferences = groupedResults(MutableFieldReference).toSeq.sortWith(order)
        val notThreadSafeLazyInitializedFieldReferences = groupedResults(LazyInitializedNotThreadSafeFieldReference).
            toSeq.sortWith(order)
        val lazyInitializedReferencesNotThreadSafeButDeterministic =
            groupedResults(LazyInitializedNotThreadSafeButDeterministicFieldReference).toSeq.sortWith(order)
        val threadSafeLazyInitializedFieldReferences = groupedResults(LazyInitializedThreadSafeFieldReference).toSeq.
            sortWith(order)
        val immutableReferences = groupedResults(ImmutableFieldReference).toSeq.sortWith(order)
        val output = s"""
                                          | Mutable Fiel References:
                                          | ${mutableReferences.mkString(" | Mutable Field Reference \n")}
                                          |  lazy initialized not thread deterministic references:
                                          | ${notThreadSafeLazyInitializedFieldReferences.mkString(" | Not Thread \n")}
                                          |
                                          | lazy initialized not thread safe but deterministic references:
                                          | ${lazyInitializedReferencesNotThreadSafeButDeterministic.mkString(" | Lazy initialized not thread safe but deterministic field reference \n")}
                                          |
                                          | lazy initialized thread safe references:
                                          | ${threadSafeLazyInitializedFieldReferences.mkString(" | Lazy initialized thread safe field reference\n")}
                                          |
                                          | Immutable Field References:
                                          | ${immutableReferences.mkString(" | Immutable Field Reference \n")}
                                          |
                                          | mutable References: ${mutableReferences.size}
                                          | lazy initialized references not thread safe or deterministic:
                                          |              ${notThreadSafeLazyInitializedFieldReferences.size}
                                          | lazy initialized references not thread safe but deterministic:
                                          |             ${lazyInitializedReferencesNotThreadSafeButDeterministic.size}
                                          | lazy initialized thread safe references: ${threadSafeLazyInitializedFieldReferences.size}
                                          | immutable References: ${immutableReferences.size}
                                          |
                                          | took : $analysisTime seconds
                                          |""".stripMargin

        val file = new File(
            s"${Calendar.getInstance().formatted("dd_MM_yyyy_hh_mm_ss")}.txt"
        )

        try {

            if (!file.exists())
                file.createNewFile()
            val bw = new BufferedWriter(new FileWriter(file))
            bw.write(output)
            bw.close()
        } catch {
            case e: IOException ⇒
                println(s"Could not write file: ${file.getName}"); throw e
            case _: Throwable ⇒
        }

        s"took : $analysisTime seconds"

    }
}
