/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.SimpleValueConstExprInterpreter

/**
 * @inheritdoc
 *
 * @author Maximilian Rüsch
 */
class L0InterpretationHandler()(
    implicit
    p:  SomeProject,
    ps: PropertyStore
) extends InterpretationHandler {

    override protected def processNewPC(pc: Int)(implicit
        state: DUSiteState
    ): ProperPropertyComputationResult = {
        val duSiteOpt = valueOriginOfPC(pc, state.tac.pcToIndex);
        if (duSiteOpt.isEmpty) {
            throw new IllegalArgumentException(s"Obtained a pc that does not represent a definition site: $pc")
        }

        state.tac.stmts(duSiteOpt.get) match {
            case Assignment(_, _, expr: SimpleValueConst) => SimpleValueConstExprInterpreter.interpret(expr, pc)
            case Assignment(_, _, expr: BinaryExpr[V])    => BinaryExprInterpreter.interpret(expr, pc)

            case Assignment(_, _, expr: ArrayLoad[V]) => L0ArrayAccessInterpreter(ps).interpret(expr, pc)
            case Assignment(_, _, expr: NewArray[V])  => L0NewArrayInterpreter(ps).interpret(expr, pc)
            case Assignment(_, _, _: New) =>
                StringInterpreter.computeFinalResult(pc, StringConstancyInformation.neutralElement)

            // Currently unsupported
            case Assignment(_, _, _: GetField[V]) =>
                StringInterpreter.computeFinalResult(pc, StringConstancyInformation.lb)

            case Assignment(_, _, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter(ps).interpret(expr, pc)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter(ps).interpret(expr, pc)

            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) =>
                L0NonVirtualFunctionCallInterpreter().interpret(expr, pc)
            case ExprStmt(_, expr: NonVirtualFunctionCall[V]) =>
                L0NonVirtualFunctionCallInterpreter().interpret(expr, pc)

            case Assignment(_, _, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter().interpret(expr, pc)
            case ExprStmt(_, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter().interpret(expr, pc)

            case vmc: VirtualMethodCall[V]     => L0VirtualMethodCallInterpreter.interpret(vmc, pc)
            case nvmc: NonVirtualMethodCall[V] => L0NonVirtualMethodCallInterpreter(ps).interpret(nvmc, pc)

            case _ =>
                StringInterpreter.computeFinalResult(pc, StringConstancyInformation.neutralElement)
        }
    }
}

object L0InterpretationHandler {

    def apply()(
        implicit
        p:  SomeProject,
        ps: PropertyStore
    ): L0InterpretationHandler = new L0InterpretationHandler
}
