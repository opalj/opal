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
import org.opalj.tac.fpcf.properties.string.StringFlowFunction

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

    def computeFinalLBFor(v: V)(implicit state: InterpretationState): Result =
        StringInterpreter.computeFinalLBFor(v)

    def computeFinalLBFor(v: PV)(implicit state: InterpretationState): Result =
        StringInterpreter.computeFinalLBFor(v)

    def computeFinalResult(sff: StringFlowFunction)(implicit state: InterpretationState): Result =
        StringInterpreter.computeFinalResult(sff)
}

object StringInterpreter {

    def computeFinalLBFor(v: V)(implicit state: InterpretationState): Result =
        computeFinalLBFor(v.toPersistentForm(state.tac.stmts))

    def computeFinalLBFor(v: PV)(implicit state: InterpretationState): Result =
        computeFinalResult(StringFlowFunction.lb(v))

    def computeFinalResult(sff: StringFlowFunction)(implicit state: InterpretationState): Result =
        Result(FinalEP(InterpretationHandler.getEntity(state), sff))

    def findUVarForDVar(dVar: V)(implicit state: InterpretationState): V = {
        state.tac.stmts(dVar.usedBy.head) match {
            case Assignment(_, _, expr: Var[V]) => expr.asVar
            case ExprStmt(_, expr: Var[V])      => expr.asVar

            case Assignment(_, _, call: InstanceFunctionCall[V]) => call.receiver.asVar
            case ExprStmt(_, call: InstanceFunctionCall[V])      => call.receiver.asVar

            case ExprStmt(_, call: InstanceMethodCall[V]) => call.receiver.asVar

            case _ =>
                throw new IllegalArgumentException(s"Cannot determine uVar from $dVar")
        }
    }
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
        interpretExpr(StringInterpreter.findUVarForDVar(instr.targetVar).toPersistentForm(state.tac.stmts), expr)
    }

    def interpretExpr(target: PV, expr: E)(implicit state: InterpretationState): ProperPropertyComputationResult
}
