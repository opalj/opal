/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package connector

import java.io.File
import scala.jdk.CollectionConverters.MapHasAsScala

import dk.brics.tajs.analysis.Analysis
import dk.brics.tajs.analysis.xl.LocalTAJSAdapter
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value
import dk.brics.tajs.Main
import dk.brics.tajs.Main.run
import dk.brics.tajs.analysis.xl.TajsAdapter
import dk.brics.tajs.flowgraph.jsnodes.JNode
import dk.brics.tajs.lattice.Context
import dk.brics.tajs.solver.BlockAndContext
import org.opalj.xl.utility
import org.opalj.xl.Coordinator
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.ObjectType
import org.opalj.xl.utility.AnalysisResult
import org.opalj.xl.utility.InterimAnalysisResult
import org.opalj.xl.detector.ScriptEngineInteraction
import org.opalj.xl.detector.CrossLanguageInteraction
import org.opalj.xl.translator.JavaJavaScriptTranslator
import org.opalj.xl.translator.translator.globalObject
import org.opalj.xl.utility.Language
import org.opalj.xl.Coordinator.ScriptEngineInstance
import org.opalj.xl.utility.Bottom
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Fields
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisBase
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisState
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.longToAllocationSite
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.OnlyCallersWithUnknownContext

abstract class TajsConnector(override val project: SomeProject) extends FPCFAnalysis with PointsToAnalysisBase {
    self =>

    case class TajsConnectorState(
            scriptEngineInstance:        ScriptEngineInstance[ElementType],
            project:                     SomeProject,
            var code:                    List[String]                                      = List.empty, //for debugging purposes
            var files:                   List[File]                                        = null,
            var scriptEngineInteraction: ScriptEngineInteraction[ContextType, PointsToSet] = null,
            var connectorDependees:      Set[EOptionP[Entity, Property]]                   = Set.empty,
            var puts:                    Map[PKey.StringPKey, Value]                       = Map.empty,
            var connectorResults:        Set[ProperPropertyComputationResult]              = Set.empty[ProperPropertyComputationResult]
    ) extends BaseAnalysisState with TypeIteratorState

