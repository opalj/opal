/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.br.ClassType
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * The base trait for all string interpreters, producing a FPCF [[StringFlowFunctionProperty]] for a given statement
 * in the context of its method.
 *
 * @author Maximilian Rüsch
 */
trait StringInterpreter {

    type T <: Stmt[V]

    /**
     * @param instr   The instruction that is to be interpreted.
     * @return A [[ProperPropertyComputationResult]] for the given pc containing the interpretation of the given
     *         instruction in the form of a [[StringFlowFunctionProperty]].
     */
    def interpret(instr: T)(implicit state: InterpretationState): ProperPropertyComputationResult

    protected def failureTree(implicit highSoundness: Boolean): StringTreeNode =
        StringInterpreter.failureTree

    protected def failure(v: PV)(implicit state: InterpretationState, highSoundness: Boolean): Result =
        StringInterpreter.failure(v)

    protected def computeFinalResult(web: PDUWeb, sff: StringFlowFunction)(implicit
        state: InterpretationState
    ): Result =
        StringInterpreter.computeFinalResult(web, sff)

    protected def computeFinalResult(webs: Set[PDUWeb], sff: StringFlowFunction)(implicit
        state: InterpretationState
    ): Result =
        StringInterpreter.computeFinalResult(webs, sff)

    protected def computeFinalResult(p: StringFlowFunctionProperty)(implicit state: InterpretationState): Result =
        StringInterpreter.computeFinalResult(p)
}

object StringInterpreter {

    def failureTree(implicit highSoundness: Boolean): StringTreeNode = {
        if (highSoundness) StringTreeNode.lb
        else StringTreeNode.ub
    }

    def failure(v: V)(implicit state: InterpretationState, highSoundness: Boolean): Result =
        failure(v.toPersistentForm(using state.tac.stmts))

    def failure(pv: PV)(implicit state: InterpretationState, highSoundness: Boolean): Result =
        computeFinalResult(StringFlowFunctionProperty.constForVariableAt(state.pc, pv, failureTree))

    def computeFinalResult(web: PDUWeb, sff: StringFlowFunction)(implicit state: InterpretationState): Result =
        computeFinalResult(StringFlowFunctionProperty(web, sff))

    def computeFinalResult(webs: Set[PDUWeb], sff: StringFlowFunction)(implicit state: InterpretationState): Result =
        computeFinalResult(StringFlowFunctionProperty(webs, sff))

    def computeFinalResult(p: StringFlowFunctionProperty)(implicit state: InterpretationState): Result =
        Result(FinalEP(InterpretationHandler.getEntity, p))

    def invalidEntitiesForUnknownCall(call: Call[V], target: Option[PV] = None)(implicit
        state: InterpretationState
    ): Set[PV] = {
        val relevantParameters = call.descriptor.parameterTypes.iterator.zipWithIndex.collect {
            case (p, index)
                if p.isClassType && ((p.asClassType eq ClassType.StringBuilder) || (p.asClassType eq ClassType.StringBuffer)) =>
                call.params(index)
        }
        val relevantReceiver = call.receiverOption.filter { _ => isStringBuilderBufferCall(call) }
        (relevantParameters ++ relevantReceiver).map(_.asVar.toPersistentForm(using state.tac.stmts)).toSet ++ target
    }

    def uninterpretedCall(call: Call[V], target: Option[PV] = None)(implicit
        state:         InterpretationState,
        highSoundness: Boolean
    ): Result = {
        val relevantEntities = invalidEntitiesForUnknownCall(call, target)
        val webs = relevantEntities.map(PDUWeb(state.pc, _))
        val flow = StringFlowFunctionProperty.constForEntities(state.pc, relevantEntities, failureTree)
        val p = StringFlowFunctionProperty(webs, flow)
        Result(FinalEP(InterpretationHandler.getEntity, p))
    }

    def isStringBuilderBufferCall(call: Call[V]): Boolean =
        (call.declaringClass eq ClassType.StringBuilder) || (call.declaringClass eq ClassType.StringBuffer)
}

/**
 * Base trait for all [[StringInterpreter]]s that have to evaluate parameters at a given call site, thus providing
 * appropriate utility.
 *
 * @author Maximilian Rüsch
 */
trait ParameterEvaluatingStringInterpreter extends StringInterpreter {

    protected def getParametersForPC(pc: Int)(implicit state: InterpretationState): Seq[Expr[V]] = {
        state.tac.stmts(state.tac.pcToIndex(pc)) match {
            case AssignmentLikeStmt(_, fc: FunctionCall[V]) => fc.params
            case _                                          => Seq.empty
        }
    }
}

/**
 * Base trait for all string interpreters that only process [[AssignmentLikeStmt]]s, allowing the trait to pre-unpack
 * the expression of the [[AssignmentLikeStmt]].
 *
 * @author Maximilian Rüsch
 */
trait AssignmentLikeBasedStringInterpreter extends StringInterpreter {

    type E <: Expr[V]

    override type T <: AssignmentLikeStmt[V]

    override final def interpret(instr: T)(implicit state: InterpretationState): ProperPropertyComputationResult =
        interpretExpr(instr, instr.expr.asInstanceOf[E])

    def interpretExpr(instr: T, expr: E)(implicit state: InterpretationState): ProperPropertyComputationResult
}

/**
 * Base trait for all string interpreters that only process [[Assignment]]s, allowing the trait to pre-unpack the
 * assignment target variable as well as the operation performed by [[AssignmentLikeBasedStringInterpreter]].
 *
 * @author Maximilian Rüsch
 */
trait AssignmentBasedStringInterpreter extends AssignmentLikeBasedStringInterpreter {

    override type T = Assignment[V]

    override final def interpretExpr(instr: T, expr: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        interpretExpr(instr.targetVar.toPersistentForm(using state.tac.stmts), expr)
    }

    def interpretExpr(target: PV, expr: E)(implicit state: InterpretationState): ProperPropertyComputationResult
}
