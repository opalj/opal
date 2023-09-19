/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package javaanalyses
package detector

import scala.collection.immutable.ArraySeq

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
import ScriptEngineDetector.engineNames
import ScriptEngineDetector.extensions
import ScriptEngineDetector.mimetypes
import org.opalj.xl.javaanalyses.detector.ScriptEngineDetector.engineManager
import org.opalj.xl.javaanalyses.detector.ScriptEngineDetector.getEngine
import org.opalj.xl.javaanalyses.detector.ScriptEngineDetector.scriptEngine
import org.opalj.xl.utility.Language.Language

import org.opalj.fpcf.EPK
import org.opalj.br.VoidType
import org.opalj.tac.fpcf.analyses.cg.AllocationsUtil
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.Stmt

/**
 * Detects calls of put, get, eval, on the Java ScriptEngine object.
 *
 * @author Tobias Roth
 * @author Dominik Helm
 */
abstract class ScriptEngineDetector( final val project: SomeProject) extends PointsToAnalysisBase {
    self =>

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

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
            new ScriptEngineAllocationAnalysis(
                p, declaredMethods(engineManager, "", engineManager, "getEngineByName", getEngine), engineNames
            ) with PointsToBase,
            new ScriptEngineAllocationAnalysis(
                p, declaredMethods(engineManager, "", engineManager, "getEngineByExtension", getEngine), extensions
            ) with PointsToBase,
            new ScriptEngineAllocationAnalysis(
                p, declaredMethods(engineManager, "", engineManager, "getEngineByMimeType", getEngine), mimetypes
            ) with PointsToBase,
            new ScriptEngineInteractionAnalysis(
                p,
                declaredMethods(
                    scriptEngine, "", scriptEngine, "put",
                    MethodDescriptor(ArraySeq(ObjectType.String, ObjectType.Object), VoidType)
                )
            ) with PointsToBase,
            new ScriptEngineInteractionAnalysis(
                p,
                declaredMethods(
                    scriptEngine, "", scriptEngine, "eval", MethodDescriptor(ObjectType.String, ObjectType.Object)
                )
            ) with PointsToBase,
            new ScriptEngineInteractionAnalysis(
                p,
                declaredMethods(
                    scriptEngine, "", scriptEngine, "get",
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

object ScriptEngineDetector {
    val scriptEngine: ObjectType = ObjectType("javax/script/ScriptEngine")
    val engineManager: ObjectType = ObjectType("javax/script/ScriptEngineManager")
    val getEngine: MethodDescriptor = MethodDescriptor(ObjectType.String, scriptEngine)

    val engineNames: Map[String, Language] = Map.from(
        Set("nashorn", "rhino", "js", "javascript", "ecmascript", "graal.js").map(_ -> Language.JavaScript)
    )

    val extensions: Map[String, Language] = Map.from(
        Set("js").map(_ -> Language.JavaScript)
    )

    val mimetypes: Map[String, Language] = Map.from(
        Set("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript").map(_ -> Language.JavaScript)
    )
}

abstract class ScriptEngineAllocationAnalysis(
        final val project:            SomeProject,
        final override val apiMethod: DeclaredMethod,
        final val engineStrings:      Map[String, Language]
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

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

        implicit val ptState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
            new PointsToAnalysisState(callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac)))
        implicit val tiState: TypeIteratorState = new BaseAnalysisState with TypeIteratorState

        if (params.isEmpty || params.head.isEmpty)
            return Results(); // Unknown engine string parameter

        val newPointsToSet = createPointsToSet(
            callPC, callerContext, apiMethod.descriptor.returnType.asReferenceType, isConstant = false
        )
        ptState.includeSharedPointsToSet(getDefSite(callPC), newPointsToSet)

        val instance = ScriptEngineInstance(newPointsToSet.getNewestElement())

        val engineString = params.head.get.asVar
        val possibleStrings = StringUtil.getPossibleStrings(engineString, callerContext, None, tac.stmts, () => {
            return Results(
                createResults,
                createInstanceResult(instance, Some(Language.Unknown), callerContext, engineString, tac.stmts)
            );
        })

        if (possibleStrings.isEmpty)
            return Results(
                createResults,
                InterimPartialResult(tiState.dependees, c(instance, None, callerContext, engineString, tac.stmts))
            ); // No language known yet

        var language = engineStrings.get(possibleStrings.head.toLowerCase)
        if (language.isEmpty || possibleStrings.tail.exists(s => engineStrings.get(s.toLowerCase) != language))
            language = Some(Language.Unknown)

        Results(createResults, createInstanceResult(instance, language, callerContext, engineString, tac.stmts))
    }

