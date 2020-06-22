/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.LazyInitializedReference
import org.opalj.br.fpcf.properties.MutableReference
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.util.PerformanceEvaluation.memory
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.reference.EagerL0ReferenceImmutabilityAnalysis

/**
 * Runs the EagerL0ReferenceImmutabilityAnalysis including all analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object ReferenceImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

  override def title: String = "runs the EagerL0ReferenceImmutabilityAnalysis"

  override def description: String =
    "runs the EagerL0ReferenceImmutabilityAnalysis"

  override def doAnalyze(
      project: Project[URL],
      parameters: Seq[String],
      isInterrupted: () => Boolean
  ): BasicReport = {
    val result = analyze(project)
    BasicReport(result)
  }

  def analyze(project: Project[URL]): String = {
    var memoryConsumption: Long = 0
    var propertyStore: PropertyStore = null
    var analysisTime: Seconds = Seconds.None
    memory {
      val analysesManager = project.get(FPCFAnalysesManagerKey)
      analysesManager.project.get(RTACallGraphKey)

      time {
        propertyStore = analysesManager
          .runAll(
            EagerL0ReferenceImmutabilityAnalysis,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyL2FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis
          )
          ._1
        propertyStore.waitOnPhaseCompletion()

      } { t =>
        analysisTime = t.toSeconds
      }
    } { mu =>
      memoryConsumption = mu
    }
    var sb: StringBuilder = new StringBuilder()
    sb = sb.append("Mutable References: \n")
    val mutableReferences = propertyStore.finalEntities(MutableReference).toList
    sb = sb.append(
      mutableReferences.mkString(", \n")
    )

    sb = sb.append("\n Lazy Initialized Reference: \n")
    val lazyInitializedReferences = propertyStore
      .finalEntities(LazyInitializedReference)
      .toList
    sb = sb.append(
      lazyInitializedReferences.mkString(", \n")
    )

    val immutableReferencesTrue = propertyStore.entities({ eps: SomeEPS =>
      eps.ub match {
        case ImmutableReference(true) => true
        case _                        => false
      }
    })
    val immutableReferencesFalse = propertyStore.entities({ eps: SomeEPS =>
      eps.ub match {
        case ImmutableReference(false) => true
        case _                         => false
      }
    })

    sb = sb.append(
      s"""
           | imm ref true: 
           |${immutableReferencesTrue.mkString(", \n")}
           | 
           |
           | imm ref false:
           | ${immutableReferencesFalse.mkString(", \n")}
           |""".stripMargin
    )

    sb.append(
      s""" 
            | mutable References: ${mutableReferences.size}
            | lazy initialized References: ${lazyInitializedReferences.size}
            | immutable References: ${}
            | 
            | took : $analysisTime seconds
            | needed: ${memoryConsumption / 1024 / 1024} MBytes        
            |     
            |""".stripMargin
    )

    /* val calendar = Calendar.getInstance()
    val file = new File(
      s"C:/MA/results/refImm_${calendar.get(Calendar.YEAR)}_" +
        s"${calendar.get(Calendar.MONTH)}_${calendar.get(Calendar.DAY_OF_MONTH)}_" +
        s"${calendar.get(Calendar.HOUR_OF_DAY)}_${calendar.get(Calendar.MINUTE)}_" +
        s"${calendar.get(Calendar.MILLISECOND)}.txt"
    )
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(sb.toString())
    bw.close() **/
    //s"""
    //    | took : $analysisTime seconds
    //    | needs : ${memoryConsumption / 1024 / 1024} MBytes
    //    |""".stripMargin
    sb.toString()
  }
}
