/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.io.File

import org.opalj.br.ClassFile
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.L0SelfReferenceLeakageAnalysis
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.DoesNotLeakSelfReference
import org.opalj.br.fpcf.properties.SelfReferenceLeakage
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the default self-reference leakage analysis.
 *
 * @author Michael Eichberg
 */
object SelfReferenceLeakageAnalysis extends ProjectsAnalysisApplication {

    protected class SelfReferenceLeakageConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with PropertyStoreBasedCommandLineConfig {
        val description =
            "Determines classes leaking their self reference, if not, then the method which instantiates the object has full control"
    }

    protected type ConfigType = SelfReferenceLeakageConfig

    protected def createConfig(args: Array[String]): SelfReferenceLeakageConfig = new SelfReferenceLeakageConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: SelfReferenceLeakageConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (projectStore, _) = analysisConfig.setupPropertyStore(project)

        var analysisTime = Seconds.None
        time {
            projectStore.setupPhase(Set[PropertyKind](SelfReferenceLeakage.Key))
            L0SelfReferenceLeakageAnalysis.start(project, null)
            projectStore.waitOnPhaseCompletion()
        } { t => analysisTime = t.toSeconds }

        val notLeakingEntities: Iterator[EPS[Entity, SelfReferenceLeakage]] =
            projectStore.entities(SelfReferenceLeakage.Key) filter { eps => eps.lb == DoesNotLeakSelfReference }
        val notLeakingClasses = notLeakingEntities map { eps =>
            val classFile = eps.e.asInstanceOf[ClassFile]
            val classType = classFile.thisType
            val className = classFile.thisType.toJava
            if (project.classHierarchy.isInterface(classType).isYes)
                "interface " + className
            else
                "class " + className
        }

        val leakageInfo =
            notLeakingClasses.toList.sorted.mkString(
                "\nClasses not leaking self reference:\n",
                "\n",
                s"\nTotal: ${notLeakingEntities.size}\n"
            )
        (project, BasicReport(leakageInfo + projectStore + "\nAnalysis time: " + analysisTime))
    }
}
