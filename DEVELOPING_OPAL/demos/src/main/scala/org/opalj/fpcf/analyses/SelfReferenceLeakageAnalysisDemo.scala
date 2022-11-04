/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.ClassFile
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.L0SelfReferenceLeakageAnalysis
import org.opalj.br.fpcf.properties.DoesNotLeakSelfReference
import org.opalj.br.fpcf.properties.SelfReferenceLeakage

/**
 * Runs the default self-reference leakage analysis.
 *
 * @author Michael Eichberg
 */
object SelfReferenceLeakageAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "Analyses whether a class leaks it self-reference this"

    override def description: String = {
        "Determines if a class leaks its self reference, if not, then the method which instantiates the object has full control."
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        val projectStore = project.get(PropertyStoreKey)

        var analysisTime = Seconds.None
        time {
            projectStore.setupPhase(Set[PropertyKind](SelfReferenceLeakage.Key))
            L0SelfReferenceLeakageAnalysis.start(project, null)
            projectStore.waitOnPhaseCompletion()
        } { t => analysisTime = t.toSeconds }

        val notLeakingEntities: Iterator[EPS[Entity, SelfReferenceLeakage]] =
            projectStore.entities(SelfReferenceLeakage.Key) filter { eps =>
                eps.lb == DoesNotLeakSelfReference
            }
        val notLeakingClasses = notLeakingEntities map { eps =>
            val classFile = eps.e.asInstanceOf[ClassFile]
            val classType = classFile.thisType
            val className = classFile.thisType.toJava
            if (project.classHierarchy.isInterface(classType).isYes)
                "interface "+className
            else
                "class "+className
        }

        val leakageInfo =
            notLeakingClasses.toList.sorted.mkString(
                "\nClasses not leaking self reference:\n",
                "\n",
                s"\nTotal: ${notLeakingEntities.size}\n"
            )
        BasicReport(leakageInfo + projectStore+"\nAnalysis time: "+analysisTime)
    }
}
