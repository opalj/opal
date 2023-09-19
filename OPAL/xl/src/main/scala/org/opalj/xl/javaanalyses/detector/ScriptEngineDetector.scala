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
import ScriptEngineDetector.engineNames
import ScriptEngineDetector.extensions
import ScriptEngineDetector.mimetypes
import org.opalj.xl.utility.Language.Language

import org.opalj.br.VoidType


/**
 * Detects calls of put, get, eval, on the Java ScriptEngine object.
 *
 * @author Tobias Roth
 * @author Dominik Helm
 */
abstract class ScriptEngineDetector(final val project: SomeProject) extends PointsToAnalysisBase {
    self =>

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    val scriptEngine = ObjectType("javax/script/ScriptEngine")
    val engineManager = ObjectType("javax/script/ScriptEngineManager")
    val getEngineDescriptor = MethodDescriptor(ObjectType.String, scriptEngine)

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
                project,
                declaredMethods(engineManager, "", engineManager, "getEngineByName", getEngineDescriptor),
                engineNames
            ) with PointsToBase,
            new ScriptEngineAllocationAnalysis(
                project,
                declaredMethods(engineManager, "", engineManager, "getEngineByExtension", getEngineDescriptor),
                extensions
            ) with PointsToBase,
            new ScriptEngineAllocationAnalysis(
                project,
                declaredMethods(engineManager, "", engineManager, "getEngineByMimeType", getEngineDescriptor),
                mimetypes
            ) with PointsToBase,
            new ScriptEngineInteractionAnalysis(
                project,
                declaredMethods(
                    scriptEngine, "", scriptEngine, "put",
                    MethodDescriptor(ArraySeq(ObjectType.String, ObjectType.Object), VoidType)
                )
            ) with PointsToBase,
            new ScriptEngineInteractionAnalysis(
                project,
                declaredMethods(
                    scriptEngine, "", scriptEngine, "eval", MethodDescriptor(ObjectType.String, ObjectType.Object)
                )
            ) with PointsToBase,
            new ScriptEngineInteractionAnalysis(
                project,
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

abstract class ScriptEngineAllocationAnalysis(
        final val project: SomeProject,
        final override val apiMethod: DeclaredMethod,
        final val engineStrings: Map[String, Language]
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

        implicit val state: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
            new PointsToAnalysisState(callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac)))

        val possibleStrings = StringUtil.getPossibleStrings(params.head.get.asVar, tac.stmts)
        if (possibleStrings.isEmpty)
            return Results(); // Unknown language

        val language = engineStrings.get(possibleStrings.get.head.toLowerCase)
        if(language.isEmpty || possibleStrings.get.tail.exists(s => engineStrings.get(s.toLowerCase) != language))
            return Results(); // Different or unknown possible languages

        val newPointsToSet = createPointsToSet(
            callPC,
            callerContext,
            apiMethod.descriptor.returnType.asReferenceType,
            isConstant = false
        )
        state.includeSharedPointsToSet(getDefSite(callPC), newPointsToSet)

        val instance = ScriptEngineInstance(newPointsToSet.getNewestElement())
        val engineInteraction = ScriptEngineInteraction(language = language.get)

        val partialResult = PartialResult(instance, CrossLanguageInteraction.key, {
            case _: EPK[_, _] => Some(InterimEUBP(instance, engineInteraction))
            case r            => throw new IllegalStateException(s"unexpected previous result $r")
        })

        Results(
            createResults,
            InterimPartialResult(Iterable(partialResult), Set.empty, c(engineInteraction, Map.empty))
        )
    }
}

abstract class ScriptEngineInteractionAnalysis(
        final val project: SomeProject, final override val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    def c(
        engineInteraction: ScriptEngineInteraction,
        oldDependees:      Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]
    )(
        eps: SomeEPS
    )(
        implicit
        state: PointsToAnalysisState[ElementType, PointsToSet, ContextType]
    ): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) =>
                val newDependees = updatedDependees(eps, oldDependees)
                val oldDependee = oldDependees(eps.toEPK)._1.ub.asInstanceOf[PointsToSet]
                val results =
                    resultsForScriptEngineAllocations(engineInteraction, newDependeePointsTo, oldDependee.numElements)
                val dependees = newDependees.valuesIterator.map(_._1).toSet
                InterimPartialResult(results, dependees, c(engineInteraction, newDependees))

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
            new PointsToAnalysisState(callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac)))

        if (params.head.isEmpty || apiMethod.name == "put" && params(1).isEmpty)
            return Results(); // Cannot determine any result here

        val engineInteraction = apiMethod.name match {
            case "eval" =>
                val codes = StringUtil.getPossibleStrings(params.head.get.asVar, tac.stmts) //TODO use interprocedural string analysis
                ScriptEngineInteraction(code = codes.getOrElse(List.empty).toList)

            case "put" =>
                val foreignVariableNames = StringUtil.getPossibleStrings(params.head.get.asVar, tac.stmts) //TODO use interprocedural string analysis
                val assignedVar = params(1).get.asVar
                val tpe = if (assignedVar.value.isPrimitiveValue)
                    assignedVar.value.asPrimitiveValue.primitiveType
                else
                    assignedVar.value.asReferenceValue.leastUpperType.get
                val puts = Map.from(foreignVariableNames.flatMap(_.map(_ -> (tpe, Set.empty[AnyRef], None))))
                ScriptEngineInteraction(puts = puts)

            case "get" =>
                val foreignVariableNames = StringUtil.getPossibleStrings(params.head.get.asVar, tac.stmts) //TODO use interprocedural string analysis
                val targetVar = targetVarOption.get
                val gets = Map.from(foreignVariableNames.flatMap(_.map(targetVar -> _)))
                ScriptEngineInteraction(gets = gets)

            case argument => throw new IllegalArgumentException(s"$argument")
        }

        val partialResults = resultsForScriptEngine(engineInteraction, receiverOption)
        val dependeesMap = state.dependeesOf(None)
        val dependees = dependeesMap.valuesIterator.map(_._1).toSet
        InterimPartialResult(partialResults, dependees, c(engineInteraction, dependeesMap))
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

object ScriptEngineDetector {
    val engineNames = Map.from(
        Set("nashorn", "rhino", "js", "javascript", "ecmascript", "graal.js").map(_ -> Language.JavaScript)
    )

    val extensions = Map.from(
        Set("js").map(_ -> Language.JavaScript)
    )

    val mimetypes = Map.from(
        Set("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript").map(_ -> Language.JavaScript)
    )
}
