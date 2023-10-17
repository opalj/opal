/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl

import java.io.File

import scala.jdk.CollectionConverters.MapHasAsScala

import dk.brics.tajs.analysis.Analysis
import dk.brics.tajs.analysis.xl.translator.LocalTAJSAdapter
import dk.brics.tajs.lattice.Value
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.Main
import dk.brics.tajs.Main.run
import dk.brics.tajs.solver.BlockAndContext
import dk.brics.tajs.analysis.xl.translator.TajsAdapter
import dk.brics.tajs.flowgraph.jsnodes.JNode
import dk.brics.tajs.lattice.Context

import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedAnalysis
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.xl.utility.AnalysisResult
import org.opalj.xl.utility.FinalAnalysisResult
import org.opalj.xl.utility.InterimAnalysisResult
import org.opalj.xl.detector.ScriptEngineInteraction
import org.opalj.xl.detector.CrossLanguageInteraction
import org.opalj.xl.translator.JavaJavaScriptTranslator
import org.opalj.xl.translator.translator.globalObject
import org.opalj.xl.utility.Language
import org.opalj.xl.Coordinator.ScriptEngineInstance
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.Results
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisBase
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisState
import org.opalj.tac.fpcf.properties.TheTACAI

abstract class TajsConnector(override val project: SomeProject) extends FPCFAnalysis with PointsToAnalysisBase {
    self =>
    val PROTOCOL_NAME = "tajs-host-env"

    case class TajsConnectorState(
            scriptEngineInstance:        ScriptEngineInstance[ElementType],
            project:                     SomeProject,
            var code:                    List[String]                                      = List.empty, //for debugging purposes
            var files:                   List[File]                                        = null,
            var scriptEngineInteraction: ScriptEngineInteraction[ContextType, PointsToSet] = null,
            var connectorDependees:      Set[EOptionP[Entity, Property]]                   = Set.empty,
            var puts:                    Map[PKey.StringPKey, Value]                       = Map.empty
    ) extends BaseAnalysisState with TypeIteratorState

    def analyzeScriptEngineInstance(scriptEngineInstance: ScriptEngineInstance[ElementType]): ProperPropertyComputationResult = {

        implicit val state = TajsConnectorState(scriptEngineInstance, project)

        def java2js = JavaJavaScriptTranslator.Java2JavaScript[PointsToSet, ContextType]

        def fillEmptyCode(possibleEmptyCode: List[String]): List[String] =
            if (possibleEmptyCode.size == 0)
                List("(function(){})();")
            else
                possibleEmptyCode

        def c(
            oldScriptEngineInteraction: ScriptEngineInteraction[ContextType, PointsToSet],
            oldTAJSanalysis:            Option[Analysis], blockAndContext: Option[BlockAndContext[Context]]
        )(eps: SomeEPS)(implicit state: TajsConnectorState): ProperPropertyComputationResult = {
            state.connectorDependees =
                state.connectorDependees.filter(dependee => dependee.e != eps.e)
            //println(s"connector; eps: $eps")
            eps match {
                case ubp @ UBP(javaScriptInteraction @ ScriptEngineInteraction(Language.JavaScript,
                    possibleEmptyCode, foreignFunctionCall, puts)) =>

                    println(s"foreign Function Call (start): $foreignFunctionCall")
                    state.code = state.code ++ fillEmptyCode(possibleEmptyCode)
                    state.puts ++= puts.map(put => java2js(put._1._1, put._1._2.asInstanceOf[ContextType], put._2.asInstanceOf[PointsToSet], put._1._3, put._1._4))
                    // handleJavaScriptCall(javaScriptFunctionCall.asInstanceOf[List[JavaScriptFunctionCall[ContextType, PointsToSet]]])
                    state.connectorDependees += ubp
                    state.files = utility.asFiles("JavaScript", ".js", state.code)
                    val (analysis, blockAndContext) = analyze(tajsAdapter = tajsAdapter)
                    createResult(javaScriptInteraction.asInstanceOf[ScriptEngineInteraction[ContextType, PointsToSet]], analysis, blockAndContext)

                /*  println(s"foreignFunctionCalls (c): $foreignFunctionCall")
                    state.code = fillEmptyCode(possibleEmptyCode)
                    state.puts =
                        puts.map(put => java2js(put._1._1, put._1._2.asInstanceOf[ContextType], put._2.asInstanceOf[PointsToSet], put._1._3, put._1._4))
                   // handleJavaScriptCall(foreignFunctionCall.asInstanceOf[List[JavaScriptFunctionCall[ContextType, PointsToSet]]])
                    state.scriptEngineInteraction = javaScriptInteraction.asInstanceOf[ScriptEngineInteraction[ContextType, PointsToSet]]
                    state.connectorDependees += eps
                   val (analysisResult, blockAndContextResult) = {
                          /*  if (blockAndContext.isDefined && oldTAJSanalysis.isDefined &&
                                oldScriptEngineInteraction.code == javaScriptInteraction.code)
                                resume(oldTAJSanalysis.get, List(blockAndContext.get).asJavaCollection)
                            else */
                                analyze(tajsAdapter)
                        }
                    createResult(state.scriptEngineInteraction, analysisResult, blockAndContextResult)
                    */
                case UBP(ScriptEngineInteraction(Language.Unknown, _, _, _)) =>
                    Results()

                case ep =>
                    state.connectorDependees += ep
                    createResult(ScriptEngineInteraction[ContextType, PointsToSet](), oldTAJSanalysis, None)
            }
        }

