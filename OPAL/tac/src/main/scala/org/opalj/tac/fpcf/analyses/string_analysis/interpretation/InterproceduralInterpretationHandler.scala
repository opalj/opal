/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import org.opalj.fpcf.ProperOnUpdateContinuation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Assignment
import org.opalj.tac.BinaryExpr
import org.opalj.tac.ExprStmt
import org.opalj.tac.GetField
import org.opalj.tac.IntConst
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StringConst
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.analyses.string_analysis.ComputationState
import org.opalj.tac.DoubleConst
import org.opalj.tac.FloatConst

/**
 * `InterproceduralInterpretationHandler` is responsible for processing expressions that are
 * relevant in order to determine which value(s) a string read operation might have. These
 * expressions usually come from the definitions sites of the variable of interest.
 * <p>
 * For this interpretation handler used interpreters (concrete instances of
 * [[AbstractStringInterpreter]]) can either return a final or intermediate result.
 *
 * @param cfg The control flow graph that underlies the program / method in which the expressions of
 *            interest reside.
 *
 * @author Patrick Mell
 */
class InterproceduralInterpretationHandler(
        cfg:             CFG[Stmt[V], TACStmts[V]],
        ps:              PropertyStore,
        declaredMethods: DeclaredMethods,
        state:           ComputationState,
        c:               ProperOnUpdateContinuation
) extends InterpretationHandler(cfg) {

    /**
     * Processed the given definition site in an interprocedural fashion.
     * <p>
     *
     * @inheritdoc
     */
    override def processDefSite(
        defSite: Int, params: List[StringConstancyInformation] = List()
    ): ProperPropertyComputationResult = {
        // Without doing the following conversion, the following compile error will occur: "the
        // result type of an implicit conversion must be more specific than org.opalj.fpcf.Entity"
        val e: Integer = defSite.toInt
        // Function parameters are not evaluated when none are present
        if (defSite < 0 && params.isEmpty) {
            return Result(e, StringConstancyProperty.lb)
        } else if (defSite < 0) {
            val paramPos = Math.abs(defSite + 2)
            return Result(e, StringConstancyProperty(params(paramPos)))
        } else if (processedDefSites.contains(defSite)) {
            return Result(e, StringConstancyProperty.getNeutralElement)
        }
        processedDefSites.append(defSite)

        val callees = state.callees.get
        stmts(defSite) match {
            case Assignment(_, _, expr: StringConst) ⇒
                new StringConstInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: IntConst) ⇒
                new IntegerValueInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: FloatConst) ⇒
                new FloatValueInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: DoubleConst) ⇒
                new DoubleValueInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: ArrayLoad[V]) ⇒
                new InterproceduralArrayInterpreter(cfg, this, callees).interpret(expr, defSite)
            case Assignment(_, _, expr: New) ⇒
                new NewInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: VirtualFunctionCall[V]) ⇒
                new InterproceduralVirtualFunctionCallInterpreter(
                    cfg, this, callees, params
                ).interpret(expr, defSite)
            case Assignment(_, _, expr: StaticFunctionCall[V]) ⇒
                new InterproceduralStaticFunctionCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(expr, defSite)
            case Assignment(_, _, expr: BinaryExpr[V]) ⇒
                new BinaryExprInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) ⇒
                new InterproceduralNonVirtualFunctionCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(expr, defSite)
            case Assignment(_, _, expr: GetField[V]) ⇒
                new InterproceduralFieldInterpreter(cfg, this, callees).interpret(expr, defSite)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) ⇒
                new InterproceduralVirtualFunctionCallInterpreter(
                    cfg, this, callees, params
                ).interpret(expr, defSite)
            case ExprStmt(_, expr: StaticFunctionCall[V]) ⇒
                new InterproceduralStaticFunctionCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(expr, defSite)
            case vmc: VirtualMethodCall[V] ⇒
                new InterproceduralVirtualMethodCallInterpreter(
                    cfg, this, callees
                ).interpret(vmc, defSite)
            case nvmc: NonVirtualMethodCall[V] ⇒
                new InterproceduralNonVirtualMethodCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(nvmc, defSite)
            case _ ⇒ Result(e, StringConstancyProperty.getNeutralElement)
        }
    }

}

object InterproceduralInterpretationHandler {

    /**
     * @see [[IntraproceduralInterpretationHandler]]
     */
    def apply(
        cfg:             CFG[Stmt[V], TACStmts[V]],
        ps:              PropertyStore,
        declaredMethods: DeclaredMethods,
        state:           ComputationState,
        c:               ProperOnUpdateContinuation
    ): InterproceduralInterpretationHandler = new InterproceduralInterpretationHandler(
        cfg, ps, declaredMethods, state, c
    )

}