    private[this] def createInstanceResult(
        instance:     ScriptEngineInstance[ElementType],
        language:     Option[Language],
        context:      ContextType,
        engineString: V,
        stmts:        Array[Stmt[V]]
    )(implicit state: TypeIteratorState): ProperPropertyComputationResult = {
        val engineInteraction = ScriptEngineInteraction(language = language.get)

        val partialResult = PartialResult[ScriptEngineInstance[ElementType], CrossLanguageInteraction](
            instance,
            CrossLanguageInteraction.key, {
                case _: EPK[_, _] => Some(InterimEUBP(instance, engineInteraction))
                case r            => throw new IllegalStateException(s"unexpected previous result $r")
            }
        )

        val dependees = if (language.contains(Language.Unknown)) Set.empty[SomeEOptionP] else state.dependees

        InterimPartialResult(
            Iterable(partialResult), dependees, c(instance, language, context, engineString, stmts)
        )
    }

    def c(
        instance:     ScriptEngineInstance[ElementType],
        language:     Option[Language],
        context:      ContextType,
        engineString: V,
        stmts:        Array[Stmt[V]]
    )(eps: SomeEPS)(implicit state: TypeIteratorState): ProperPropertyComputationResult = {
        var resultLanguage = language
        AllocationsUtil.continuationForAllocation[None.type, ContextType](
            eps, context, _ => (engineString, stmts), _ => true, _ => { resultLanguage = Some(Language.Unknown) }
        ) { (_, _, allocationIndex, stmts) =>
            val newLanguage = StringUtil.getString(allocationIndex, stmts).flatMap { engineStrings.get }
            if (newLanguage.isEmpty || newLanguage != resultLanguage)
                resultLanguage = Some(Language.Unknown)
        }
        createInstanceResult(instance, resultLanguage, context, engineString, stmts)
    }
}

