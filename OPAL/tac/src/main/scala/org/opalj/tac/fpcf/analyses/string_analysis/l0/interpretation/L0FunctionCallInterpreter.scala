/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.Method
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Processes [[NonVirtualFunctionCall]]s without a call graph.
 *
 * @author Maximilian Rüsch
 */
trait L0FunctionCallInterpreter[State <: L0ComputationState]
    extends L0StringInterpreter[State]
    with ParameterEvaluatingStringInterpreter[State] {

    override type T <: FunctionCall[V]

    implicit val ps: PropertyStore

    protected[this] case class FunctionCallState(
        defSitePC:           Int,
        calleeMethods:       Seq[Method],
        var tacDependees:    Map[Method, EOptionP[Method, TACAI]],
        var returnDependees: Map[Method, Seq[EOptionP[SContext, StringConstancyProperty]]] = Map.empty
    ) {
        var hasUnresolvableReturnValue: Map[Method, Boolean] = Map.empty.withDefaultValue(false)

        private var _paramDependees: Seq[Seq[EOptionP[DefSiteEntity, StringConstancyProperty]]] = Seq.empty
        private var _paramEntityToPositionMapping: Map[DefSiteEntity, (Int, Int)] = Map.empty

        def paramDependees: Seq[Seq[EOptionP[DefSiteEntity, StringConstancyProperty]]] = _paramDependees

        def updateParamDependee(newDependee: EOptionP[DefSiteEntity, StringConstancyProperty]): Unit = {
            val pos = _paramEntityToPositionMapping(newDependee.e)
            _paramDependees = _paramDependees.updated(
                pos._1,
                _paramDependees(pos._1).updated(
                    pos._2,
                    newDependee
                )
            )
        }

        def setParamDependees(newParamDependees: Seq[Seq[EOptionP[DefSiteEntity, StringConstancyProperty]]]): Unit = {
            _paramDependees = newParamDependees
            _paramEntityToPositionMapping = Map.empty
            _paramDependees.zipWithIndex.map {
                case (param, outerIndex) => param.zipWithIndex.map {
                        case (dependee, innerIndex) =>
                            _paramEntityToPositionMapping += dependee.e -> (outerIndex, innerIndex)
                    }
            }
        }

        def updateReturnDependee(method: Method, newDependee: EOptionP[SContext, StringConstancyProperty]): Unit = {
            returnDependees = returnDependees.updated(
                method,
                returnDependees(method).updated(
                    returnDependees(method).indexWhere(_.e == newDependee.e),
                    newDependee
                )
            )
        }

        def hasDependees: Boolean = {
            tacDependees.values.exists(_.isRefinable) ||
            returnDependees.values.flatten.exists(_.isRefinable) ||
            paramDependees.flatten.exists(_.isRefinable)
        }

        def dependees: Iterable[SomeEOptionP] = {
            tacDependees.values.filter(_.isRefinable) ++
                returnDependees.values.flatten.filter(_.isRefinable) ++
                paramDependees.flatten.filter(_.isRefinable)
        }
    }

    protected def interpretArbitraryCallToMethods(implicit
        state:     State,
        callState: FunctionCallState
    ): ProperPropertyComputationResult = {
        callState.calleeMethods.foreach { m =>
            val tacEOptP = callState.tacDependees(m)
            if (tacEOptP.isFinal) {
                val calleeTac = tacEOptP.asFinal.p.tac
                if (calleeTac.isEmpty) {
                    // When the tac ep is final but we still do not have a callee tac, we cannot infer arbitrary call values at all
                    callState.hasUnresolvableReturnValue += m -> true
                } else {
                    val returns = calleeTac.get.stmts.toIndexedSeq.filter(stmt => stmt.isInstanceOf[ReturnValue[V]])
                    if (returns.isEmpty) {
                        // A function without returns, e.g., because it is guaranteed to throw an exception, is approximated
                        // with the lower bound
                        callState.hasUnresolvableReturnValue += m -> true
                    } else {
                        callState.returnDependees += m -> returns.map { ret =>
                            val entity: SContext = (
                                ret.asInstanceOf[ReturnValue[V]].expr.asVar.toPersistentForm(calleeTac.get.stmts),
                                m
                            )
                            ps(entity, StringConstancyProperty.key)
                        }
                    }
                }
            }
        }

        tryComputeFinalResult
    }

    private def tryComputeFinalResult(implicit
        state:     State,
        callState: FunctionCallState
    ): ProperPropertyComputationResult = {
        if (callState.hasDependees) {
            InterimResult.forLB(
                InterpretationHandler.getEntityFromDefSitePC(callState.defSitePC),
                StringConstancyProperty.lb,
                callState.dependees.toSet,
                continuation(state, callState)
            )
        } else {
            val parameterScis = callState.paramDependees.map { param =>
                StringConstancyInformation.reduceMultiple(param.map {
                    _.asFinal.p.sci
                })
            }
            val methodScis = callState.calleeMethods.map { m =>
                if (callState.hasUnresolvableReturnValue(m)) {
                    StringConstancyInformation.lb
                } else {
                    StringConstancyInformation.reduceMultiple(callState.returnDependees(m).map {
                        _.asFinal.p.sci.fillInParameters(parameterScis)
                    })
                }
            }

            computeFinalResult(callState.defSitePC, StringConstancyInformation.reduceMultiple(methodScis))
        }
    }

    private def continuation(
        state:     State,
        callState: FunctionCallState
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case EUBP(m: Method, _: TACAI) =>
                callState.tacDependees += m -> eps.asInstanceOf[EOptionP[Method, TACAI]]
                interpretArbitraryCallToMethods(state, callState)

            case EUBP(_: (_, _), _: StringConstancyProperty) =>
                val contextEPS = eps.asInstanceOf[EOptionP[SContext, StringConstancyProperty]]
                callState.updateReturnDependee(contextEPS.e._2, contextEPS)
                tryComputeFinalResult(state, callState)

            case EUBP(_: DefSiteEntity, _: StringConstancyProperty) =>
                callState.updateParamDependee(eps.asInstanceOf[EOptionP[DefSiteEntity, StringConstancyProperty]])
                tryComputeFinalResult(state, callState)

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }
}
