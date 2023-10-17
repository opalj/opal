/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package javaanalyses
package detector
package scriptengine

import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value
import org.opalj.xl.translator.JavaJavaScriptTranslator
import org.opalj.xl.utility.InterimAnalysisResult
import org.opalj.xl.Coordinator
import org.opalj.xl.utility.AnalysisResult
import org.opalj.xl.Coordinator.V
import org.opalj.xl.utility.FinalAnalysisResult

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomePartialResult
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.SimpleContext
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

abstract class ScriptEngineInteractionAnalysisGet(
        final val project:            SomeProject,
        final override val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    def java2js = JavaJavaScriptTranslator.Java2JavaScript[PointsToSet, ContextType]

    def js2java = JavaJavaScriptTranslator.JavaScript2Java[PointsToSet, ContextType]

    def c(
        receiverOption:   Option[Expr[V]],
        callIndex:        Int,
        possibleStrings:  Set[String],
        targetVarDefSite: Entity,
        context:          ContextType,
        param:            V,
        stmts:            Array[Stmt[V]]
    )(eps: SomeEPS)(implicit typeIteratorState: TypeIteratorState, pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType]): ProperPropertyComputationResult = {
        val epk = eps.toEPK

        var dependees: Set[SomeEOptionP] = Set.empty

        eps match {
            case InterimUBP(FinalAnalysisResult(store)) =>
                val possibleValues = possibleStrings.map(variableName => store.asInstanceOf[Map[PKey, Value]]
                    .getOrElse(PKey.StringPKey.make(variableName), Value.makeUndef()))
                val pointsToSetSet = js2java(possibleValues)._2

                pointsToSetSet.foreach { pointsToSet =>
                    pointsToAnalysisState.includeSharedPointsToSet(
                        targetVarDefSite, pointsToSet
                    )
                }

            case eps => dependees += eps

        }

        if (typeIteratorState.hasDependee(epk)) {
            AllocationsUtil.continuationForAllocation[None.type, ContextType](
                eps, context, _ => (param, stmts), _ => true, _ => {
                throw new Exception("TODO: What to do if param is unknown?")
            }
            ) { (_, _, allocationIndex, stmts) =>
                val newParam = StringUtil.getString(allocationIndex, stmts)
                if (newParam.isEmpty)
                    throw new Exception("TODO: What to do if param is unknown?")

                receiverOption.get.asVar.definedBy.foreach(defSite => {
                    val allocations = currentPointsToOfDefSite("getTarget", defSite)
                    allocations.forNewestNElements(allocations.numElements) { alloc =>
                        {
                            val instance = Coordinator.ScriptEngineInstance(alloc)
                            propertyStore(instance, AnalysisResult.key) match {
                                case InterimUBP(InterimAnalysisResult(store)) =>
                                    val possibleValues =
                                        possibleStrings.map(variableName => store.asInstanceOf[Map[PKey, Value]]
                                            .getOrElse(PKey.StringPKey.make(variableName), Value.makeUndef()))

                                    val (referenceTypes, pointsToSetSet, jsNodes) = js2java(possibleValues)

                                    this.createPointsToSet(-100 - jsNodes.head.getIndex, SimpleContext.asInstanceOf[ContextType], referenceTypes.head, false, false)

                                    pointsToSetSet.foreach { pointsToSet =>
                                        pointsToAnalysisState.includeSharedPointsToSet(
                                            targetVarDefSite, pointsToSet
                                        )
                                    }
                                case eps => dependees += eps
                            }
                        }
                    }
                })
            }

            typeIteratorState.updateDependency(eps)
        }

        dependees = typeIteratorState.dependees ++ dependees

        Results(InterimPartialResult(
            List.empty[SomePartialResult], // results,
            dependees,
            c(
                receiverOption,
                callIndex, //TODO ?
                possibleStrings,
                targetVarDefSite,
                context,
                param,
                stmts
            )
        ), createResults)
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

        if (params.head.isEmpty)
            return Results(); // Cannot determine any result here

        val param = params.head.get.asVar
        val possibleStrings =
            StringUtil.getPossibleStrings(param, callerContext, None, tac.stmts, () => {
                throw new Exception("TODO: What to do if param is unknown?")
            })

        var dependees: Set[SomeEOptionP] = Set.empty

        val targetVarDefSite = getDefSite(callPC)
        receiverOption.get.asVar.definedBy.foreach(defSite => {
            val allocations = currentPointsToOfDefSite("getTarget", defSite)
            allocations.forNewestNElements(allocations.numElements) { alloc =>
                {
                    val instance = Coordinator.ScriptEngineInstance(alloc)
                    propertyStore(instance, AnalysisResult.key) match {
                        case InterimUBP(InterimAnalysisResult(store)) =>
                            val possibleValues =
                                possibleStrings.map(variableName => store.asInstanceOf[Map[PKey, Value]]
                                    .getOrElse(PKey.StringPKey.make(variableName), Value.makeUndef()))

                            val pointsToSetSet = js2java(possibleValues)._2

                            pointsToSetSet.foreach { pointsToSet =>
                                pointsToAnalysisState.includeSharedPointsToSet(
                                    targetVarDefSite, pointsToSet
                                )
                            }
                        case eps => dependees += eps
                    }
                }
            }
        })

        Results(createResults, InterimPartialResult(List.empty[SomePartialResult], dependees, c(
            receiverOption,
            callPC, possibleStrings, targetVarDefSite,
            callerContext, param, tac.stmts
        )))
    }
}