    def analyzeScriptEngineInstance(scriptEngineInstance: ScriptEngineInstance[ElementType]): ProperPropertyComputationResult = {

        implicit val state = TajsConnectorState(scriptEngineInstance, project)

        def java2js = JavaJavaScriptTranslator.Java2JavaScript[PointsToSet, ContextType] _
        // def js2java = JavaJavaScriptTranslator.JavaScript2Java[PointsToSet, ContextType]

        val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

        def fillEmptyCode(possibleEmptyCode: List[String]): List[String] =
            if (possibleEmptyCode.size == 0)
                List("(function(){})();")
            else
                possibleEmptyCode

        def c(
            oldScriptEngineInteraction: ScriptEngineInteraction[ContextType, PointsToSet],
            oldTAJSanalysis:            Option[Analysis], blockAndContext: Option[BlockAndContext[Context]]
        )(eps: SomeEPS)(implicit state: TajsConnectorState): ProperPropertyComputationResult = {
            state.connectorDependees = state.connectorDependees.filter(dependee => dependee.e != eps.e)

            eps match {

                case UBP(javaScriptInteraction @ ScriptEngineInteraction(Language.JavaScript, possibleEmptyCode, _, puts)) =>
                    println(oldScriptEngineInteraction == javaScriptInteraction)
                    println("---")
                    println(oldScriptEngineInteraction.puts.map(_._2._1).mkString("\n"))
                    println("- - - - - - - -")
                    println(puts.map(_._2._1).mkString("\n"))
                    puts.map(_._2._1).foreach(
                        put => {
                            val pts = put.asInstanceOf[AllocationSitePointsToSet]
                            if (pts.numElements > 5) {
                                pts.forNewestNElements(pts.numElements) {
                                    x =>
                                        println(longToAllocationSite(x))
                                }
                                throw new Exception(
                                    s"""
                                       | code: ${state.code}
                                       | puts: ${state.puts}
                                       |""".stripMargin
                                )
                            }
                        }
                    )
                    println("***")

                    prepareAnalysis(possibleEmptyCode, puts)
                    val (analysis, blockAndContext) = start(tajsAdapter = tajsAdapter)
                    state.connectorDependees += eps
                    createResult(javaScriptInteraction.asInstanceOf[ScriptEngineInteraction[ContextType, PointsToSet]], analysis, blockAndContext)

                case UBP(_: PointsToSet @unchecked) =>
                    println("Resume:")
                    val (analysis, blockAndContext) = start(tajsAdapter = tajsAdapter)
                    state.connectorDependees += eps
                    createResult(oldScriptEngineInteraction, analysis, blockAndContext)

                case ep =>
                    state.connectorDependees += ep
                    createResult(ScriptEngineInteraction[ContextType, PointsToSet](), oldTAJSanalysis, None)
            }
        }

        def start(tajsAdapter: TajsAdapter)(implicit state: TajsConnectorState): (Option[Analysis], Option[BlockAndContext[Context]]) = {
            println("start tajs")
            println(state.puts)
            LocalTAJSAdapter.setLocalTAJSAdapter(tajsAdapter)

            val javaScriptFilePaths = state.files.map(_.getPath).toArray
            val analysis = Main.init(javaScriptFilePaths, null)

            val flowGraph = analysis.getSolver.getFlowGraph
            val mainFunction = flowGraph.getMain
            val analysisLatticeElement = analysis.getSolver.getAnalysisLatticeElement
            val entryBlock = mainFunction.getEntry
            analysisLatticeElement.getStates(entryBlock).forEach((context, tajsState) => {
                val obj = tajsState.getStore.get(globalObject)
                obj.setWritable()
                state.puts.foreach(put => tajsState.getStore.get(globalObject).setProperty(put._1, put._2))
            })
            run(analysis)
            (Some(analysis), None)
        }

        /*  def resume(analysis: Analysis): (Option[Analysis], Option[BlockAndContext[Context]]) = {
            println("resume")
            run(analysis)
            (Some(analysis), None)
        } */

        object tajsAdapter extends TajsAdapter {

            override def newObject(index: Integer, javaName: String): Value = {
                val referenceType = ObjectType(javaName.replace(".", "/"))

                val newPointsToSet = createPointsToSet(-100 - index, NoContext.asInstanceOf[ContextType], referenceType, false, false)
                val value = java2js("newObject", NoContext.asInstanceOf[ContextType], newPointsToSet, null, None)._2
                value
            }

            override def setProperty(v: Value, propertyName: String, rhsFieldValue: Value): Unit = {

                if (v.isJavaObject) {
                    v.getObjectLabels.forEach(ol => {
                        val jNode = ol.getNode.asInstanceOf[JNode[ElementType, ContextType, IntTrieSet, TheTACAI]]
                        val javaName = ol.getJavaName
                        val objectType = ObjectType(javaName.replace(".", "/"))
                        val classFile = project.classFile(objectType)
                        val possibleFields = {
                            if (classFile.isDefined)
                                classFile.get.fields.find(_.name == propertyName).toList
                            else
                                List.empty[Fields]
                        } //TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        //val context = jNode.getContext
                        val pointsToSet = jNode.getPointsToSet.asInstanceOf[PointsToSet]

                        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                            new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null) //FinalEP(context.method.definedMethod, jNode.getTacai))
                        rhsFieldValue.getObjectLabels.forEach(ol => {
                            val node = ol.getNode
                            val rhsPointsToSet =
                                if (ol.getNode.isInstanceOf[JNode[_, _, _, _]]) {
                                    val jnode = ol.getNode.asInstanceOf[JNode[_, _, _, _]]
                                    val pointsToSet = jnode.getPointsToSet.asInstanceOf[PointsToSet]
                                    pointsToSet
                                } else {
                                    val index = -100 - node.getIndex
                                    createPointsToSet(index, NoContext.asInstanceOf[ContextType], ObjectType.Object, false, false)
                                }

                            possibleFields.foreach(field => {
                                pointsToSet.forNewestNElements(pointsToSet.numElements) { as =>
                                    val tpe = getTypeOf(as)
                                    if (tpe.isObjectType) {
                                        Iterator((as, field)).foreach(fieldEntity => {
                                            pointsToAnalysisState.includeSharedPointsToSet(
                                                fieldEntity,
                                                rhsPointsToSet,
                                                PointsToSetLike.noFilter
                                            )
                                        })
                                    }
                                }
                            })
                        })

                        val setPropertyDependeesMap =
                            if (pointsToAnalysisState.hasDependees("setProperty"))
                                pointsToAnalysisState.dependeesOf("setProperty")
                            else
                                Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

                        state.connectorDependees ++= setPropertyDependeesMap.valuesIterator.map(_._1)
                        state.connectorResults ++= createResults
                    })
                } else if (v.isJSJavaTYPE) {
                    v.getObjectLabels.forEach(ol => {
                        val javaName = ol.getJavaName
                        val objectType = ObjectType(javaName.replace(".", "/"))
                        val classFile = project.classFile(objectType)
                        val possibleFields = {
                            if (classFile.isDefined)
                                classFile.get.fields.find(field => field.name == propertyName && field.isStatic).toList
                            else
                                List.empty[Fields]
                        }

                        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                            new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null) //FinalEP(context.method.definedMethod, jNode.getTacai))
                        rhsFieldValue.getObjectLabels.forEach(ol => {
                            val node = ol.getNode
                            val rhsPointsToSet =
                                if (ol.getNode.isInstanceOf[JNode[_, _, _, _]]) {
                                    val jnode = ol.getNode.asInstanceOf[JNode[_, _, _, _]]
                                    jnode.getPointsToSet.asInstanceOf[PointsToSet]
                                } else {
                                    val index = -100 - node.getIndex
                                    createPointsToSet(index, NoContext.asInstanceOf[ContextType], ObjectType.Object, false, false)
                                }

                            possibleFields.foreach(field => {
                                pointsToAnalysisState.includeSharedPointsToSet(
                                    field,
                                    rhsPointsToSet,
                                    PointsToSetLike.noFilter
                                )
                            })

                        })
                        val setPropertyDependeesMap =
                            if (pointsToAnalysisState.hasDependees("setProperty"))
                                pointsToAnalysisState.dependeesOf("setProperty")
                            else
                                Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

                        state.connectorDependees ++= setPropertyDependeesMap.valuesIterator.map(_._1)
                        state.connectorResults ++= createResults
                    })
                }
            }

