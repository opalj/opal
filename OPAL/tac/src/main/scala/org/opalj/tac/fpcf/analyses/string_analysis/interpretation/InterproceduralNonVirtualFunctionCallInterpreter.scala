/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperOnUpdateContinuation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.Method
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.ReturnValue
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.string_analysis.ComputationState

/**
 * The `InterproceduralNonVirtualFunctionCallInterpreter` is responsible for processing
 * [[NonVirtualFunctionCall]]s in an interprocedural fashion.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class InterproceduralNonVirtualFunctionCallInterpreter(
        cfg:             CFG[Stmt[V], TACStmts[V]],
        exprHandler:     InterproceduralInterpretationHandler,
        ps:              PropertyStore,
        state:           ComputationState,
        declaredMethods: DeclaredMethods,
        c:               ProperOnUpdateContinuation
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = NonVirtualFunctionCall[V]

    def getTACAI(
        m: Method, dependees: ListBuffer[SomeEOptionP]
    ): Option[TACode[TACMethodParameter, V]] = {
        val tacai = ps(m, TACAI.key)
        if (tacai.hasUBP) {
            tacai.ub.tac
        } else {
            if (!state.dependees.contains(m)) {
                state.dependees(m) = ListBuffer()
            }
            state.dependees(m).append(tacai)
            None
        }
    }

    /**
     * Currently, [[NonVirtualFunctionCall]]s are not supported. Thus, this function always returns
     * a list with a single element consisting of
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel.DYNAMIC]],
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyType.APPEND]] and
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation.UnknownWordSymbol]].
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): ProperPropertyComputationResult = {
        val dependees = ListBuffer[SomeEOptionP]()
        val m = declaredMethods.declaredMethods.find(_.name == instr.name).get.definedMethod
        val tac = getTACAI(m, dependees)
        if (tac.isDefined) {
            // TAC available => Get return UVar and start the string analysis
            val ret = tac.get.stmts.find(_.isInstanceOf[ReturnValue[V]]).get
            val uvar = ret.asInstanceOf[ReturnValue[V]].expr.asVar
            val entity = (uvar, m)

            val eps = ps(entity, StringConstancyProperty.key)
            eps match {
                case FinalEP(e, p) ⇒
                    Result(e, p)
                case _ ⇒
                    if (!state.dependees.contains(m)) {
                        state.dependees(m) = ListBuffer()
                    }
                    state.dependees(m).append(eps)
                    state.var2IndexMapping(uvar) = defSite
                    InterimResult(
                        entity,
                        StringConstancyProperty.lowerBound,
                        StringConstancyProperty.upperBound,
                        List(),
                        c
                    )
            }
        } else {
            // No TAC => Register dependee and continue
            InterimResult(
                m,
                StringConstancyProperty.lowerBound,
                StringConstancyProperty.upperBound,
                dependees,
                c
            )
        }
    }

}
