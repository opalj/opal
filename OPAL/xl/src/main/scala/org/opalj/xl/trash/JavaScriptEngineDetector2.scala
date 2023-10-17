/* BSD 2-Clause License - see OPAL/LICENSE for details. */
/*package org.opalj
package xl
package javaanalyses
package detector

import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.fpcf.SomePartialResult
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.InterimEUBP
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.FieldType
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisBase
import org.opalj.tac.fpcf.analyses.APIBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisState
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedAnalysis
import org.opalj.tac.fpcf.analyses.TACAIBasedAPIBasedAnalysis
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.Expr
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.cg.reflection.StringUtil
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.xl.Coordinator.V
import org.opalj.xl.detector.CrossLanguageInteraction
import org.opalj.xl.detector.ScriptEngineInteraction
import org.opalj.xl.Coordinator.ScriptEngineInstance
import org.opalj.xl.utility.Language
import JavaScriptEngineDetector.jsEngineNames
import JavaScriptEngineDetector.jsExtensions
import JavaScriptEngineDetector.jsMimetypes

import org.opalj.br.VoidType

class ScriptEngineElement()
case class GetEngine() extends ScriptEngineElement
case class Eval(receiverOption: Option[Expr[V]], code: List[String]) extends ScriptEngineElement
case class Put(receiverOption: Option[Expr[V]], varName: String, assignedVar: V) extends ScriptEngineElement
case class Get(receiverOption: Option[Expr[V]], targetVar: V, varName: String) extends ScriptEngineElement

/**
 * Detects calls of put, get, eval, on the Java ScriptEngine object.
 *
 * @author Tobias Roth
 */
abstract class JavaScriptEngineDetector(
        final val project: SomeProject
) extends PointsToAnalysisBase { self =>

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    val scriptEngine = ObjectType("javax/script/ScriptEngine")
    val engineManager = ObjectType("javax/script/ScriptEngineManager")

    trait PointsToBase extends AbstractPointsToBasedAnalysis {
        override protected[this] type ElementType = self.ElementType
        override protected[this] type PointsToSet = self.PointsToSet
        override protected[this] type DependerType = self.DependerType

        override protected[this] val pointsToPropertyKey: PropertyKey[PointsToSet] =
            self.pointsToPropertyKey

        override protected[this] def emptyPointsToSet: PointsToSet = self.emptyPointsToSet

        override protected[this] def createPointsToSet(
            pc:            Int,
            callContext:   ContextType,
            allocatedType: ReferenceType,
            isConstant:    Boolean,
            isEmptyArray:  Boolean
        ): PointsToSet = {
            self.createPointsToSet(
                pc,
                callContext.asInstanceOf[self.ContextType],
                allocatedType,
                isConstant,
                isEmptyArray
            )
        }

        @inline override protected[this] def getTypeOf(element: ElementType): ReferenceType = {
            self.getTypeOf(element)
        }

        @inline override protected[this] def getTypeIdOf(element: ElementType): Int = {
            self.getTypeIdOf(element)
        }

        @inline override protected[this] def isEmptyArray(element: ElementType): Boolean = {
            self.isEmptyArray(element)
        }
    }

    def process(p: SomeProject): PropertyComputationResult = {
        val analyses: List[APIBasedAnalysis] = List(
            new ScriptEngineAnalysis(
                project,
                declaredMethods(
                    engineManager,
                    "",
                    engineManager,
                    "getEngineByName",
                    MethodDescriptor(ObjectType.String, scriptEngine)
                )
            ) with PointsToBase,
            new ScriptEngineAnalysis(
                project,
                declaredMethods(
                    engineManager,
                    "",
                    engineManager,
                    "getEngineByExtension",
                    MethodDescriptor(ObjectType.String, scriptEngine)
                )
            ) with PointsToBase,
            new ScriptEngineAnalysis(
                project,
                declaredMethods(
                    engineManager,
                    "",
                    engineManager,
                    "getEngineByMimeType",
                    MethodDescriptor(ObjectType.String, scriptEngine)
                )
            ) with PointsToBase,
            new ScriptEngineAnalysis(
                project,
                declaredMethods(
                    scriptEngine,
                    "",
                    scriptEngine,
                    "put",
                    MethodDescriptor(ArraySeq(ObjectType.String, ObjectType.Object), VoidType)
                )
            ) with PointsToBase,
            new ScriptEngineAnalysis(
                project,
                declaredMethods(
                    scriptEngine,
                    "",
                    scriptEngine,
                    "eval",
                    MethodDescriptor(ObjectType.String, ObjectType.Object)
                )
            ) with PointsToBase,
            new ScriptEngineAnalysis(
                project,
                declaredMethods(
                    scriptEngine,
                    "",
                    scriptEngine,
                    "get",
                    MethodDescriptor(ObjectType.String, ObjectType.Object)
                )
            ) with PointsToBase,
        /*new ScriptEngineAnalysis(
                project,
                declaredMethods(
                    ObjectType.Object,
                    "",
                    ObjectType.Object,
                    "invoke",
                    MethodDescriptor(ObjectType.String, ObjectType.Object)
                )
            ) with PointsToBase */
        )
        Results(analyses.map(_.registerAPIMethod()))
    }
}

