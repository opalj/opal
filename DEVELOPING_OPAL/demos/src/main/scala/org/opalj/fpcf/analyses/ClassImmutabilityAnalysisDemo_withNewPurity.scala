/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.util.Calendar

import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
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
import org.opalj.tac.fpcf.analyses.immutability.EagerLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.reference.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerLxClassImmutabilityAnalysis_new as well as analysis needed for improving the result
 *
 * @author Tobias Peter Roth
 */
object ClassImmutabilityAnalysisDemo_withNewPurity extends ProjectAnalysisApplication {

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
        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)

        analysesManager.project.get(RTACallGraphKey)

        time {
            propertyStore = analysesManager
                .runAll(
                    LazyUnsoundPrematurelyReadFieldsAnalysis,
                    LazyL2PurityAnalysis_new,
                    LazyL0ReferenceImmutabilityAnalysis,
                    LazyL0FieldImmutabilityAnalysis,
                    LazyLxTypeImmutabilityAnalysis_new,
                    EagerLxClassImmutabilityAnalysis_new,
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
        val allProjectClassTypes = project.allProjectClassFiles.map(_.thisType).toSet

        val sb = new StringBuilder
        sb.append("Mutable Class: \n")
        val mutableClasses = propertyStore
            .finalEntities(MutableClass)
            .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
            .toList
        sb.append(
            mutableClasses
                .map(x ⇒ x.toString+" |Mutable Class\n")
        )
        sb.append("\nShallow Immutable Class:\n")
        val shallowImmutableClasses = propertyStore
            .finalEntities(ShallowImmutableClass)
            .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
            .toList
        sb.append(
            shallowImmutableClasses
                .map(x ⇒ x.toString+" |Shallow Immutable Class\n")
        )
        sb.append("\nDependent Immutable Class: \n")
        val dependentImmutableClasses = propertyStore
            .finalEntities(DependentImmutableClass)
            .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
            .toList
        sb.append(
            dependentImmutableClasses
                .map(x ⇒ x.toString+" |Dependent Immutable Class\n")
        )

        sb.append("\nDeep Immutable Class Classes:\n")
        val allInterfaces =
            project.allProjectClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet

        val deepImmutables = propertyStore
            .finalEntities(DeepImmutableClass)
            .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
            .toList
        val deepImmutableClassesInterfaces = deepImmutables
            .filter(x ⇒ x.isInstanceOf[ObjectType] && allInterfaces.contains(x.asInstanceOf[ObjectType]))
            .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
        val deepImmutableClasses =
            deepImmutables
                .filter(!deepImmutableClassesInterfaces.toSet.contains(_))
                .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
        sb.append(
            deepImmutableClasses.toList
                .map(x ⇒ x.toString+"  |Deep Immutable Class\n")
        )
        sb.append("\nDeep Immutable Class Classes: Interface\n")

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
                     | deep immutables: ${deepImmutables.size}
                     | 
                     | took : $analysisTime seconds
                     |"""".stripMargin)

        val calendar = Calendar.getInstance()
        val file = new File(
            s"C:/MA/results/classImm_withNewPurity_${calendar.get(Calendar.YEAR)}_"+
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
