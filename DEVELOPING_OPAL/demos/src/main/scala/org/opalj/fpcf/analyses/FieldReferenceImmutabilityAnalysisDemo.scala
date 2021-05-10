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
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.EagerL3FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import java.io.IOException
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
import org.opalj.br.fpcf.properties.FieldAssignability
import org.opalj.br.fpcf.properties.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.UnsafelyLazilyInitialized
import org.opalj.br.fpcf.properties.LazilyInitialized
import org.opalj.br.fpcf.properties.Assignable

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
        project.get(RTACallGraphKey)

        time {
            propertyStore = analysesManager
                .runAll(
                    EagerL3FieldAssignabilityAnalysis,
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

        val groupedResults = propertyStore.entities(FieldAssignability.key).
            filter(field ⇒ allFieldsInProjectClassFiles.contains(field.asInstanceOf[Field])).
            toTraversable.groupBy(_.toFinalEP.p)

        val order = (eps1: EPS[Entity, FieldAssignability], eps2: EPS[Entity, FieldAssignability]) ⇒
            eps1.e.toString < eps2.e.toString
        val mutableReferences = groupedResults(Assignable).toSeq.sortWith(order)
        val notThreadSafeLazyInitializedFieldReferences = groupedResults(UnsafelyLazilyInitialized).
            toSeq.sortWith(order)
        /* val lazyInitializedReferencesNotThreadSafeButDeterministic =
            groupedResults(LazyInitializedNotThreadSafeButDeterministicFieldReference).toSeq.sortWith(order)*/
        val threadSafeLazyInitializedFieldReferences = groupedResults(LazilyInitialized).toSeq.
            sortWith(order)
        val immutableReferences = groupedResults(EffectivelyNonAssignable).toSeq.sortWith(order)
        val output =
            s"""
          | Mutable Field References:
          | ${mutableReferences.mkString(" | Mutable Field Reference \n")}
          |
          |  Lazy Initialized Not Thread Safe Field References:
          | ${notThreadSafeLazyInitializedFieldReferences.mkString(" | Not Thread Safe Lazy Initialization \n")}
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
          | Lazy Initialized Thread Safe References: ${threadSafeLazyInitializedFieldReferences.size}
          | Immutable References: ${immutableReferences.size}
          |
          | sum: ${
                mutableReferences.size + notThreadSafeLazyInitializedFieldReferences.size +
                    threadSafeLazyInitializedFieldReferences.size +
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