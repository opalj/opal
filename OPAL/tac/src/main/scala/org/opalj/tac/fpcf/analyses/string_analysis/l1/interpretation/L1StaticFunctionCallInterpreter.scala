/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import scala.util.Try

import org.opalj.br.ObjectType
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for processing [[StaticFunctionCall]]s with a call graph.
 *
 * @author Maximilian Rüsch
 */
class L1StaticFunctionCallInterpreter(
        exprHandler:     InterpretationHandler[L1ComputationState],
        ps:              PropertyStore,
        contextProvider: ContextProvider
) extends L1StringInterpreter[L1ComputationState] {

    override type T = StaticFunctionCall[V]

    override def interpret(instr: T, defSite: Int)(
        implicit state: L1ComputationState
    ): EOptionP[Entity, StringConstancyProperty] = {
        if (instr.declaringClass == ObjectType.String && instr.name == "valueOf") {
            processStringValueOf(instr)
        } else {
            processArbitraryCall(instr, defSite)
        }
    }

    /**
     * A function for processing calls to [[String#valueOf]]. This function assumes that the passed
     * `call` element is actually such a call.
     * This function returns an intermediate results if one or more interpretations could not be
     * finished. Otherwise, if all definition sites could be fully processed, this function
     * returns an instance of Result which corresponds to the result of the interpretation of
     * the parameter passed to the call.
     */
    private def processStringValueOf(call: StaticFunctionCall[V])(
        implicit state: L1ComputationState
    ): EOptionP[Entity, StringConstancyProperty] = {
        val results = call.params.head.asVar.definedBy.toArray.sorted.map { exprHandler.processDefSite(_) }
        val interim = results.find(_.isRefinable)
        if (interim.isDefined) {
            interim.get
        } else {
            // For char values, we need to do a conversion (as the returned results are integers)
            val scis = results.map { r => r.asFinal.p.stringConstancyInformation }
            val finalScis = if (call.descriptor.parameterType(0).toJava == "char") {
                scis.map { sci =>
                    if (Try(sci.possibleStrings.toInt).isSuccess) {
                        sci.copy(possibleStrings = sci.possibleStrings.toInt.toChar.toString)
                    } else {
                        sci
                    }
                }
            } else {
                scis
            }
            FinalEP(call, StringConstancyProperty(StringConstancyInformation.reduceMultiple(finalScis)))
        }
    }

    private def processArbitraryCall(
        instr:   StaticFunctionCall[V],
        defSite: Int
    )(implicit state: L1ComputationState): EOptionP[Entity, StringConstancyProperty] = {
        val methods, _ = getMethodsForPC(instr.pc)(ps, state.callees, contextProvider)

        // Static methods cannot be overwritten, thus
        // 1) we do not need the second return value of getMethodsForPC and
        // 2) interpreting the head is enough
        if (methods._1.isEmpty) {
            state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), StringConstancyInformation.lb)
            return FinalEP(instr, StringConstancyProperty.lb)
        }

        val m = methods._1.head
        val (_, tac) = getTACAI(ps, m, state)

        val directCallSites = state.callees.directCallSites(NoContext)(ps, contextProvider)
        val relevantPCs = directCallSites.filter {
            case (_, calledMethods) =>
                calledMethods.exists(m =>
                    m.method.name == instr.name && m.method.declaringClassType == instr.declaringClass
                )
        }.keys

        // Collect all parameters; either from the state if the interpretation of instr was started
        // before (in this case, the assumption is that all parameters are fully interpreted) or
        // start a new interpretation
        val params = if (state.nonFinalFunctionArgs.contains(instr)) {
            state.nonFinalFunctionArgs(instr)
        } else {
            evaluateParameters(getParametersForPCs(relevantPCs), exprHandler, instr)
        }
        // Continue only when all parameter information are available
        val refinableResults = getRefinableParameterResults(params.toSeq.map(t => t.toSeq.map(_.toSeq)))
        if (refinableResults.nonEmpty) {
            if (tac.isDefined) {
                val returns = tac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
                returns.foreach { ret =>
                    val entity = (ret.asInstanceOf[ReturnValue[V]].expr.asVar.toPersistentForm(tac.get.stmts), m)
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
        if (tac.isDefined) {
            state.removeFromMethodPrep2defSite(m, defSite)
            // TAC available => Get return UVar and start the string analysis
            val returns = tac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
            if (returns.isEmpty) {
                // A function without returns, e.g., because it is guaranteed to throw an exception, is approximated
                // with the lower bound
                FinalEP(instr, StringConstancyProperty.lb)
            } else {
                val results = returns.map { ret =>
                    val entity = (ret.asInstanceOf[ReturnValue[V]].expr.asVar.toPersistentForm(tac.get.stmts), m)
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
