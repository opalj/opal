/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import scala.util.Try

import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.fpcf.par.PKECPropertyStore
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.fpcf.analyses.cg.reflection.ReflectionRelatedCallsAnalysisScheduler
import org.opalj.tac.fpcf.analyses.string.LazyStringAnalysis
import org.opalj.tac.fpcf.analyses.string.flowanalysis.LazyMethodStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l0.LazyL0StringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l1.LazyL1StringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l2.LazyL2StringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l3.LazyL3StringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.trivial.LazyTrivialStringAnalysis
import org.opalj.tac.fpcf.analyses.systemproperties.TriggeredSystemPropertiesAnalysisScheduler
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * @author Maximilian Rüsch
 */
object StringAnalysisDemo extends ProjectAnalysisApplication {

    private case class Configuration(
        private val analysisConfig: Configuration.AnalysisConfig
    ) {

        def analyses: Seq[FPCFLazyAnalysisScheduler] = {
            analysisConfig match {
                case Configuration.TrivialAnalysis => Seq(LazyTrivialStringAnalysis)
                case Configuration.LevelAnalysis(level) =>
                    Seq(
                        LazyStringAnalysis,
                        LazyMethodStringFlowAnalysis,
                        Configuration.LevelToSchedulerMapping(level)
                    )
            }
        }
    }

    private object Configuration {

        private[Configuration] trait AnalysisConfig
        private[Configuration] case object TrivialAnalysis extends AnalysisConfig
        private[Configuration] case class LevelAnalysis(level: Int) extends AnalysisConfig

        final val LevelToSchedulerMapping = Map(
            0 -> LazyL0StringFlowAnalysis,
            1 -> LazyL1StringFlowAnalysis,
            2 -> LazyL2StringFlowAnalysis,
            3 -> LazyL3StringFlowAnalysis
        )

        def apply(parameters: Seq[String]): Configuration = {
            val levelParameter = parameters.find(_.startsWith("-level=")).getOrElse("-level=trivial")
            val analysisConfig = levelParameter.replace("-level=", "") match {
                case "trivial" => TrivialAnalysis
                case string    => LevelAnalysis(string.toInt)
            }

            new Configuration(analysisConfig)
        }
    }

    override def description: String =
        """
          | Analyses the callees of the Main.entrypoint method of the given project,
          | e.g. run with the DEVELOPING_OPAL/demos/src/main/resources/opal-xerces-playground.zip package.
          |
          | Also contains some live logging of runtime information about the property store.
          |""".stripMargin

    override def analysisSpecificParametersDescription: String =
        s"[-level=trivial|${Configuration.LevelToSchedulerMapping.keys.toSeq.sorted.mkString("|")}]"

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Iterable[String] = {
        parameters.flatMap {
            case levelParameter if levelParameter.startsWith("-level=") =>
                levelParameter.replace("-level=", "") match {
                    case "trivial" =>
                        None
                    case string
                        if Try(string.toInt).isSuccess
                            && Configuration.LevelToSchedulerMapping.keySet.contains(string.toInt) =>
                        None
                    case value =>
                        Some(s"Unknown level parameter value: $value")
                }

            case param => Some(s"unknown parameter: $param")
        }
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        implicit val configuration: Configuration = Configuration(parameters)
        BasicReport(analyze(project))
    }

    def analyze(project: Project[URL])(implicit configuration: Configuration): String = {
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

        val ex = new ScheduledThreadPoolExecutor(1)
        val task = new Runnable {
            def run(): Unit = {
                propertyStore match {
                    case realPS: PKECPropertyStore =>
                        val sizes = realPS.ps.map(_.size())
                        val topEPK = sizes.zipWithIndex.sortBy(-_._1).take(5)
                            .filter(_._2 <= PropertyKey.maxId)
                            .map(s => s"${PropertyKey.key(s._2)}: ${s._1}")
                        System.out.println(s"Top States: ${topEPK.mkString("\n|        ", "\n|        ", "\n")}")
                        System.out.println(s"PS EPK States: ${sizes.sum}")
                        System.out.println(s"PS Active tasks: ${realPS.activeTasks.get}")
                        System.out.println("---------------------------------------------------------------------------")

                    case _ =>
                }
            }
        }
        val f = ex.scheduleAtFixedRate(task, 0, 5, TimeUnit.SECONDS)
        project.get(SimpleContextsKey)
        time {
            analysesManager
                .runAll(
                    cgKey.allCallGraphAnalyses(project)
                        ++ configuration.analyses
                        ++ Seq(
                            ReflectionRelatedCallsAnalysisScheduler,
                            TriggeredSystemPropertiesAnalysisScheduler
                        )
                )
            propertyStore.waitOnPhaseCompletion()
        } { t => analysisTime = t.toSeconds }
        f.cancel(false)

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

        s"""
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
           | took : $analysisTime seconds
           |""".stripMargin
    }
}