abstract class ScriptEngineInteractionAnalysis(
        final val project: SomeProject, final override val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    def c(
        engineInteraction:  ScriptEngineInteraction,
        oldEngineDependees: Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)],
        context:            ContextType,
        param:              V,
        stmts:              Array[Stmt[V]]
    )(eps: SomeEPS)(implicit tiState: TypeIteratorState): ProperPropertyComputationResult = {
        var results = List.empty[SomePartialResult]
        val epk = eps.toEPK

        var newEngineInteraction = engineInteraction

        if (tiState.hasDependee(epk)) {
            AllocationsUtil.continuationForAllocation[Set[String] => ScriptEngineInteraction, ContextType](
                eps, context, _ => (param, stmts), _ => true, _ => {
                throw new Exception("TODO: What to do if param is unknown?")
            }
            ) { (newInteraction, _, allocationIndex, stmts) =>
                val newParam = StringUtil.getString(allocationIndex, stmts)
                if (newParam.isEmpty)
                    throw new Exception("TODO: What to do if param is unknown?")
                newEngineInteraction = newEngineInteraction.updated(newInteraction(Set(newParam.get)))
            }

            assert(newEngineInteraction ne engineInteraction)

            oldEngineDependees.valuesIterator.foreach { data =>
                val engineAllocations = data._1.ub.asInstanceOf[PointsToSet]
                results :::= resultsForScriptEngineAllocations(newEngineInteraction, engineAllocations, 0)
            }

            tiState.updateDependency(eps)
        }

        val newEngineDependees = if (oldEngineDependees.contains(epk)) {
            val UBP(newPointsTo: PointsToSet @unchecked) = eps
            val oldDependee = oldEngineDependees(epk)._1.ub.asInstanceOf[PointsToSet]
            results :::= resultsForScriptEngineAllocations(newEngineInteraction, newPointsTo, oldDependee.numElements)
            updatedDependees(eps, oldEngineDependees)
        } else oldEngineDependees

        val dependees = tiState.dependees ++ newEngineDependees.valuesIterator.map(_._1).toSet
        InterimPartialResult(results, dependees, c(newEngineInteraction, newEngineDependees, context, param, stmts))
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

        implicit val ptState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
            new PointsToAnalysisState(callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac)))

        implicit val tiState: TypeIteratorState = new BaseAnalysisState with TypeIteratorState

        if (params.head.isEmpty || apiMethod.name == "put" && params(1).isEmpty)
            return Results(); // Cannot determine any result here

        val interactionCtor = apiMethod.name match {
            case "eval" =>
                (s: Set[String]) => ScriptEngineInteraction(code = s.toList)

            case "put" =>
                (s: Set[String]) => {
                    val assignedVar = params(1).get.asVar
                    val tpe = if (assignedVar.value.isPrimitiveValue)
                        assignedVar.value.asPrimitiveValue.primitiveType
                    else
                        assignedVar.value.asReferenceValue.leastUpperType.get
                    val value = (tpe, Set.empty[AnyRef], None)
                    ScriptEngineInteraction(puts = Map.from(s.map(_ -> value)))
                }

            case "get" =>
                (s: Set[String]) => {
                    val targetVar = targetVarOption.get
                    ScriptEngineInteraction(gets = Map.from(s.map(targetVar -> _)))
                }

            case argument => throw new IllegalArgumentException(s"$argument")
        }

        val param = params.head.get.asVar
        val possibleStrings = StringUtil.getPossibleStrings(param, callerContext, interactionCtor, tac.stmts, () => {
            throw new Exception("TODO: What to do if param is unknown?")
        })

        val engineInteraction = interactionCtor(possibleStrings)

        val partialResults = resultsForScriptEngine(engineInteraction, receiverOption)
        val dependeesMap = ptState.dependeesOf(interactionCtor)
        val dependees = tiState.dependees ++ dependeesMap.valuesIterator.map(_._1)

        InterimPartialResult(
            partialResults, dependees, c(engineInteraction, dependeesMap, callerContext, param, tac.stmts)
        )
    }

    private[this] def resultsForScriptEngine(
        javaScriptInteraction: ScriptEngineInteraction,
        receiverOption:        Option[Expr[V]]
    )(implicit state: State): List[SomePartialResult] = {
        receiverOption.get.asVar.definedBy.foldLeft(List.empty[SomePartialResult])(
            (results, defSite) => {
                val allocations = currentPointsToOfDefSite(None, defSite)
                resultsForScriptEngineAllocations(javaScriptInteraction, allocations, 0) ::: results
            }
        )
    }

    private[this] def resultsForScriptEngineAllocations(
        engineInteraction: ScriptEngineInteraction,
        allocations:       PointsToSet,
        seenElements:      Int
    ): List[SomePartialResult] = {
        var results = List.empty[SomePartialResult]
        allocations.forNewestNElements(allocations.numElements - seenElements) { alloc =>
            val instance = Coordinator.ScriptEngineInstance(alloc)
            results ::= PartialResult[ScriptEngineInstance[ElementType], CrossLanguageInteraction](
                instance, CrossLanguageInteraction.key, {
                case InterimUBP(p: ScriptEngineInteraction) =>
                    Some(InterimEUBP(instance, p.updated(engineInteraction)))
                case _: EPK[_, _] =>
                    Some(InterimEUBP(instance, engineInteraction))
                case r =>
                    throw new IllegalStateException(s"unexpected previous result $r")
            }
            )
        }
        results
    }
}

trait ScriptEngineDetectorScheduler extends BasicFPCFEagerAnalysisScheduler {
    def propertyKind: PropertyMetaInformation

    def createAnalysis: SomeProject => ScriptEngineDetector

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, DefinitionSitesKey, TypeIteratorKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callees, propertyKind)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(propertyKind, CrossLanguageInteraction)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = createAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }
}

object TypeBasedApiScriptEngineDetectorSchedulerScheduler extends ScriptEngineDetectorScheduler {
    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => ScriptEngineDetector =
        new ScriptEngineDetector(_) with TypeBasedAnalysis
}

object AllocationSiteBasedApiScriptEngineDetectorScheduler extends ScriptEngineDetectorScheduler {
    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => ScriptEngineDetector =
        new ScriptEngineDetector(_) with AllocationSiteBasedAnalysis
}
