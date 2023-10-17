/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package javaanalyses
package detector
package scriptengine

import org.opalj.xl.detector.CrossLanguageInteraction
import org.opalj.xl.detector.ScriptEngineInteraction
import org.opalj.xl.utility.Language
import org.opalj.xl.utility.Language.Language
import org.opalj.xl.Coordinator.ScriptEngineInstance
import org.opalj.xl.Coordinator.V

import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisBase
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.TACAIBasedAPIBasedAnalysis
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.reflection.StringUtil
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.tac.Expr
import org.opalj.tac.fpcf.analyses.cg.AllocationsUtil
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisState

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

        println("alloc analysis process new caller")

        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
            new PointsToAnalysisState(callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac)))

        implicit val typeIteratorState: TypeIteratorState = new BaseAnalysisState with TypeIteratorState

        if (params.isEmpty || params.head.isEmpty)
            return Results(); // Unknown engine string parameter

        val newPointsToSet = createPointsToSet(
            callPC, callerContext, apiMethod.descriptor.returnType.asReferenceType, isConstant = false
        )

        pointsToAnalysisState.includeSharedPointsToSet(getDefSite(callPC), newPointsToSet)

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
                InterimPartialResult(typeIteratorState.dependees, c(instance, None, callerContext, engineString, tac.stmts))
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
        val engineInteraction = ScriptEngineInteraction[ContextType, PointsToSet](language = language.get)

        val partialResult = PartialResult[ScriptEngineInstance[ElementType], CrossLanguageInteraction](
            instance,
            CrossLanguageInteraction.key, {
                case _: EPK[_, _] => Some(InterimEUBP(instance, engineInteraction))
                case InterimUBP(oldEngineInteraction: ScriptEngineInteraction[_, _]) =>
                    Some(InterimEUBP(instance, oldEngineInteraction.asInstanceOf[ScriptEngineInteraction[ContextType, PointsToSet]].updated(engineInteraction)))
                case r => throw new IllegalStateException(s"unexpected previous result $r")
            }
        )

        val dependees = if (language.contains(Language.Unknown)) Set.empty[SomeEOptionP] else state.dependees

        InterimPartialResult(
            Iterable(partialResult), dependees, c(instance, language, context, engineString, stmts)
        )
    }

    def c(instance: ScriptEngineInstance[ElementType], language: Option[Language],
          context: ContextType, engineString: V, stmts: Array[Stmt[V]])(eps: SomeEPS)(implicit state: TypeIteratorState): ProperPropertyComputationResult = {
        println(s"alloc analysis continuation")
        var resultLanguage = language
        AllocationsUtil.continuationForAllocation[None.type, ContextType](
            eps, context, _ => (engineString, stmts), _ => true, _ => {
            resultLanguage = Some(Language.Unknown)
        }
        ) { (_, _, allocationIndex, stmts) =>
            val newLanguage = StringUtil.getString(allocationIndex, stmts).flatMap {
                engineStrings.get
            }
            if (newLanguage.isEmpty || newLanguage != resultLanguage)
                resultLanguage = Some(Language.Unknown)
        }
        createInstanceResult(instance, resultLanguage, context, engineString, stmts)
    }
}
