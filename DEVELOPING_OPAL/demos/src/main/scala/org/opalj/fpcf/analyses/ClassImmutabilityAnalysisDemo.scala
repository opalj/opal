/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.util.Calendar

import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.reference.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.util.PerformanceEvaluation.memory

//import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableClass
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.ShallowImmutableClass
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerLxClassImmutabilityAnalysis_new as well as analysis needed for improving the result
 *
 * @author Tobias Peter Roth
 */
object ClassImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "run EagerLxClassImmutabilityAnalysis_new"

    override def description: String = "run EagerLxClassImmutabilityAnalysis_new"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
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
                        LazyUnsoundPrematurelyReadFieldsAnalysis,
                        LazyL2PurityAnalysis,
                        LazyL0ReferenceImmutabilityAnalysis,
                        LazyL0FieldImmutabilityAnalysis,
                        LazyLxTypeImmutabilityAnalysis_new,
                        EagerLxClassImmutabilityAnalysis_new,
                        LazyStaticDataUsageAnalysis,
                        LazyL0CompileTimeConstancyAnalysis,
                        LazyInterProceduralEscapeAnalysis,
                        LazyReturnValueFreshnessAnalysis,
                        LazyFieldLocalityAnalysis,
                        LazyL2FieldMutabilityAnalysis,
                        LazyClassImmutabilityAnalysis,
                        LazyTypeImmutabilityAnalysis
                    )
                    ._1
                propertyStore.waitOnPhaseCompletion();
            } { t ⇒
                analysisTime = t.toSeconds
            }
        } { mu ⇒
            memoryConsumption = mu
        }

        val sb = new StringBuilder
        sb.append("Mutable Class: \n")
        val mutableClasses = propertyStore
            .finalEntities(MutableClass)
            .toList
        sb.append(
            mutableClasses
                .map(x ⇒ x.toString+" |Mutable Class\n")
        )
        sb.append("\nShallow Immutable Class:\n")
        val shallowImmutableClasses = propertyStore.finalEntities(ShallowImmutableClass).toList
        sb.append(
            shallowImmutableClasses
                .map(x ⇒ x.toString+" |Shallow Immutable Class\n")
        )
        sb.append("\nDependent Immutable Class: \n")
        val dependentImmutableClasses = propertyStore
            .finalEntities(DependentImmutableClass)
            .toList
        sb.append(
            dependentImmutableClasses
                .map(x ⇒ x.toString+" |Dependent Immutable Class\n")
        )

        sb.append("\nDeep Immutable Class Classes:\n")
        val allInterfaces = project.allClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet
        val deepImmutableClasses = propertyStore
            .finalEntities(DeepImmutableClass)
            .toList
            .filter(
                x ⇒ !x.isInstanceOf[ObjectType] || !allInterfaces.contains(x.asInstanceOf[ObjectType])
            )
        sb.append(
            deepImmutableClasses.toList
                .map(x ⇒ x.toString+"  |Deep Immutable Class\n")
        )
        sb.append("\nDeep Immutable Class Classes: Interface\n")
        val deepImmutableClassesInterfaces = propertyStore
            .finalEntities(DeepImmutableClass)
            .toList
            .filter(x ⇒ x.isInstanceOf[ObjectType] && allInterfaces.contains(x.asInstanceOf[ObjectType]))
        sb.append(
            deepImmutableClassesInterfaces
                .map(x ⇒ x.toString+"  |Deep Immutable Class Interface\n")
        )
        sb.append(s"""
          | mutable Classes: ${mutableClasses.size}
          | shallow immutable classes: ${shallowImmutableClasses.size}
          | dependent immutable classes: ${dependentImmutableClasses.size}
          | deep immutable classes: ${deepImmutableClasses.size}
          | deep immutable classes interfaces: ${deepImmutableClassesInterfaces.size}
          | 
          | took : $analysisTime seconds
          | needs : ${memoryConsumption / 1024 / 1024} MBytes
          |"""".stripMargin)

        val calendar = Calendar.getInstance()
        val file = new File(
            s"C:/MA/results/classImm_${calendar.get(Calendar.YEAR)}_"+
                s"${calendar.get(Calendar.MONTH)}_${calendar.get(Calendar.DAY_OF_MONTH)}_"+
                s"${calendar.get(Calendar.HOUR_OF_DAY)}_${calendar.get(Calendar.MINUTE)}_"+
                s"${calendar.get(Calendar.MILLISECOND)}.txt"
        )
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(sb.toString())
        bw.close()

        s"""
         | took : $analysisTime seconds
         | needs : ${memoryConsumption / 1024 / 1024} MBytes
         |""".stripMargin
    }
}
