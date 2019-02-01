/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
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
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * `IntraproceduralInterpretationHandler` is responsible for processing expressions that are
 * relevant in order to determine which value(s) a string read operation might have. These
 * expressions usually come from the definitions sites of the variable of interest.
 * <p>
 * For this interpretation handler it is crucial that all used interpreters (concrete instances of
 * [[AbstractStringInterpreter]]) return a final computation result!
 *
 * @param cfg The control flow graph that underlies the program / method in which the expressions of
 *            interest reside.
 * @author Patrick Mell
 */
class IntraproceduralInterpretationHandler(
        cfg: CFG[Stmt[V], TACStmts[V]]
) extends InterpretationHandler(cfg) {

    /**
     * Processed the given definition site in an intraprocedural fashion.
     * <p>
     * @inheritdoc
     */
    override def processDefSite(
        defSite: Int, params: List[StringConstancyInformation] = List()
    ): ProperPropertyComputationResult = {
        // Without doing the following conversion, the following compile error will occur: "the
        // result type of an implicit conversion must be more specific than org.opalj.fpcf.Entity"
        val e: Integer = defSite.toInt
        // Function parameters are not evaluated but regarded as unknown
        if (defSite < 0) {
            return Result(e, StringConstancyProperty.lb)
        } else if (processedDefSites.contains(defSite)) {
            return Result(e, StringConstancyProperty.getNeutralElement)
        }
        processedDefSites.append(defSite)

        // TODO: Refactor by making the match return a concrete instance of
        //  AbstractStringInterpreter on which 'interpret' is the called only once
        val result: ProperPropertyComputationResult = stmts(defSite) match {
            case Assignment(_, _, expr: StringConst) ⇒
                new StringConstInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: IntConst) ⇒
                new IntegerValueInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: ArrayLoad[V]) ⇒
                new IntraproceduralArrayInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: New) ⇒
                new NewInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: VirtualFunctionCall[V]) ⇒
                new IntraproceduralVirtualFunctionCallInterpreter(
                    cfg, this
                ).interpret(expr, defSite)
            case Assignment(_, _, expr: StaticFunctionCall[V]) ⇒
                new IntraproceduralStaticFunctionCallInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: BinaryExpr[V]) ⇒
                new BinaryExprInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) ⇒
                new IntraproceduralNonVirtualFunctionCallInterpreter(
                    cfg, this
                ).interpret(expr, defSite)
            case Assignment(_, _, expr: GetField[V]) ⇒
                new IntraproceduralFieldInterpreter(cfg, this).interpret(expr, defSite)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) ⇒
                new IntraproceduralVirtualFunctionCallInterpreter(
                    cfg, this
                ).interpret(expr, defSite)
            case ExprStmt(_, expr: StaticFunctionCall[V]) ⇒
                new IntraproceduralStaticFunctionCallInterpreter(cfg, this).interpret(expr, defSite)
            case vmc: VirtualMethodCall[V] ⇒
                new IntraproceduralVirtualMethodCallInterpreter(cfg, this).interpret(vmc, defSite)
            case nvmc: NonVirtualMethodCall[V] ⇒
                new IntraproceduralNonVirtualMethodCallInterpreter(
                    cfg, this
                ).interpret(nvmc, defSite)
            case _ ⇒ Result(e, StringConstancyProperty.getNeutralElement)
        }
        // Replace the entity of the result
        Result(e, result.asInstanceOf[Result].finalEP.p)
    }

}

object IntraproceduralInterpretationHandler {

    /**
     * @see [[IntraproceduralInterpretationHandler]]
     */
    def apply(
        cfg: CFG[Stmt[V], TACStmts[V]]
    ): IntraproceduralInterpretationHandler = new IntraproceduralInterpretationHandler(cfg)

}