abstract class ScriptEngineAnalysis(
        final val project: SomeProject, final override val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    def c(scriptEngine: ScriptEngineElement, oldDependees: Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)])(eps: SomeEPS)(implicit state: PointsToAnalysisState[ElementType, PointsToSet, ContextType]): ProperPropertyComputationResult = {
        println(s"c $scriptEngine; $eps")
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) =>
                val newDependees = updatedDependees(eps, oldDependees)

                scriptEngine match {

                    case GetEngine() => throw new IllegalArgumentException(s"getEngine")

                    case Eval(receiverOption, code) =>
                        println("eval2")
                        val javaScriptInteraction = ScriptEngineInteraction(code = code)
                        val partialResults =
                            updateForNewestNelements(oldDependees(eps.toEPK)._1.ub.asInstanceOf[PointsToSet].numElements, ListBuffer.empty[SomePartialResult], newDependeePointsTo, javaScriptInteraction)
                        val dependees = newDependees.valuesIterator.map(_._1).toSet
                        InterimPartialResult(partialResults, dependees, c(scriptEngine, newDependees))

                    case Get(receiverOption, targetVar, varName) =>
                        println("get2")
                        val gets = Map(targetVar -> varName)
                        val javaScriptInteraction = ScriptEngineInteraction(gets = gets)
                        val partialResults =
                            updateForNewestNelements(oldDependees(eps.toEPK)._1.ub.asInstanceOf[PointsToSet].numElements, ListBuffer.empty[SomePartialResult], newDependeePointsTo, javaScriptInteraction)
                        val dependees = newDependees.valuesIterator.map(_._1).toSet
                        InterimPartialResult(partialResults, dependees, c(scriptEngine, newDependees))

                    case put @ Put(receiverOption, javaScriptVariableName, assignedVar) =>
                        println("put2")
                        val tpe = if (assignedVar.value.isPrimitiveValue)
                            assignedVar.value.asPrimitiveValue.primitiveType
                        else
                            assignedVar.value.asReferenceValue.leastUpperType.get
                        val puts = Map(javaScriptVariableName -> (tpe, Set.empty[AnyRef], None: Option[Double]))
                        val javaScriptInteraction = ScriptEngineInteraction(puts = puts)
                        val partialResults =
                            updateForNewestNelements(oldDependees(eps.toEPK)._1.ub.asInstanceOf[PointsToSet].numElements, ListBuffer.empty[SomePartialResult], newDependeePointsTo, javaScriptInteraction)
                        val dependees = newDependees.valuesIterator.map(_._1).toSet
                        InterimPartialResult(partialResults, dependees, c(scriptEngine, newDependees))

                    case _ => throw new Exception("TODO3")
                }
            case _ => throw new Exception("TODO4")
        }
    }

    override def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        callPC:          Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {

        implicit val state: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](
                callerContext,
                FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
            )

        def newScriptEnginePointsToSet: ProperPropertyComputationResult = {
            println("new scriptengine points to set")
            var partialResult: PartialResult[ScriptEngineInstance[ElementType], CrossLanguageInteraction] = null
            val newPointsToSet = createPointsToSet(
                callPC,
                callerContext,
                apiMethod.descriptor.returnType.asReferenceType,
                isConstant = false
            )

            val defSite = getDefSite(callPC)

            state.includeSharedPointsToSet(
                defSite,
                newPointsToSet,
                PointsToSetLike.noFilter
            )

            newPointsToSet.forNewestNElements(1) {
                alloc =>
                    {
                        partialResult =
                            PartialResult(ScriptEngineInstance[ElementType](alloc), CrossLanguageInteraction.key, {
                                case InterimUBP(p: ScriptEngineInteraction) =>
                                    Some(InterimEUBP(
                                        ScriptEngineInstance[ElementType](alloc),
                                        p.updated(language = Language.JavaScript)
                                    ))
                                case _: EPK[_, _] =>
                                    Some(InterimEUBP(
                                        ScriptEngineInstance[ElementType](alloc),
                                        ScriptEngineInteraction(language = Language.JavaScript)
                                    ))
                                case r =>
                                    throw new IllegalStateException(s"unexpected previous result $r")
                            })
                    }
                    partialResult
            }
            Results(
                createResults,
                InterimPartialResult(List(partialResult), Set.empty, c(GetEngine(), Map.empty))
            )
        }

        apiMethod.name match {
            case "getEngineByExtension" if (!StringUtil.getPossibleStrings(params.head.get.asVar, tac.stmts).get.
                forall(extension => jsExtensions.contains(extension.toLowerCase))) =>
                newScriptEnginePointsToSet
            case "getEngineByMimeType" if (StringUtil.getPossibleStrings(params.head.get.asVar, tac.stmts).get.
                forall(mimeType => jsMimetypes.contains(mimeType.toLowerCase))) =>
                newScriptEnginePointsToSet
            case "getEngineByName" if (StringUtil.getPossibleStrings(params.head.get.asVar, tac.stmts).get.
                forall(engineName => jsEngineNames.contains(engineName.toLowerCase))) =>
                newScriptEnginePointsToSet
            case "eval" =>
                println("eval1")
                val code = StringUtil.getPossibleStrings(params.head.get.asVar, tac.stmts).get.toList //TODO use interprocedural string analysis
                val javaScriptInteraction = ScriptEngineInteraction(code = code)
                val partialResults = updateScriptEngine(receiverOption, javaScriptInteraction)
                val dependeesMap = state.dependeesOf("")
                val dependees = dependeesMap.valuesIterator.map(_._1).toSet
                InterimPartialResult(partialResults, dependees, c(Eval(receiverOption, code), dependeesMap))

            case "put" =>
                println("put1")
                var puts = Map.empty[String, (FieldType, Set[AnyRef], Option[Double])]
                val javaScriptVariableName = StringUtil.getPossibleStrings(params.head.get.asVar, tac.stmts).get.head //TODO use sets //TODO use interprocedural string analysis
                val assignedVar = params(1).get.asVar
                val tpe = if (assignedVar.value.isPrimitiveValue)
                    assignedVar.value.asPrimitiveValue.primitiveType
                else
                    assignedVar.value.asReferenceValue.leastUpperType.get
                puts += javaScriptVariableName -> (tpe, Set.empty, None)
                val javaScriptInteraction = ScriptEngineInteraction(puts = puts)
                val partialResults = updateScriptEngine(receiverOption, javaScriptInteraction)
                val dependeesMap = state.dependeesOf("")
                val dependees = dependeesMap.valuesIterator.map(_._1).toSet
                InterimPartialResult(partialResults, dependees, c(Put(receiverOption, javaScriptVariableName, assignedVar), dependeesMap))

            case "get" =>
                println("get1")
                val varName = StringUtil.getPossibleStrings(params.head.get.asVar, tac.stmts).get.head //TODO use sets
                val targetVar = targetVarOption.get
                val gets = Map(targetVar -> varName)
                val javaScriptInteraction = ScriptEngineInteraction(gets = gets)
                val partialResults = updateScriptEngine(receiverOption, javaScriptInteraction)
                val dependeesMap = state.dependeesOf("")
                val dependees = dependeesMap.valuesIterator.map(_._1).toSet
                InterimPartialResult(partialResults, dependees, c(Get(receiverOption, targetVar, varName), dependeesMap))

            case argument => throw new IllegalArgumentException(s"$argument")
        }
    }

    private def updateScriptEngine(receiverOption: Option[Expr[V]], javaScriptInteraction: ScriptEngineInteraction)(implicit state: State): ListBuffer[SomePartialResult] = {
        receiverOption.get.asVar.definedBy.foldLeft(ListBuffer.empty[SomePartialResult])(
            (list, defSite) => {
                val allocations = currentPointsToOfDefSite("", defSite)
                updateForNewestNelements(0, list, allocations, javaScriptInteraction)
            }
        )
    }
    private[this] def updateForNewestNelements(seenElements: Int, list: ListBuffer[SomePartialResult], allocations: PointsToSet, javaScriptInteraction: ScriptEngineInteraction): ListBuffer[SomePartialResult] = {
        allocations.forNewestNElements(allocations.numElements - seenElements) {
            alloc =>
                list += PartialResult[ScriptEngineInstance[ElementType], CrossLanguageInteraction](Coordinator.ScriptEngineInstance(alloc), CrossLanguageInteraction.key, {
                    case InterimUBP(p: ScriptEngineInteraction) => Some(InterimEUBP(Coordinator.ScriptEngineInstance(alloc), p.update(javaScriptInteraction)))
                    case _: EPK[_, _]                           => Some(InterimEUBP(Coordinator.ScriptEngineInstance(alloc), javaScriptInteraction))
                    case r =>
                        throw new IllegalStateException(s"unexpected previous result $r")
                })
        }
        list
    }
}

trait JavaScriptEngineDetectorScheduler extends BasicFPCFEagerAnalysisScheduler {
    def propertyKind: PropertyMetaInformation
    def createAnalysis: SomeProject => JavaScriptEngineDetector

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, DefinitionSitesKey, TypeIteratorKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callees, propertyKind)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(propertyKind)

    override def derivesEagerly: Set[PropertyBounds] = PropertyBounds.ubs(CrossLanguageInteraction) //Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = createAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }
}

object TypeBasedApiJavaScriptEngineDetectorSchedulerScheduler extends JavaScriptEngineDetectorScheduler {
    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => JavaScriptEngineDetector =
        new JavaScriptEngineDetector(_) with TypeBasedAnalysis
}

object AllocationSiteBasedApiJavaScriptEngineDetectorScheduler extends JavaScriptEngineDetectorScheduler {
    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => JavaScriptEngineDetector =
        new JavaScriptEngineDetector(_) with AllocationSiteBasedAnalysis
}

object JavaScriptEngineDetector {
    val jsEngineNames = Set("nashorn", "rhino", "js", "javascript", "ecmascript", "graal.js")
    val jsExtensions = Set("js")
    val jsMimetypes = Set("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript")
}
*/ 