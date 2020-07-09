/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.MutableReference
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import java.io._
import java.util.Calendar

import org.opalj.br.Field
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeOrNotDeterministicReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeReference
import org.opalj.fpcf
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.reference.EagerL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new

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
        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        analysesManager.project.get(RTACallGraphKey)

        time {
            propertyStore = analysesManager
                .runAll(
                    EagerL0ReferenceImmutabilityAnalysis,
                    LazyL0FieldImmutabilityAnalysis,
                    LazyLxClassImmutabilityAnalysis_new,
                    LazyLxTypeImmutabilityAnalysis_new,
                    LazyUnsoundPrematurelyReadFieldsAnalysis,
                    LazyL2PurityAnalysis_new,
                    LazyInterProceduralEscapeAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyFieldLocalityAnalysis,
                    LazyLxTypeImmutabilityAnalysis_new
                )
                ._1
            propertyStore.waitOnPhaseCompletion()

        } { t ⇒
            analysisTime = t.toSeconds
        }
        var sb: StringBuilder = new StringBuilder()
        val allfieldsInProjectClassFiles = project.allProjectClassFiles.toIterator.flatMap { _.fields }
            //.filter(f ⇒ (!f.isTransient && !f.isSyn)) // for ReImComparison
            .toSet
        sb = sb.append("Mutable References: \n")
        val mutableReferences = propertyStore
            .finalEntities(MutableReference)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList.sortWith((e1: fpcf.Entity, e2: fpcf.Entity) ⇒ e1.toString < e2.toString)
        sb = sb.append(
            mutableReferences.map(x ⇒ x.toString+"\n").toString()
        )

        val lazyInitializedReferencesThreadSafe = propertyStore
            .finalEntities(LazyInitializedThreadSafeReference).toList.sortWith((e1: fpcf.Entity, e2: fpcf.Entity) ⇒ e1.toString < e2.toString)

        val lazyInitializedReferencesNotThreadSafeButDeterministic = propertyStore.
            finalEntities(LazyInitializedNotThreadSafeButDeterministicReference).toList.sortWith((e1: fpcf.Entity, e2: fpcf.Entity) ⇒ e1.toString < e2.toString)

        val notThreadSafeOrNotDeterministicLazyInitialization = propertyStore.
            finalEntities(LazyInitializedNotThreadSafeOrNotDeterministicReference).toList.sortWith((e1: fpcf.Entity, e2: fpcf.Entity) ⇒ e1.toString < e2.toString)

        sb.append(
            s"""
               | lazy initialized thread safe references: ${lazyInitializedReferencesThreadSafe.mkString(",\n")}
               |
               | lazy initialized not thread safe but deterministic references: ${lazyInitializedReferencesNotThreadSafeButDeterministic.mkString(", \n")}
               |
               | lazy initialized not thread safe or not deterministic references: ${notThreadSafeOrNotDeterministicLazyInitialization.mkString(", \n")}
               |
               |""".stripMargin
        )

        val immutableReferences = propertyStore.entities(eps ⇒ //allfieldsInProjectClassFiles.contains(eps.e.asInstanceOf[Field]) &&
            eps.isFinal && (eps.asFinal.p match {
                case ImmutableReference(_) ⇒ true
                case _                     ⇒ false
            })).toList.sortWith((e1: fpcf.Entity, e2: fpcf.Entity) ⇒ e1.toString < e2.toString)
        sb = sb.append(
            immutableReferences.map(x ⇒ x.toString+"\n").mkString(", ")
        )

        sb.append(
            s""" 
         | mutable References: ${mutableReferences.size}
         | lazy initialized references not thread safe or deterministic: ${notThreadSafeOrNotDeterministicLazyInitialization.size}
         | lazy initialized references not thread safe but deterministic: ${lazyInitializedReferencesNotThreadSafeButDeterministic.size}
         | lazy initialized thread safe references: ${lazyInitializedReferencesThreadSafe.size}
         | immutable References: ${immutableReferences.size}
         | 
         | took : $analysisTime seconds   
         |     
         |""".stripMargin
        )

        val calendar = Calendar.getInstance()
        val file = new File(
            s"C:/MA/results/refImm_withNewPurity_${calendar.get(Calendar.YEAR)}_"+
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
