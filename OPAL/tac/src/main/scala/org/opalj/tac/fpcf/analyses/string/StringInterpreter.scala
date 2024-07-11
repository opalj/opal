/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.interpretation.SoundnessMode
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @author Maximilian Rüsch
 */
trait StringInterpreter {

    type T <: Stmt[V]

    /**
     * @param instr   The instruction that is to be interpreted.
     * @return A [[ProperPropertyComputationResult]] for the given pc containing the interpretation of the given
     *         instruction.
     */
    def interpret(instr: T)(implicit state: InterpretationState): ProperPropertyComputationResult

    def failure(v: PV)(implicit state: InterpretationState, soundnessMode: SoundnessMode): Result =
        StringInterpreter.failure(v)

    def computeFinalResult(web: PDUWeb, sff: StringFlowFunction)(implicit state: InterpretationState): Result =
        StringInterpreter.computeFinalResult(web, sff)

    def computeFinalResult(webs: Set[PDUWeb], sff: StringFlowFunction)(implicit state: InterpretationState): Result =
        StringInterpreter.computeFinalResult(webs, sff)

    def computeFinalResult(p: StringFlowFunctionProperty)(implicit state: InterpretationState): Result =
        StringInterpreter.computeFinalResult(p)
}

object StringInterpreter {

    def failure(v: V)(implicit state: InterpretationState, soundnessMode: SoundnessMode): Result =
        failure(v.toPersistentForm(state.tac.stmts))

    def failure(pv: PV)(implicit state: InterpretationState, soundnessMode: SoundnessMode): Result = {
        if (soundnessMode.isHigh) {
            computeFinalResult(StringFlowFunctionProperty.lb(state.pc, pv))
        } else {
            computeFinalResult(StringFlowFunctionProperty.noFlow(state.pc, pv))
        }
    }

    def computeFinalResult(web: PDUWeb, sff: StringFlowFunction)(implicit state: InterpretationState): Result =
        Result(FinalEP(InterpretationHandler.getEntity(state), StringFlowFunctionProperty(web, sff)))

    def computeFinalResult(webs: Set[PDUWeb], sff: StringFlowFunction)(implicit state: InterpretationState): Result =
        Result(FinalEP(InterpretationHandler.getEntity(state), StringFlowFunctionProperty(webs, sff)))

    def computeFinalResult(p: StringFlowFunctionProperty)(implicit state: InterpretationState): Result =
        Result(FinalEP(InterpretationHandler.getEntity(state), p))
}

trait ParameterEvaluatingStringInterpreter extends StringInterpreter {

    protected def getParametersForPC(pc: Int)(implicit state: InterpretationState): Seq[Expr[V]] = {
        state.tac.stmts(state.tac.pcToIndex(pc)) match {
            case ExprStmt(_, vfc: FunctionCall[V])     => vfc.params
            case Assignment(_, _, fc: FunctionCall[V]) => fc.params
            case _                                     => Seq.empty
        }
    }
}

trait AssignmentLikeBasedStringInterpreter extends StringInterpreter {

    type E <: Expr[V]

    override type T <: AssignmentLikeStmt[V]

    override final def interpret(instr: T)(implicit state: InterpretationState): ProperPropertyComputationResult =
        interpretExpr(instr, instr.expr.asInstanceOf[E])

    def interpretExpr(instr: T, expr: E)(implicit state: InterpretationState): ProperPropertyComputationResult
}

trait AssignmentBasedStringInterpreter extends AssignmentLikeBasedStringInterpreter {

    override type T = Assignment[V]

    override final def interpretExpr(instr: T, expr: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        interpretExpr(instr.targetVar.toPersistentForm(state.tac.stmts), expr)
    }

    def interpretExpr(target: PV, expr: E)(implicit state: InterpretationState): ProperPropertyComputationResult
}
