/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

import scala.util.Try

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralStringAnalysis
import org.opalj.tac.ReturnValue

/**
 * The `InterproceduralStaticFunctionCallInterpreter` is responsible for processing
 * [[StaticFunctionCall]]s in an interprocedural fashion.
 * <p>
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class InterproceduralStaticFunctionCallInterpreter(
        cfg:             CFG[Stmt[V], TACStmts[V]],
        exprHandler:     InterproceduralInterpretationHandler,
        ps:              PropertyStore,
        state:           InterproceduralComputationState,
        params:          List[Seq[StringConstancyInformation]],
        declaredMethods: DeclaredMethods
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = StaticFunctionCall[V]

    /**
     * This function always returns a list with a single element consisting of
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel.DYNAMIC]],
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyType.APPEND]], and
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation.UnknownWordSymbol]].
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        if (instr.declaringClass.fqn == "java/lang/String" && instr.name == "valueOf") {
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
    private def processStringValueOf(
        call: StaticFunctionCall[V]
    ): EOptionP[Entity, StringConstancyProperty] = {
        val results = call.params.head.asVar.definedBy.toArray.sorted.map { ds ⇒
            exprHandler.processDefSite(ds, params)
        }
        val interim = results.find(_.isRefinable)
        if (interim.isDefined) {
            interim.get
        } else {
            // For char values, we need to do a conversion (as the returned results are integers)
            val scis = results.map { r ⇒
                r.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
            }
            val finalScis = if (call.descriptor.parameterType(0).toJava == "char") {
                scis.map { sci ⇒
                    if (Try(sci.possibleStrings.toInt).isSuccess) {
                        sci.copy(possibleStrings = sci.possibleStrings.toInt.toChar.toString)
                    } else {
                        sci
                    }
                }
            } else {
                scis
            }
            val finalSci = StringConstancyInformation.reduceMultiple(finalScis)
            FinalEP(call, StringConstancyProperty(finalSci))
        }
    }

    /**
     * This function interprets an arbitrary static function call.
     */
    private def processArbitraryCall(
        instr: StaticFunctionCall[V], defSite: Int
    ): EOptionP[Entity, StringConstancyProperty] = {
        val methods, _ = getMethodsForPC(
            instr.pc, ps, state.callees, declaredMethods
        )

        // Static methods cannot be overwritten, thus 1) we do not need the second return value of
        // getMethodsForPC and 2) interpreting the head is enough
        if (methods._1.isEmpty) {
            state.appendToFpe2Sci(defSite, StringConstancyProperty.lb.stringConstancyInformation)
            return FinalEP(instr, StringConstancyProperty.lb)
        }

        val m = methods._1.head
        val (_, tac) = getTACAI(ps, m, state)

        val directCallSites = state.callees.directCallSites()(ps, declaredMethods)
        val relevantPCs = directCallSites.filter {
            case (_, calledMethods) ⇒
                calledMethods.exists(m ⇒
                    m.name == instr.name && m.declaringClassType == instr.declaringClass)
        }.keys

        // Collect all parameters; either from the state if the interpretation of instr was started
        // before (in this case, the assumption is that all parameters are fully interpreted) or
        // start a new interpretation
        val params = if (state.nonFinalFunctionArgs.contains(instr)) {
            state.nonFinalFunctionArgs(instr)
        } else {
            evaluateParameters(
                getParametersForPCs(relevantPCs, state.tac),
                exprHandler,
                instr,
                state.nonFinalFunctionArgsPos,
                state.entity2Function
            )
        }
        // Continue only when all parameter information are available
        val nonFinalResults = getNonFinalParameters(params)
        if (nonFinalResults.nonEmpty) {
            if (tac.isDefined) {
                val returns = tac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
                returns.foreach { ret ⇒
                    val entity = (ret.asInstanceOf[ReturnValue[V]].expr.asVar, m)
                    val eps = ps(entity, StringConstancyProperty.key)
                    state.dependees = eps :: state.dependees
                    state.appendToVar2IndexMapping(entity._1, defSite)
                }
            }
            state.nonFinalFunctionArgs(instr) = params
            state.appendToMethodPrep2defSite(m, defSite)
            return nonFinalResults.head
        }

        state.nonFinalFunctionArgs.remove(instr)
        state.nonFinalFunctionArgsPos.remove(instr)
        val evaluatedParams = convertEvaluatedParameters(params)
        if (tac.isDefined) {
            state.removeFromMethodPrep2defSite(m, defSite)
            // TAC available => Get return UVar and start the string analysis
            val returns = tac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
            if (returns.isEmpty) {
                // A function without returns, e.g., because it is guaranteed to throw an exception,
                // is approximated with the lower bound
                FinalEP(instr, StringConstancyProperty.lb)
            } else {
                val results = returns.map { ret ⇒
                    val entity = (ret.asInstanceOf[ReturnValue[V]].expr.asVar, m)
                    InterproceduralStringAnalysis.registerParams(entity, evaluatedParams)

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