        def analyze(tajsAdapter: TajsAdapter)(implicit state: TajsConnectorState): (Option[Analysis], Option[BlockAndContext[Context]]) = {
            println(s">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>start tajs ${state.code.mkString("\n")} \n ${state.puts.mkString("\n")}  ")
            println("----------")

            LocalTAJSAdapter.setLocalTAJSAdapter(tajsAdapter)

            val arguments = state.files.map(_.getPath).toArray
            val analysis = Main.init(arguments, null)

            val flowGraph = analysis.getSolver.getFlowGraph
            val mainFunction = flowGraph.getMain
            val analysisLatticeElement = analysis.getSolver.getAnalysisLatticeElement
            val entryBlock = mainFunction.getEntry
            analysisLatticeElement.getStates(entryBlock).forEach((context, tajsState) => {
                val obj = tajsState.getStore.get(globalObject)
                obj.setWritable()
                state.puts.foreach(put =>
                    tajsState.getStore.get(globalObject).setProperty(put._1, put._2))
            })
            run(analysis, new java.util.HashMap[PKey.StringPKey, Value]()) //state.puts.asJava)
            println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> end tajs")
            (Some(analysis), None)
        }

        /* def resume(oldAnalysis: Analysis,
                   blockAndContext: java.util.Collection[BlockAndContext[Context]]
                  )(implicit state: TajsConnectorState): (Option[Analysis], Option[BlockAndContext[Context]]) = {
            println(s"--------------------------------start resume tajs ${state.code.mkString("\n")}")
            try {
                rerun(oldAnalysis, state.puts.asJava, blockAndContext)
            } catch {
                case crossLanguageAnalysisException: CrossLanguageAnalysisException =>
                    val analysis = crossLanguageAnalysisException.getAnalysis
                    val bc = crossLanguageAnalysisException.getBlockAndContext
                    return (Some(analysis), Some(bc.asInstanceOf[BlockAndContext[Context]]))
            } finally {
                println("---------------------------- end resume tajs")
            }
            (Some(oldAnalysis), None)
        } */

        /* def handleJavaScriptCall(javaScriptFunctionCalls: List[JavaScriptFunctionCall[ContextType, PointsToSet]]
                                )(implicit state: TajsConnectorState): Unit = {
            if (javaScriptFunctionCalls.nonEmpty) {
                state.code = state.code ++ javaScriptFunctionCalls.map(javaScriptFunctionCall => {
                    val params =
                        if (javaScriptFunctionCall.actualParams.size > 0)
                            javaScriptFunctionCall.actualParams.keys.map(key =>VarNames.genVName(key._1)).mkString(", ")
                        else
                            ""
                    state.puts ++= javaScriptFunctionCall.actualParams.map(actualParam => {
                       val result = JavaJavaScriptTranslator.Java2JavaScript(VarNames.genVName(actualParam._1._1), actualParam._1._2, actualParam._2, actualParam._1._3, actualParam._1._4)
                        (result._1, result._2)
                    })

                    s"${Constants.javaScriptResultVariableName} = " +
                        s"${javaScriptFunctionCall.functionName}(${params});"
                })
            }
        } */

        object tajsAdapter extends TajsAdapter {
            override def queryObject(v: Value): Value = {
                println(s"Adapter Call.................................$v")

                val possibleValues = new java.util.ArrayList[Value]()
                if (v.isJavaObject) {
                    v.getObjectLabels.forEach(
                        ol => {
                            val objectLabel = ol.getNode.asInstanceOf[JNode[ElementType, ContextType, IntTrieSet, TheTACAI]]
                            val context = ol.getContextType.asInstanceOf[ContextType]
                            implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                                new PointsToAnalysisState(context, FinalEP(context.method.definedMethod, objectLabel.getTacai))
                            objectLabel.getDefSites.foreach(defSite => {
                                val pointsToSet = currentPointsToOfDefSite("object", defSite)
                                possibleValues.add(
                                    java2js("", context, pointsToSet, objectLabel.getDefSites, objectLabel.getTacai)._2
                                )
                            })
                            val objectDependeesMap = pointsToAnalysisState.dependeesOf("object")
                            val objectDependees = objectDependeesMap.valuesIterator.map(_._1)

                            state.connectorDependees = state.connectorDependees ++ objectDependees.toSet
                        }
                    )
                    Value.join(possibleValues)
                } else
                    Value.makeUndef()

            }

