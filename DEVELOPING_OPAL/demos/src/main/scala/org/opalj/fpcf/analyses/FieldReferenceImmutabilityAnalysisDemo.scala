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
import org.opalj.br.fpcf.properties.ImmutableFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeFieldReference
import org.opalj.br.fpcf.properties.MutableFieldReference

/**
 * Runs the EagerL0FieldReferenceImmutabilityAnalysis including all analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object FieldReferenceImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "determines the immutability of (static/instance) field references"

    override def description: String =
        "identifies immutable field references"

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
        val output =
            s"""
          | Mutable Field References:
          | ${mutableReferences.mkString(" | Mutable Field Reference \n")}
          |
          |  Lazy Initialized Not Thread Safe Field References:
          | ${notThreadSafeLazyInitializedFieldReferences.mkString(" | Not Thread Safe Lazy Initialization \n")}
          |
          | Lazy Initialized Not Thread Safe But Deterministic References:
          | ${
                lazyInitializedReferencesNotThreadSafeButDeterministic.
                    mkString(" | Lazy Initialized Not Thread Safe But Deterministic Field Reference \n")
            }
          |
          | lazy Initialized Thread Safe References:
          | ${threadSafeLazyInitializedFieldReferences.mkString(" | Lazy initialized thread safe field reference \n")}
          |
          | Immutable Field References:
          | ${immutableReferences.mkString(" | Immutable Field Reference \n")}
          |
          |
          | Mutable References: ${mutableReferences.size}
           Lazy Initialized References Not Thread : ${notThreadSafeLazyInitializedFieldReferences.size}
          | Lazy Initialized References Not Thread Safe But Deterministic: ${
                lazyInitializedReferencesNotThreadSafeButDeterministic.size
            }
          | Lazy Initialized Thread Safe References: ${threadSafeLazyInitializedFieldReferences.size}
          | Immutable References: ${immutableReferences.size}
          |
          | sum: ${
                mutableReferences.size + notThreadSafeLazyInitializedFieldReferences.size +
                    lazyInitializedReferencesNotThreadSafeButDeterministic.size + threadSafeLazyInitializedFieldReferences.size +
                    immutableReferences.size
            }
          | took : $analysisTime seconds
          |""".stripMargin

        val file = new File(s"${Calendar.getInstance().formatted("dd_MM_yyyy_hh_mm_ss")}.txt")

        val bw = new BufferedWriter(new FileWriter(file))

        try {
            bw.write(output)
            bw.close()
        } catch {
            case e: IOException ⇒ println(
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
