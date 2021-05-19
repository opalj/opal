/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Calendar
import java.io.IOException

import org.opalj.br.Field
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.TransitivelyImmutableField
import org.opalj.br.fpcf.properties.DependentlyImmutableField
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.NonTransitivelyImmutableField
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL1ClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL1TypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.LazyL3FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.ai.domain
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey

/**
 * Runs the EagerL0FieldImmutabilityAnalysis including all analyses needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object FieldImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "determines the immutability of (instance/static) fields"

    override def description: String =
        "identifies fields that are immutable in a deep or shallow way"

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

        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
            Set[Class[_ <: AnyRef]](classOf[domain.l2.DefaultPerformInvocationsDomainWithCFG[URL]])
        }

        time {
            propertyStore = analysesManager
                .runAll(
                    LazyL3FieldAssignabilityAnalysis,
                    LazyUnsoundPrematurelyReadFieldsAnalysis,
                    LazyL2PurityAnalysis,
                    EagerL0FieldImmutabilityAnalysis,
                    LazyL1ClassImmutabilityAnalysis,
                    LazyL1TypeImmutabilityAnalysis,
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

        val groupedResults = propertyStore
            .entities(FieldImmutability.key)
            .filter(field ⇒ allFieldsInProjectClassFiles.contains(field.e.asInstanceOf[Field]))
            .toTraversable
            .groupBy(_.toFinalEP.p)

        val order = (eps1: EPS[Entity, FieldImmutability], eps2: EPS[Entity, FieldImmutability]) ⇒
            eps1.e.toString < eps2.e.toString
        val mutableFields = groupedResults.getOrElse(MutableField, Seq.empty).toSeq.sortWith(order)
        val shallowImmutableFields =
            groupedResults.getOrElse(NonTransitivelyImmutableField, Seq.empty).toSeq.sortWith(order)
        val dependentImmutableFields =
            groupedResults.getOrElse(DependentlyImmutableField(Set.empty), Seq.empty).toSeq.sortWith(order)
        val deepImmutableFields =
            groupedResults.getOrElse(TransitivelyImmutableField, Seq.empty).toSeq.sortWith(order)

        val output =
            /*
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
             |*/
            s""" |
             | Mutable Fields: ${mutableFields.size}
             | Shallow Immutable Fields: ${shallowImmutableFields.size}
             | Dependent Immutable Fields: ${dependentImmutableFields.size}
             | Deep Immutable Fields: ${deepImmutableFields.size}
             |
             | sum: ${
                mutableFields.size + shallowImmutableFields.size +
                    dependentImmutableFields.size + deepImmutableFields.size
            }
             |
             | took : $analysisTime seconds
             |
             | level: ${project.getProjectInformationKeyInitializationData(AIDomainFactoryKey)}
             |propertyStore: ${propertyStore.getClass}
             |""".stripMargin

        import java.text.SimpleDateFormat

        val calender = Calendar.getInstance()
        calender.add(Calendar.ALL_STYLES, 1)
        val date = calender.getTime()
        val simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss")

        val file = new File(
            s"demo_${analysis.toString}_${simpleDateFormat.format(date)}.txt"
        )

        println(s"filepath: ${file.getAbsolutePath}")
        val bw = new BufferedWriter(new FileWriter(file))

        try {
            bw.write(output)
            bw.close()
        } catch {
            case e: IOException ⇒
                println(
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