            override def readProperty(v: Value, propertyName: String): Value = {
                var jsValue = Value.makeUndef() //Value.makeAbsent()
                if (v.isJavaObject) {
                    v.getObjectLabels.forEach(ol => {
                        val jNode = ol.getNode.asInstanceOf[JNode[ElementType, ContextType, IntTrieSet, TheTACAI]]
                        val javaName = ol.getJavaName
                        val objectType = ObjectType(javaName.replace(".", "/"))
                        val classFile = project.classFile(objectType)
                        val possibleFields = classFile.get.fields.find(_.name == propertyName)
                        //val context = jNode.getContext
                        val tacai = jNode.getTacai
                        val baseValuePointsToSet = jNode.getPointsToSet.asInstanceOf[PointsToSet]

                        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                            new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

                        baseValuePointsToSet.forNewestNElements(baseValuePointsToSet.numElements) { as =>
                            possibleFields.foreach(field => {
                                val fieldEntities = Iterator((as, field))
                                for (fieldEntity <- fieldEntities) {
                                    val propertyPointsToSet =
                                        currentPointsTo("readProperty", fieldEntity, PointsToSetLike.noFilter)
                                    val fieldType = field.fieldType
                                    val t = if (fieldType.isObjectType) {
                                        Some(fieldType.asObjectType)
                                    } else None
                                    jsValue = jsValue.join(java2js(propertyName, NoContext.asInstanceOf[ContextType], propertyPointsToSet, tacai, t)._2)
                                }
                            })
                        }

                        val readPropertyDependeesMap = if (pointsToAnalysisState.hasDependees("readProperty"))
                            pointsToAnalysisState.dependeesOf("readProperty")
                        else
                            Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

                        state.connectorDependees = state.connectorDependees ++
                            readPropertyDependeesMap.valuesIterator.map(_._1)
                        state.connectorResults ++= createResults
                    })
                } else if (v.isJSJavaTYPE) {
                    v.getObjectLabels.forEach(ol => {
                        val javaName = ol.getJavaName
                        val objectType = ObjectType(javaName.replace(".", "/"))
                        val classFile = project.classFile(objectType)
                        val possibleField: Option[Field] = {
                            if (classFile.isDefined)
                                classFile.get.fields.find(field => field.name == propertyName && field.isStatic)
                            else
                                Option.empty
                        }

                        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] = {
                            new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

                        }
                        possibleField match {
                            case Some(field) =>
                                val propertyPointsToSet =
                                    currentPointsTo("readProperty", field, PointsToSetLike.noFilter)
                                val fieldType = field.fieldType
                                val t = if (fieldType.isObjectType) {
                                    Some(fieldType.asObjectType)
                                } else None
                                jsValue = jsValue.join(java2js(propertyName, NoContext.asInstanceOf[ContextType], propertyPointsToSet, null, t)._2)
                            case _ =>
                        }

                        val readPropertyDependeesMap = if (pointsToAnalysisState.hasDependees("readProperty"))
                            pointsToAnalysisState.dependeesOf("readProperty")
                        else
                            Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

                        state.connectorDependees = state.connectorDependees ++
                            readPropertyDependeesMap.valuesIterator.map(_._1)
                        state.connectorResults ++= createResults
                    })
                }
                jsValue
            }

            override def callFunction(v: Value, methodName: String, parameters: java.util.List[Value]): Value = {
                var result = Value.makeStr("Test")//Value.makeAbsent()

                if (v.isJavaObject || v.isJSJavaTYPE) {
                    v.getObjectLabels.forEach(ol => {
                        // val jNode = ol.getNode.asInstanceOf[JNode[ElementType, ContextType, IntTrieSet, TheTACAI]]
                        val javaName = ol.getJavaName
                        val objectType = ObjectType(javaName.replace(".", "/"))
                        val classFile = project.classFile(objectType)

                        var possibleMethods = Iterable.empty[Method]
                        if (v.isJavaObject) {
                            possibleMethods = project.instanceMethods(objectType)
                                .filter(_.name == methodName).map(_.method)
                        } else if (v.isJSJavaTYPE) {
                            possibleMethods = project.allMethods.filter(_.classFile == classFile.orNull).
                                filter(_.isStatic).filter(_.name == methodName)
                        }

                        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                            new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

                        possibleMethods.foreach(method => {

                            val declaredMethod = declaredMethods(method)
                            val context = typeIterator.newContext(declaredMethod)

                            //Call Graph
                            state.connectorResults += PartialResult[DeclaredMethod, Callers](declaredMethod, Callers.key, {
                                case InterimUBP(ub) if !ub.hasCallersWithUnknownContext =>
                                    Some(InterimEUBP(declaredMethod, ub.updatedWithUnknownContext()))

                                case _: InterimEP[_, _] => None

                                case _: EPK[_, _] =>
                                    Some(InterimEUBP(declaredMethod, OnlyCallersWithUnknownContext))

                                case r =>
                                    throw new IllegalStateException(s"unexpected previous result $r")
                            })

                            // function parameters
                            val fps = formalParameters(declaredMethod)
                            var paramIndex = 0
                            parameters.forEach(parameter => {
                                var parameterPointsToSet = emptyPointsToSet
                                if (parameter.isJavaObject) {
                                    parameter.getObjectLabels.forEach(ol =>
                                        parameterPointsToSet = ol.getNode.asInstanceOf[JNode[PointsToSet, ContextType, IntTrieSet, TheTACAI]].getPointsToSet)
                                }
                                val paramType = declaredMethod.descriptor.parameterType(paramIndex)
                                val fp = getFormalParameter(paramIndex + 1, fps, context)
                                val filter = (t: ReferenceType) => classHierarchy.isSubtypeOf(t, paramType.asReferenceType)
                                pointsToAnalysisState.includeSharedPointsToSet(
                                    fp,
                                    parameterPointsToSet,
                                    filter
                                )
                                paramIndex = paramIndex + 1
                            })

                            if (context.method.descriptor.returnType.isReferenceType) {

                                val pointsToSet = currentPointsTo("callFunction", context, PointsToSetLike.noFilter)

                                val v = java2js("returnValue", context, pointsToSet, null, None)
                                result = v._2.join(result)
                            }
                        })

                        val callFunctionDependeesMap =
                            if (pointsToAnalysisState.hasDependees("callFunction"))
                                pointsToAnalysisState.dependeesOf("callFunction")
                            else
                                Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]
                        state.connectorDependees = state.connectorDependees ++ callFunctionDependeesMap.valuesIterator.map(_._1)
                        state.connectorResults ++= createResults
                    })
                }
                result
            }
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
                val exceptionalExitBlock = mainFunction.getExceptionalExit
                val analysisLatticeElement = analysis.get.getSolver.getAnalysisLatticeElement
                val ordinaryExitStates = analysisLatticeElement.getStates(ordinaryExitBlock)
                val exceptionalExitStates = analysisLatticeElement.getStates(exceptionalExitBlock)

                val ordinaryExistStore = ordinaryExitStates.
                    asScala.values.flatMap(_.getStore.get(globalObject).getAllProperties.asScala).
                    toMap

                val exceptionalExitStore = exceptionalExitStates.asScala.values.
                    flatMap(_.getStore.get(globalObject).getAllProperties.asScala).toMap

                store = ordinaryExistStore.map(entry =>
                    (entry._1, entry._2.join(exceptionalExitStore.getOrElse(entry._1, Value.makeUndef())))) ++
                    exceptionalExitStore.filter(entry => !ordinaryExistStore.contains(entry._1))
                store = store.map(entry => (entry._1, entry._2.join(Value.makeUndef())))
            }

            Results(
                state.connectorResults,
                InterimResult(
                    scriptEngineInstance,
                    Bottom,
                    InterimAnalysisResult[PKey, Value](store),
                    state.connectorDependees,
                    c(scriptEngineInteraction, analysis, blockAndContext)
                )
            )
        }

        def prepareAnalysis(possibleEmptyCode: List[String], puts: Map[(String, Any, TheTACAI), (Any, Coordinator.V, Option[ObjectType])]): Unit = {
            println("prepare analysis")
            println("old:")
            println(puts.mkString("\n"))
            state.code = fillEmptyCode(possibleEmptyCode)
            state.puts = puts.map(put => {
                val variableName = put._1._1
                val context = put._1._2.asInstanceOf[ContextType]
                val pointsToSet = put._2._1.asInstanceOf[PointsToSet]
                println(s"num elements: ${pointsToSet.numElements}")
                val tacai: TheTACAI = put._1._3
                // val v = put._2._2
                val optionType = put._2._3

                val jsValue = java2js(variableName, context, pointsToSet, tacai, optionType)
                jsValue
            })
            println(
                s"""
                   | new:
                   | ${state.puts.mkString("\n")}
                   |""".stripMargin
            )
            state.files = utility.asFiles("JavaScript", ".js", state.code)
        }

        println("start analysis................................CONNECTOR--------------------------------------------------------------------------")

        //start of analysis
        propertyStore(scriptEngineInstance, CrossLanguageInteraction.key) match {

            case ubp @ UBP(interaction @ ScriptEngineInteraction(Language.JavaScript, possibleEmptyCode, _, puts)) =>
                prepareAnalysis(possibleEmptyCode, puts)
                val (analysis, blockAndContext) = start(tajsAdapter = tajsAdapter)
                state.connectorDependees += ubp
                createResult(
                    interaction.asInstanceOf[ScriptEngineInteraction[ContextType, PointsToSet]],
                    analysis,
                    blockAndContext
                )

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
