/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import com.typesafe.config.ConfigValueFactory

import org.opalj.ai.domain
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.android.AndroidManifestKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.cg.CFA_1_0_CallGraphKey
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

object AndroidAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "Generate Callgraph for Android App"

    override def description: String = "Parses AndroidManifest.xml to determine entry points"

    override def analysisSpecificParametersDescription: String = {
        "[-manifest=\"<Path to AndroidManifest.xml " +
          "(e.g., -manifest=path/to/AndroidManifest.xml) ]"
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        val manifestPath = parameters.collectFirst {
            case param if param.startsWith("-manifest=") => param.split("=")(1)
        }

        if (manifestPath.isEmpty)
            throw new IllegalArgumentException(
                "Pass path to (non-binary) manifest with -manifest=path/to/AndroidManifest.xml"
            )

        var newConfig = project.config
        newConfig = newConfig.withValue(
            InitialEntryPointsKey.ConfigKey,
            ConfigValueFactory.fromAnyRef("org.opalj.tac.cg.android.AndroidEntryPointsFinder")
        )

        val newProject = Project.recreate(project, newConfig)
        val result = analyze(newProject, manifestPath.get)
        BasicReport(result)
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Iterable[String] = {
        val remainingParameters =
            parameters.filter { p => !p.startsWith("-manifest=") }
        super.checkAnalysisSpecificParameters(remainingParameters)
    }

    def analyze(project: Project[URL], manifestPath: String): String = {

        var propertyStoreTime: Seconds = Seconds.None
        var callGraphTime: Seconds = Seconds.None
        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>
            Set[Class[_ <: AnyRef]](classOf[domain.l2.DefaultPerformInvocationsDomainWithCFG[URL]])
        }

        project.updateProjectInformationKeyInitializationData(AndroidManifestKey) {
            _ => manifestPath
        }

        implicit val ps: PropertyStore = time {
            project.get(PropertyStoreKey)
        } { t => propertyStoreTime = t.toSeconds }

        val cg = time {
            project.get(CFA_1_0_CallGraphKey)
        } { t => callGraphTime = t.toSeconds }

        try {
            ps.shutdown()
        } catch {
            case t: Throwable =>
                Console.err.println("PropertyStore shutdown failed: ")
                t.printStackTrace()
        }

        val reachableContexts = cg.reachableMethods().to(Iterable)
        val reachableMethods = reachableContexts.map(_.method).toSet

        val numEdges = cg.numEdges

        s"""
       | Num Reachable Methods: ${reachableMethods.size}
       | Num Edges: ${numEdges}
       |
       | CG Analysis took : $callGraphTime seconds
       |
       | level: ${project.getProjectInformationKeyInitializationData(AIDomainFactoryKey)}
       | propertyStore: ${ps.getClass}
       |
       |""".stripMargin
    }
}
