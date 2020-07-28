/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.util.Calendar

import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.MutableType_new
import org.opalj.br.fpcf.properties.ShallowImmutableType
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.reference.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new

/**
 * Runs the EagerLxClassImmutabilityAnalysis_new as well as analysis needed for improving the result
 *
 * @author Tobias Peter Roth
 */
object TypeImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "run EagerLxTypeImmutabilityAnalysis_new"

    override def description: String = "run EagerLxTypeImmutabilityAnalysis_new"

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
                    LazyLxClassImmutabilityAnalysis_new,
                    EagerLxTypeImmutabilityAnalysis_new,
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
        val sb: StringBuilder = new StringBuilder
        val allProjectClassFilesIterator = project.allProjectClassFiles
        val types =
            allProjectClassFilesIterator.filter(_.thisType ne ObjectType.Object).map(_.thisType).toSet
        sb.append("\nMutableTypes: \n")
        val mutableTypes = propertyStore
            .finalEntities(MutableType_new)
            .filter({ x ⇒
                types.contains(x.asInstanceOf[ObjectType])
            })
            .toList
        sb.append(
            mutableTypes
                .map(x ⇒ x.toString+"  |Mutable Type\n")
                .toString
        )
        sb.append("\nShallow Immutable Types:\n")
        val shallowImmutableTypes = propertyStore
            .finalEntities(ShallowImmutableType)
            .filter({ x ⇒
                types.contains(x.asInstanceOf[ObjectType])
            })
            .toList
        sb.append(shallowImmutableTypes.map(x ⇒ x.toString+" |Shallow Immutable Type\n"))
        sb.append("\nDependent Immutable Types: \n")
        val dependentImmutableTypes = propertyStore
            .finalEntities(DependentImmutableType)
            .filter({ x ⇒
                types.contains(x.asInstanceOf[ObjectType])
            })
            .toList
        sb.append(
            dependentImmutableTypes.map(x ⇒ x.toString+" |Dependent Immutable Type\n")
        )
        sb.append("\nDeep Immutable Types:\n")
        val deepImmutableTypes = propertyStore
            .finalEntities(DeepImmutableType)
            .filter({ x ⇒
                types.contains(x.asInstanceOf[ObjectType])
            })
            .toList
        sb.append(deepImmutableTypes.map(x ⇒ x.toString+"  |Deep Immutable Type\n"))
        sb.append(s"\nType immutability analysis took: $analysisTime on average")

        sb.append("\n\n")
        sb.append(
            s"""
         | mutable types: ${mutableTypes.size}
         | shallow immutable types: ${shallowImmutableTypes.size}
         | dependent immutable types: ${dependentImmutableTypes.size}
         | deep immutable types: ${deepImmutableTypes.size}
         | 
         | sum: ${mutableTypes.size + shallowImmutableTypes.size + dependentImmutableTypes.size + deepImmutableTypes.size}
         | took : $analysisTime seconds
         |""".stripMargin
        )

        val calendar = Calendar.getInstance()
        val file = new File(
            s"C:/MA/results/typeImm_withNewPurity_${calendar.get(Calendar.YEAR)}_"+
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
