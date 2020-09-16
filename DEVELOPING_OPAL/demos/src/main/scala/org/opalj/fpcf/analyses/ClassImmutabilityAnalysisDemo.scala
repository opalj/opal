/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

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
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.LazyL0FieldReferenceImmutabilityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import java.io.IOException
import org.opalj.br.fpcf.properties.ClassImmutability_new

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

        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)

        analysesManager.project.get(RTACallGraphKey)

        time {
            propertyStore = analysesManager
                .runAll(
                    LazyUnsoundPrematurelyReadFieldsAnalysis,
                    LazyL2PurityAnalysis_new,
                    LazyL0FieldReferenceImmutabilityAnalysis,
                    LazyL0FieldImmutabilityAnalysis,
                    LazyLxTypeImmutabilityAnalysis_new,
                    EagerLxClassImmutabilityAnalysis_new,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyInterProceduralEscapeAnalysis //,
                //LazyReturnValueFreshnessAnalysis,
                //LazyFieldLocalityAnalysis
                )
                ._1
            propertyStore.waitOnPhaseCompletion();
        } { t ⇒
            analysisTime = t.toSeconds
        }
        val allProjectClassTypes = project.allProjectClassFiles.map(_.thisType).toSet

        val groupedResults = propertyStore.entities(ClassImmutability_new.key).
            filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType])).toTraversable.groupBy(_.e)

        val order = (eps1: EPS[Entity, ClassImmutability_new], eps2: EPS[Entity, ClassImmutability_new]) ⇒
            eps1.e.toString < eps2.e.toString
        val mutableClasses =
            groupedResults(MutableClass).toSeq.sortWith(order)
        val shallowImmutableClasses =
            groupedResults(ShallowImmutableClass).toSeq.sortWith(order)
        val dependentImmutableClasses =
            groupedResults(DependentImmutableClass).toSeq.sortWith(order)
        val deepImmutables = groupedResults(DeepImmutableClass)
        val allInterfaces =
            project.allProjectClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet
        val deepImmutableClassesInterfaces = deepImmutables
            .filter(eps ⇒ allInterfaces.contains(eps.asInstanceOf[ObjectType])).toSeq.sortWith(order)
        val deepImmutableClasses =
            deepImmutables
                .filter(eps ⇒ !allInterfaces.contains(eps.asInstanceOf[ObjectType])).toSeq.sortWith(order)

        val output =
            s"""
             | Mutable Classes:
             | ${mutableClasses.mkString(" |Mutable Class")}
             |
             | Shallow Immutable Classes:
             | ${shallowImmutableClasses.mkString(" |Shallow Immutable Class\n")}
             |
             | Dependent Immutable Classes:
             | ${dependentImmutableClasses.mkString(" |Dependent Immutable Class\n")}
             |
             | Deep Immutable Classes:
             | ${deepImmutableClasses.mkString(" | Deep Immutable Classes\n")}
             |
             | Deep Immutable Class Interfaces:
             | ${deepImmutableClassesInterfaces.mkString(" | Deep Immutable Classes Interfaces\n")}
             |
             |
             | mutable Classes: ${mutableClasses.size}
             | shallow immutable classes: ${shallowImmutableClasses.size}
             | dependent immutable classes: ${dependentImmutableClasses.size}
             | deep immutable classes: ${deepImmutableClasses.size}
             | deep immutable classes interfaces: ${deepImmutableClassesInterfaces.size}
             | deep immutables: ${deepImmutables.size}
             |
             | took : $analysisTime seconds
             |"""".stripMargin

        val file = new File(
            s"${Calendar.getInstance().formatted("dd_MM_yyyy_hh_mm_ss")}.txt"
        )
        try {
            val bw = new BufferedWriter(new FileWriter(file))
            bw.write(output)
            bw.close()
        } catch {
            case e: IOException ⇒
                println(s"Could not write the file: ${file.getName}"); throw e
            case _: Throwable ⇒
        }
        s" took : $analysisTime seconds"
    }
}
