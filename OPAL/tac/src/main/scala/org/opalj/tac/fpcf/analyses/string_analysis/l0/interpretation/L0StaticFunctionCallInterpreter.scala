/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Processes [[StaticFunctionCall]]s in without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0StaticFunctionCallInterpreter[State <: L0ComputationState[State]](
    exprHandler: InterpretationHandler[State]
)(
    implicit
    p:  SomeProject,
    ps: PropertyStore
) extends L0StringInterpreter[State] {

    override type T = StaticFunctionCall[V]

    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] = {
        val calleeMethod = instr.resolveCallTarget(state.entity._2.classFile.thisType)
        if (calleeMethod.isEmpty) {
            state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), StringConstancyInformation.lb)
            return FinalEP(instr, StringConstancyProperty.lb)
        }

        // Collect all parameters; either from the state if the interpretation of instr was started before (in this case,
        // the assumption is that all parameters are fully interpreted) or start a new interpretation
        val params = if (state.nonFinalFunctionArgs.contains(instr)) {
            state.nonFinalFunctionArgs(instr)
        } else {
            evaluateParameters(getParametersForPCs(List(instr.pc)), exprHandler, instr)
        }

        val m = calleeMethod.value
        val (_, calleeTac) = getTACAI(ps, m, state)

        // Continue only when all parameter information are available
        val refinableResults = getRefinableParameterResults(params.toSeq.map(t => t.toSeq.map(_.toSeq)))
        if (refinableResults.nonEmpty) {
            // question why do we depend on the return value of the call
            if (calleeTac.isDefined) {
                val returns = calleeTac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
                returns.foreach { ret =>
                    val entity = (ret.asInstanceOf[ReturnValue[V]].expr.asVar.toPersistentForm(calleeTac.get.stmts), m)
                    val eps = ps(entity, StringConstancyProperty.key)
                    state.dependees = eps :: state.dependees
                    state.appendToVar2IndexMapping(entity._1, defSite)
                }
            }
            state.nonFinalFunctionArgs(instr) = params
            state.appendToMethodPrep2defSite(m, defSite)
            return refinableResults.head
        }

        state.nonFinalFunctionArgs.remove(instr)
        state.nonFinalFunctionArgsPos.remove(instr)
        val evaluatedParams = convertEvaluatedParameters(params.toSeq.map(t => t.toSeq.map(_.toSeq.map(_.asFinal))))
        if (calleeTac.isDefined) {
            state.removeFromMethodPrep2defSite(m, defSite)
            // TAC available => Get return UVar and start the string analysis
            val returns = calleeTac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
            if (returns.isEmpty) {
                // A function without returns, e.g., because it is guaranteed to throw an exception, is approximated
                // with the lower bound
                FinalEP(instr, StringConstancyProperty.lb)
            } else {
                val results = returns.map { ret =>
                    val entity = (ret.asInstanceOf[ReturnValue[V]].expr.asVar.toPersistentForm(calleeTac.get.stmts), m)
                    StringAnalysis.registerParams(entity, evaluatedParams)

                    val eps = ps(entity, StringConstancyProperty.key)
                    if (eps.isRefinable) {
                        state.dependees = eps :: state.dependees
                        state.appendToVar2IndexMapping(entity._1, defSite)
                    }
                    eps
                }
                results.find(_.isRefinable).getOrElse(results.head)
            }
        } else {
            // No TAC => Register dependee and continue
            state.appendToMethodPrep2defSite(m, defSite)
            EPK(state.entity, StringConstancyProperty.key)
        }
    }
}
