/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package javaanalyses
package detector
package scriptengine

import org.opalj.xl.detector.ScriptEngineInteraction
import org.opalj.xl.Coordinator.ScriptEngineInstance
import org.opalj.xl.detector.CrossLanguageInteraction
import org.opalj.xl.Coordinator
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
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomePartialResult
import org.opalj.fpcf.UBP
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.analyses.cg.AllocationsUtil
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisBase
import org.opalj.tac.Expr
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisState
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.TACAIBasedAPIBasedAnalysis
import org.opalj.tac.fpcf.analyses.cg.reflection.StringUtil

abstract class ScriptEngineInteractionAnalysisEval(
        final val project:            SomeProject,
        final override val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    def c(
        callIndex:          Int,
        engineInteraction:  ScriptEngineInteraction[ContextType, PointsToSet],
        oldEngineDependees: Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)],
        context:            ContextType,
        param:              V,
        stmts:              Array[Stmt[V]]
    )(eps: SomeEPS)(implicit
        typeIteratorState: TypeIteratorState,
                    pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType]
    ): ProperPropertyComputationResult = {

        println(s"eval continuation $eps")

        var results = List.empty[SomePartialResult]
        val epk = eps.toEPK
        var newEngineInteraction = engineInteraction

        if (typeIteratorState.hasDependee(epk)) {
            AllocationsUtil.continuationForAllocation[Set[String] => ScriptEngineInteraction[ContextType, PointsToSet], ContextType](
                eps, context, _ => (param, stmts), _ => true, _ => {
                throw new Exception("TODO: What to do if param is unknown?")
            }
            ) { (_, _, allocationIndex, stmts) =>
                val newParam = StringUtil.getString(allocationIndex, stmts)
                if (newParam.isEmpty)
                    throw new Exception("TODO: What to do if param is unknown?")

                newEngineInteraction = newEngineInteraction.updated(ScriptEngineInteraction(code = List(newParam.get)))
            }

            assert(newEngineInteraction ne engineInteraction)

            oldEngineDependees.valuesIterator.foreach { data =>
                val engineAllocations = data._1.ub.asInstanceOf[PointsToSet]
                results :::= resultsForScriptEngineAllocations(newEngineInteraction, engineAllocations, 0)
            }

            typeIteratorState.updateDependency(eps)
        }

        val newEngineDependees = if (oldEngineDependees.contains(epk)) {
            val UBP(newPointsTo: PointsToSet @unchecked) = eps
            val oldDependee = oldEngineDependees(epk)._1.ub.asInstanceOf[PointsToSet]
            results :::= resultsForScriptEngineAllocations(newEngineInteraction, newPointsTo, oldDependee.numElements)
            updatedDependees(eps, oldEngineDependees)
        } else oldEngineDependees

        val dependees = typeIteratorState.dependees ++
            newEngineDependees.valuesIterator.map(_._1).toSet

        InterimPartialResult(
            results,
            dependees,
            c(
                callIndex,
                newEngineInteraction,
                newEngineDependees,
                context,
                param,
                stmts
            )
        )
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

        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
            new PointsToAnalysisState(callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac)))

        implicit val typeIteratorState: TypeIteratorState = new BaseAnalysisState with TypeIteratorState

        val param = params.head.get.asVar
        val possibleStrings =
            StringUtil.getPossibleStrings(param, callerContext, None, tac.stmts, () => {
                throw new Exception("TODO: What to do if param is unknown?")
            })

        var dependees: Set[SomeEOptionP] = Set.empty

        val scriptEngineInteraction = ScriptEngineInteraction[ContextType, PointsToSet](code = possibleStrings.toList)

        val partialResults = resultsForScriptEngine("engine", scriptEngineInteraction, receiverOption)

        val engineDependeesMap =
            if (pointsToAnalysisState.hasDependees("engine"))
                pointsToAnalysisState.dependeesOf("engine")
            else
                Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

        dependees = dependees ++ typeIteratorState.dependees ++ engineDependeesMap.valuesIterator.map(_._1)

        Results(
            createResults,
            InterimPartialResult(partialResults, dependees, c(callPC, scriptEngineInteraction,
                engineDependeesMap, callerContext, param, tac.stmts))
        )
    }

    private[this] def resultsForScriptEngine(
        depender:              String,
        javaScriptInteraction: ScriptEngineInteraction[ContextType, PointsToSet],
        receiverOption:        Option[Expr[V]]
    )(implicit state: State): List[SomePartialResult] =
        receiverOption.get.asVar.definedBy.foldLeft(List.empty[SomePartialResult])(
            (results, defSite) => {
                val allocations = currentPointsToOfDefSite(depender, defSite)
                var instances = List.empty[ScriptEngineInstance[ElementType]]
                allocations.forNewestNElements(allocations.numElements) {
                    alloc => instances = Coordinator.ScriptEngineInstance(alloc) :: instances
                }
                resultsForScriptEngineAllocations(javaScriptInteraction, allocations, 0) ::: results
            }
        )

    private[this] def resultsForScriptEngineAllocations(
        engineInteraction: ScriptEngineInteraction[ContextType, PointsToSet],
        allocations:       PointsToSet,
        seenElements:      Int
    ): List[SomePartialResult] = {
        var results = List.empty[SomePartialResult]
        allocations.forNewestNElements(allocations.numElements - seenElements) { alloc =>
            val instance = Coordinator.ScriptEngineInstance(alloc)
            results ::= PartialResult[ScriptEngineInstance[ElementType], CrossLanguageInteraction](
                instance, CrossLanguageInteraction.key, {
                case InterimUBP(p: ScriptEngineInteraction[_, _]) =>
                    Some(InterimEUBP(instance, p.asInstanceOf[ScriptEngineInteraction[ContextType, PointsToSet]].
                        updated(engineInteraction)))
                case _: EPK[_, _] =>
                    Some(InterimEUBP(instance, engineInteraction))
                case result =>
                    throw new IllegalStateException(s"unexpected previous result $result")
            }
            )
        }
        results
    }
}
