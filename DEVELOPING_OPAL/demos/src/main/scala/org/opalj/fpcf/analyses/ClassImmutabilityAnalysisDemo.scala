/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.util.Calendar

import org.opalj.br.ObjectType

//import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.analyses.EagerLxClassImmutabilityAnalysis_new
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldImmutabilityAnalysis
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
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyLxTypeImmutabilityAnalysis_new
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

        val analysesManager = project.get(FPCFAnalysesManagerKey)

        analysesManager.project.get(RTACallGraphKey)

        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
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
                    LazyL1FieldMutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis
                )
                ._1
            propertyStore.waitOnPhaseCompletion();
        } { t ⇒
            analysisTime = t.toSeconds
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
        val shallowImmutableClasses = propertyStore.finalEntities(ShallowImmutableClass)
            .toList
        sb.append(
            shallowImmutableClasses
                .map(x ⇒ x.toString+" |Shallow Immutable Class\n")
        )
        sb.append("\nDependent Immutable Class: \n")
        val dependentImmutableClasses = propertyStore
            .finalEntities(DependentImmutableClass).toList
        sb.append(
            dependentImmutableClasses
                .map(x ⇒ x.toString+" |Dependent Immutable Class\n")
        )

        sb.append("\nDeep Immutable Class Classes:\n")
        val allInterfaces = project.allClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet
        val deepImmutableClasses = propertyStore
            .finalEntities(DeepImmutableClass).toList
            .filter(x ⇒ !x.isInstanceOf[ObjectType] || !allInterfaces.contains(x.asInstanceOf[ObjectType]))
        sb.append(
            deepImmutableClasses
                .toList
                .map(x ⇒ x.toString+"  |Deep Immutable Class\n")
        )
        sb.append("\nDeep Immutable Class Classes: Interface\n")
        val deepImmutableClassesInterfaces = propertyStore
            .finalEntities(DeepImmutableClass).toList
            .filter(x ⇒ x.isInstanceOf[ObjectType] && allInterfaces.contains(x.asInstanceOf[ObjectType]))
        sb.append(
            deepImmutableClassesInterfaces
                .map(x ⇒ x.toString+"  |Deep Immutable Class Interface\n")
        )
        sb.append("\n\n")
        sb.append("mutable Classes: "+mutableClasses.size+"\n")
        sb.append("shallow immutable classes: "+shallowImmutableClasses.size+"\n")
        sb.append("dependent immutable classes: "+dependentImmutableClasses.size+"\n")
        sb.append("deep immutable classes: "+deepImmutableClasses.size+"\n")
        sb.append("deep immutable classes interfaces: "+deepImmutableClassesInterfaces.size+"\n")

        val dateString: String = Calendar.getInstance().get(Calendar.MILLISECOND).toString
        val file = new File("C:/MA/results/classImm"+dateString+".txt")
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(sb.toString())
        bw.close()

        " took : "+analysisTime+" seconds"
    }
}
