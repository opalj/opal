/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package connector

import java.io.File

import scala.jdk.CollectionConverters.MapHasAsScala

import dk.brics.tajs.analysis.Analysis
import dk.brics.tajs.analysis.xl.adapter.LocalTAJSAdapter
import dk.brics.tajs.analysis.xl.adapter.TajsAdapter
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value
import dk.brics.tajs.Main
import dk.brics.tajs.Main.run
import dk.brics.tajs.flowgraph.jsnodes.JNode
import dk.brics.tajs.lattice.Context
import dk.brics.tajs.lattice.ObjectLabel
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
import org.opalj.log.OPALLogger
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
import org.opalj.br.Method
import org.opalj.br.ReferenceType
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
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.OnlyCallersWithUnknownContext
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.br.DeclaredField
import org.opalj.br.fpcf.properties.fieldaccess.IndirectFieldAccesses

abstract class TajsConnector(override val project: SomeProject) extends FPCFAnalysis with PointsToAnalysisBase {
    self =>

    case class TajsConnectorState(
            scriptEngineInstance:        ScriptEngineInstance[ElementType],
            project:                     SomeProject,
            var tajsAdapter:             TajsAdapter                                 = null,
            var code:                    List[String]                                      = List.empty, //for debugging purposes
            var files:                   List[File]                                        = List.empty,
            var scriptEngineInteraction: ScriptEngineInteraction[ContextType, PointsToSet] = null,
            var connectorDependees:      Set[EOptionP[Entity, Property]]                   = Set.empty,
            var puts:                    Map[PKey.StringPKey, Value]                       = Map.empty,
            var connectorResults:        Set[ProperPropertyComputationResult]              = Set.empty[ProperPropertyComputationResult],
            var indirectFieldAccesses: IndirectFieldAccesses = new IndirectFieldAccesses()
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


        def c(oldTAJSanalyses: List[Analysis])
             (eps: SomeEPS)(implicit state: TajsConnectorState): ProperPropertyComputationResult = {

            state.connectorDependees = state.connectorDependees.filter(dependee => dependee.e != eps.e)

            eps match {
                case UBP(interaction @ ScriptEngineInteraction(context, Language.JavaScript, possibleEmptyCode, _, puts)) =>
                    state.scriptEngineInteraction = interaction.asInstanceOf[ScriptEngineInteraction[ContextType, PointsToSet]]
                    prepareAnalysis(possibleEmptyCode, puts)
                    val analyses = state.files.map(file => {
                      runAnalysis(file.getPath)._1.get
                    })
                    state.connectorDependees += eps
                    createResult(analyses)

                case ubp@UBP(_: PointsToSet @unchecked) =>
                    val analyses = state.files.map(file => {
                        runAnalysis(file.getPath)._1.get
                    })
                    if(ubp.isRefinable)
                        state.connectorDependees += ubp
                    createResult(analyses)

                case ep =>
                    state.connectorDependees += ep
                    createResult(oldTAJSanalyses)
            }
        }

        def runAnalysis(javaScriptFilePath: String)(implicit state: TajsConnectorState): (Option[Analysis], Option[BlockAndContext[Context]]) = {
            LocalTAJSAdapter.setLocalTAJSAdapter(state.tajsAdapter)

            val analysis = Main.init(List(javaScriptFilePath).toArray, null)
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

        def newObjectImplementation(index: Integer, javaName: String): Value = {
            val referenceType = ObjectType(javaName.replace(".", "/"))

            val newPointsToSet = createPointsToSet(-100 - index, NoContext.asInstanceOf[ContextType], referenceType, false, false)
            val value = java2js("newObject", NoContext.asInstanceOf[ContextType], newPointsToSet, null, None)._2
            value
        }

        def getPossibleDeclaredFields(propertyName: String, ol: ObjectLabel[_], v: Value): List[DeclaredField] = {
            val javaName = ol.getJavaName
            val objectType = ObjectType(javaName.replace(".", "/"))
            val classFile = project.classFile(objectType)
            val possibleDeclaredFields = {
                if (classFile.isDefined)
                    classFile.get.fields.filter(field => field.name == propertyName && (!v.isJSJavaTYPE || field.isStatic)).map(declaredFields(_)).toList
                else
                    List.empty[DeclaredField]
            }
            possibleDeclaredFields
        }

        def setPropertyImplementation(v: Value, propertyName: String, rhsFieldValue: Value): Unit = {

            implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

            v.getObjectLabels.forEach(ol => {

            val possibleDeclaredFields = getPossibleDeclaredFields(propertyName, ol, v)
            possibleDeclaredFields.foreach(declaredField=>{
                if(declaredField.definedField.isPublic && state.scriptEngineInteraction!=null && state.scriptEngineInteraction.context!=NoContext)
                    state.indirectFieldAccesses.addFieldWrite(state.scriptEngineInteraction.context, 0, declaredField, None, None)
            })
            if (v.isJavaObject || v.isJSJavaTYPE){
                    rhsFieldValue.getObjectLabels.forEach(rhsOl => {
                        val node = rhsOl.getNode
                        val rhsPointsToSet =
                            if (rhsOl.getNode.isInstanceOf[JNode[_, _, _, _]]) {
                                val rhsJNode = node.asInstanceOf[JNode[_, _, _, _]]
                                rhsJNode.getPointsToSet.asInstanceOf[PointsToSet]
                            } else {
                                val index = -100 - node.getIndex
                                createPointsToSet(index, NoContext.asInstanceOf[ContextType], ObjectType.Object, false, false)
                            }
                        if(v.isJavaObject){
                            val jNode = ol.getNode.asInstanceOf[JNode[ElementType, ContextType, IntTrieSet, TheTACAI]]
                            val pointsToSet = if (jNode != null)
                                jNode.getPointsToSet.asInstanceOf[PointsToSet]
                            else
                                emptyPointsToSet
                            possibleDeclaredFields.foreach(declaredField => {
                                pointsToSet.forNewestNElements(pointsToSet.numElements) { as =>
                                    val tpe = getTypeOf(as)
                                    if (tpe.isObjectType) {
                                        Iterator((as, declaredField)).foreach(fieldEntity => {
                                            pointsToAnalysisState.includeSharedPointsToSet(
                                                fieldEntity,
                                                rhsPointsToSet,
                                                PointsToSetLike.noFilter
                                            )
                                        })
                                    }
                                }
                            })
                        } else if(v.isJSJavaTYPE){
                            possibleDeclaredFields.foreach(declaredField => {
                                pointsToAnalysisState.includeSharedPointsToSet(
                                    declaredField,
                                    rhsPointsToSet,
                                    PointsToSetLike.noFilter
                                )
                            })
                        }

                    })
            } })
            val setPropertyDependeesMap =
                if (pointsToAnalysisState.hasDependees("setProperty"))
                    pointsToAnalysisState.dependeesOf("setProperty")
                else
                    Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

            state.connectorDependees ++= setPropertyDependeesMap.valuesIterator.map(_._1)
            state.connectorResults ++= createResults ++ {
                if(state.scriptEngineInteraction!=null && state.scriptEngineInteraction.context!=NoContext)
                    state.indirectFieldAccesses.partialResults(state.scriptEngineInteraction.context)
                else
                    Nil
            }

        }

        def readPropertyImplementation(v: Value, propertyName: String): Value = {

            implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

            var jsValue = Value.makeUndef()
            if (v.isJavaObject || v.isJSJavaTYPE) {
                v.getObjectLabels.forEach(ol => {

                    val possibleDeclaredFields = getPossibleDeclaredFields(propertyName, ol, v)

                    if(v.isJavaObject){
                        val jNode = ol.getNode.asInstanceOf[JNode[ElementType, ContextType, IntTrieSet, TheTACAI]]
                        val tacai = jNode.getTacai
                        val baseValuePointsToSet = jNode.getPointsToSet.asInstanceOf[PointsToSet]
                        baseValuePointsToSet.forNewestNElements(baseValuePointsToSet.numElements) { as =>
                            possibleDeclaredFields.foreach(field => {
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
                    } else if(v.isJSJavaTYPE){
                        possibleDeclaredFields.foreach(declaredField => {
                            val propertyPointsToSet =
                                currentPointsTo("readProperty", declaredField, PointsToSetLike.noFilter)
                            val fieldType = declaredField.fieldType
                            val t = if (fieldType.isObjectType) {
                                Some(fieldType.asObjectType)
                            } else None
                            jsValue = jsValue.join(java2js(propertyName, NoContext.asInstanceOf[ContextType], propertyPointsToSet, null, t)._2)
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
            }
            jsValue
        }

        def callFunctionImplementation(v: Value, methodName: String, parameters: java.util.List[Value]): Value = {

            implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

            var result = Value.makeAbsent()

            if (v.isJavaObject || v.isJSJavaTYPE) {
                v.getObjectLabels.forEach(ol => {
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
                            } //TODO merge pointsto sets
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

                            val returnValue = java2js("returnValue", context, pointsToSet, null, None)
                            result = returnValue._2.join(result)
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

        object TajsAdapterImplementation extends TajsAdapter {

            override def newObject(index: Integer, javaName: String): Value =
                newObjectImplementation(index, javaName)


            override def setProperty(v: Value, propertyName: String, rhsFieldValue: Value): Unit =
                setPropertyImplementation(v, propertyName, rhsFieldValue)

            override def readProperty(v: Value, propertyName: String): Value =
                readPropertyImplementation(v,propertyName)

            override def callFunction(v: Value, methodName: String, parameters: java.util.List[Value]): Value =
                callFunctionImplementation(v, methodName, parameters)
        }

        def createResult(analyses:                List[Analysis])
                        (implicit state: TajsConnectorState): ProperPropertyComputationResult = {
            var store: Map[PKey, Value] = Map.empty[PKey, Value]
            for (analysis <- analyses) {
                val mainFunction = analysis.getSolver.getFlowGraph.getMain
                val ordinaryExitBlock = mainFunction.getOrdinaryExit
                val exceptionalExitBlock = mainFunction.getExceptionalExit
                val analysisLatticeElement = analysis.getSolver.getAnalysisLatticeElement
                val ordinaryExitStates = analysisLatticeElement.getStates(ordinaryExitBlock)
                val exceptionalExitStates = analysisLatticeElement.getStates(exceptionalExitBlock)

                val ordinaryExistStore = ordinaryExitStates.
                    asScala.values.flatMap(_.getStore.get(globalObject).getAllProperties.asScala).
                    toMap

                val exceptionalExitStore = exceptionalExitStates.asScala.values.
                    flatMap(_.getStore.get(globalObject).getAllProperties.asScala).toMap

                var specificAnalysisStore = ordinaryExistStore.map(entry =>
                    (entry._1, entry._2.join(exceptionalExitStore.getOrElse(entry._1, Value.makeUndef())))) ++
                    exceptionalExitStore.filter(entry => !ordinaryExistStore.contains(entry._1))

                specificAnalysisStore = specificAnalysisStore.map(entry => (entry._1, entry._2.join(Value.makeUndef())))

                specificAnalysisStore.map(entry => {
                    if (!store.contains(entry._1)) {
                        store += entry
                    } else {
                        store += entry._1 -> store(entry._1).join(entry._2)
                    }
                })
            }

            Results(
                state.connectorResults,
                InterimResult(
                    scriptEngineInstance,
                    Bottom,
                    InterimAnalysisResult[PKey, Value](store),
                    state.connectorDependees,
                    c(analyses)
                )
            )
        }

        def prepareAnalysis(possibleEmptyCode: List[String], puts: Map[(String, Any, TheTACAI), (Any, Coordinator.V, Option[ObjectType])]): Unit = {
            state.code = fillEmptyCode(possibleEmptyCode)
            state.puts = puts.map(put => {
                val variableName = put._1._1
                val context = put._1._2.asInstanceOf[ContextType]
                val pointsToSet = put._2._1.asInstanceOf[PointsToSet]
                val tacai: TheTACAI = put._1._3
                val optionType = put._2._3

                val jsValue = java2js(variableName, context, pointsToSet, tacai, optionType)
                jsValue
            })
            if (state.code.size <= 4) {
                var i = 0
                state.code.toSet.subsets().foreach(subset => {
                    subset.toList.permutations.foreach(permutation => {
                        val c = permutation.mkString(";")
                        state.files ::= utility.asFile(s"JavaScript_$i", ".js", c)
                        i = i + 1
                    })
                })
            } else {
                OPALLogger.error("cross language analysis", "Too many evals!")
            }
        }

        state.tajsAdapter = TajsAdapterImplementation
        //start of analysis
        propertyStore(scriptEngineInstance, CrossLanguageInteraction.key) match {

            case ubp @ UBP(interaction @ ScriptEngineInteraction(context, Language.JavaScript, possibleEmptyCode, _, puts)) =>
                state.scriptEngineInteraction =
                    interaction.asInstanceOf[ScriptEngineInteraction[ContextType, PointsToSet]]
                prepareAnalysis(possibleEmptyCode, puts)
                val analyses = state.files.map(file => {
                    runAnalysis(file.getPath)._1.get
                })

                state.connectorDependees += ubp
                createResult(analyses)

            case ep =>
                state.connectorDependees += ep
                createResult(Nil)
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
