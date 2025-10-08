/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import scala.language.postfixOps

import java.io.File
import java.net.URL

import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.cli.StringAnalysisArg
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.cli.AnalysisLevelArg
import org.opalj.fpcf.PropertyStoreBasedCommandLineConfig
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.fpcf.analyses.cg.reflection.ReflectionRelatedCallsAnalysisScheduler
import org.opalj.tac.fpcf.analyses.systemproperties.TriggeredSystemPropertiesAnalysisScheduler
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * @author Maximilian Rüsch
 */
object StringAnalysisDemo extends ProjectsAnalysisApplication {

    protected class StringAnalysisDemoConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with PropertyStoreBasedCommandLineConfig {
        val description: String =
            """
              | Analyses the callees of the Main.entrypoint method of the given project,
              | e.g. run with the DEVELOPING_OPAL/demos/src/main/resources/opal-xerces-playground.zip package.
              |
              | Also contains some live logging of runtime information about the property store.
              |""".stripMargin

        private val analysisLevelArg =
            new AnalysisLevelArg(StringAnalysisArg.description, StringAnalysisArg.levels: _*) {
                override val defaultValue: Option[String] = Some("trivial")
                override val withNone = false
            }

        args(
            analysisLevelArg !
        )
        init()

        val analyses: Seq[FPCFAnalysisScheduler[_]] = {
            StringAnalysisArg.getAnalyses(apply(analysisLevelArg)).map(getScheduler(_, eager = false))
        }
    }

    protected type ConfigType = StringAnalysisDemoConfig

    protected def createConfig(args: Array[String]): StringAnalysisDemoConfig = new StringAnalysisDemoConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: StringAnalysisDemoConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, projectTime) = analysisConfig.setupProject()

        val domain = classOf[DefaultDomainWithCFGAndDefUse[_]]
        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => Set(domain)
            case Some(requirements) => requirements + domain
        }

        val cgKey = AllocationSiteBasedPointsToCallGraphKey
        implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        val typeIterator = cgKey.getTypeIterator(project)
        project.updateProjectInformationKeyInitializationData(ContextProviderKey) { _ => typeIterator }

        project.get(SimpleContextsKey)
        time {
            analysesManager
                .runAll(
                    cgKey.allCallGraphAnalyses(project)
                        ++ analysisConfig.analyses
                        ++ Seq(
                            ReflectionRelatedCallsAnalysisScheduler,
                            TriggeredSystemPropertiesAnalysisScheduler
                        )
                )
            propertyStore.waitOnPhaseCompletion()
        } { t => analysisTime = t.toSeconds }

        val declaredMethods = project.get(DeclaredMethodsKey)
        val entrypointMethod = project.allMethodsWithBody.find { m =>
            m.name == "entrypoint" &&
            m.classFile.thisType.fqn.endsWith("Main")
        }.get
        val dm = declaredMethods(entrypointMethod)

        implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)
        val calleesUB = propertyStore(dm, Callees.key).ub
        val calleesByPC = calleesUB.callerContexts.flatMap(calleesUB.callSites(_).iterator).toSeq
        val incompleteCallSites = calleesUB.callerContexts.flatMap(calleesUB.incompleteCallSites(_)).toSeq

        def getDepths(filter: Entity => Boolean): Seq[String] = {
            val depths = propertyStore.entities(StringConstancyProperty.key)
                .filter(eps => filter(eps.e))
                .map(_.ub.tree.depth).toSeq

            depths
                .groupBy(depth => depth)
                .transform((_: Int, counts) => counts.size)
                .toSeq.sortBy(_._1)
                .map(depthAndCount => s"Depth: ${depthAndCount._1}   Count: ${depthAndCount._2}")
        }

        def getMethodsList(contexts: Iterable[Context]): String = {
            if (contexts.size > 50) "\n|     Too many contexts to display!"
            else contexts.iterator
                .map(c => s"- ${c.method.name} on ${c.method.declaringClassType.fqn}")
                .mkString("\n|     ", "\n|     ", "")
        }

        def getPCMethodsList(pcContexts: Iterable[(Int, Iterator[Context])]): String = {
            if (pcContexts.size > 50) "\n|     Too many pc contexts to display!"
            else pcContexts.iterator
                .map(c => s"  PC: ${c._1} ${getMethodsList(c._2.toSeq)}\n")
                .mkString("\n|     ", "\n|     ", "")
        }

        (
            project,
            BasicReport(s"""
           |
           | Callees: ${calleesByPC.size} ${getPCMethodsList(calleesByPC)}
           |
           | Access Sites with missing information: $incompleteCallSites
           |
           | MethodParameterContext depths:
           | ${getDepths(_.getClass.getName.endsWith("MethodParameterContext")).mkString("\n|     ", "\n|     ", "")}
           |
           | Context depths:
           | ${getDepths(_.getClass.getName.endsWith("VariableContext")).mkString("\n|     ", "\n|     ", "")}
           |
           | Definition depths:
           | ${getDepths(_.getClass.getName.endsWith("VariableDefinition")).mkString("\n|     ", "\n|     ", "")}
           |
           | took : $analysisTime seconds (and $projectTime seconds for project setup)
           |""".stripMargin)
        )
    }
}