            override def queryField(v: Value, fieldName: String): Value = ???

            override def queryMethod(v: Value, methodName: String): Value = ???
        }

        def createResult(
            scriptEngineInteraction: ScriptEngineInteraction[ContextType, PointsToSet],
            analysis:                Option[Analysis],
            blockAndContext:         Option[BlockAndContext[Context]]
        )(implicit state: TajsConnectorState): ProperPropertyComputationResult = {
            var store: Map[PKey, Value] = Map.empty[PKey, Value]
            if (analysis.isDefined) {
                val mainFunction = analysis.get.getSolver.getFlowGraph.getMain
                val ordinaryExitBlock = mainFunction.getOrdinaryExit
                //  val exceptionalExitBlock = mainFunction.getExceptionalExit
                val analysisLatticeElement = analysis.get.getSolver.getAnalysisLatticeElement
                val ordinaryExitStates = analysisLatticeElement.getStates(ordinaryExitBlock)
                // val exceptionalExitStates = analysisLatticeElement.getStates(exceptionalExitBlock)

                val ordinaryExistStore = ordinaryExitStates.
                    asScala.values.flatMap(_.getStore.get(globalObject).getAllProperties.asScala).
                    toMap
                //val exceptionalExitStore = exceptionalExitStates.asScala.values.
                //        flatMap(_.getStore.get(globalObject).getAllProperties.asScala).toMap

                store = ordinaryExistStore /*.map(entry => (entry._1, entry._2.join(exceptionalExitStore.getOrElse(entry._1, Value.makeUndef())))) ++
                    exceptionalExitStore.filter(entry=> !ordinaryExistStore.contains(entry._1))

                store = store.map(entry=>(entry._1, entry._2.join(Value.makeUndef())))*/
            }

            println(s"store: ${store.mkString("\n")}")
            InterimResult(
                scriptEngineInstance,
                InterimAnalysisResult[PKey, Value](store),
                FinalAnalysisResult[PKey, Value](store),
                state.connectorDependees,
                c(scriptEngineInteraction, analysis, blockAndContext)
            )
        }

        //start of analysis
        val result = propertyStore(scriptEngineInstance, CrossLanguageInteraction.key)
        println(s"connector start, result: $result")
        result match {
            case ubp @ UBP(engineInteraction @ ScriptEngineInteraction(
                Language.JavaScript, possibleEmptyCode, javaScriptFunctionCall, puts)) =>
                println(s"foreign Function Call (start): $javaScriptFunctionCall")
                state.code = state.code ++ fillEmptyCode(possibleEmptyCode)
                state.puts ++= puts.map(put => java2js(put._1._1, put._1._2.asInstanceOf[ContextType], put._2.asInstanceOf[PointsToSet], put._1._3, put._1._4))
                // handleJavaScriptCall(javaScriptFunctionCall.asInstanceOf[List[JavaScriptFunctionCall[ContextType, PointsToSet]]])
                state.connectorDependees += ubp
                state.files = utility.asFiles("JavaScript", ".js", state.code)
                val (analysis, blockAndContext) = analyze(tajsAdapter = tajsAdapter)
                createResult(engineInteraction.asInstanceOf[ScriptEngineInteraction[ContextType, PointsToSet]], analysis, blockAndContext)

            case UBP(ScriptEngineInteraction(Language.Unknown, _, _, _)) =>
                Results()

            case ep =>
                state.connectorDependees += ep
                createResult(ScriptEngineInteraction[ContextType, PointsToSet](), None, None)
        }
    }
}

trait TriggeredTajsConnectorScheduler extends BasicFPCFTriggeredAnalysisScheduler {
    def propertyKind: PropertyMetaInformation

    def createAnalysis: SomeProject => TajsConnector

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, DefinitionSitesKey, TypeIteratorKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(CrossLanguageInteraction, propertyKind, Callees)

    override def triggeredBy: PropertyKey[CrossLanguageInteraction] = CrossLanguageInteraction.key

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(propertyKind, AnalysisResult)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): TajsConnector = {
        val analysis = createAnalysis(p)
        ps.registerTriggeredComputation(triggeredBy, analysis.analyzeScriptEngineInstance)
        analysis
    }
}

object TypeBasedTriggeredTajsConnectorScheduler extends TriggeredTajsConnectorScheduler {
    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => TajsConnector =
        new TajsConnector(_) with TypeBasedAnalysis
}

object AllocationSiteBasedTriggeredTajsConnectorScheduler extends TriggeredTajsConnectorScheduler {
    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => TajsConnector =
        new TajsConnector(_) with AllocationSiteBasedAnalysis
}
