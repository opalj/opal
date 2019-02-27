/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

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
import org.opalj.tac.DoubleConst
import org.opalj.tac.ExprStmt
import org.opalj.tac.FloatConst
import org.opalj.tac.GetField
import org.opalj.tac.IntConst
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StringConst
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.DoubleValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.FloatValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.IntegerValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer.ArrayFinalizer
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.NewInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.StringConstInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer.NonVirtualMethodCallFinalizer
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer.VirtualFunctionCallFinalizer
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState
import org.opalj.tac.GetStatic

/**
 * `InterproceduralInterpretationHandler` is responsible for processing expressions that are
 * relevant in order to determine which value(s) a string read operation might have. These
 * expressions usually come from the definitions sites of the variable of interest.
 * <p>
 * For this interpretation handler used interpreters (concrete instances of
 * [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter]]) can
 * either return a final or intermediate result.
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
        state:           InterproceduralComputationState,
        c:               ProperOnUpdateContinuation
) extends InterpretationHandler(cfg) {

    /**
     * Processed the given definition site in an interprocedural fashion.
     * <p>
     *
     * @inheritdoc
     */
    override def processDefSite(
        defSite: Int, params: List[Seq[StringConstancyInformation]] = List()
    ): ProperPropertyComputationResult = {
        // Without doing the following conversion, the following compile error will occur: "the
        // result type of an implicit conversion must be more specific than org.opalj.fpcf.Entity"
        val e: Integer = defSite.toInt
        // Function parameters are not evaluated when none are present (this always includes the
        // implicit parameter for "this")
        if (defSite < 0 && (params.isEmpty || defSite == -1)) {
            return Result(e, StringConstancyProperty.lb)
        } else if (defSite < 0) {
            val paramPos = Math.abs(defSite + 2)
            val paramScis = params.map(_(paramPos)).distinct
            val finalParamSci = StringConstancyInformation.reduceMultiple(paramScis)
            return Result(e, StringConstancyProperty(finalParamSci))
        } else if (processedDefSites.contains(defSite)) {
            return Result(e, StringConstancyProperty.getNeutralElement)
        }
        // Note that def sites referring to constant expressions will be deleted further down
        processedDefSites.append(defSite)

        val callees = state.callees
        stmts(defSite) match {
            case Assignment(_, _, expr: StringConst) ⇒
                val result = new StringConstInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                processedDefSites.remove(processedDefSites.length - 1)
                result
            case Assignment(_, _, expr: IntConst) ⇒
                val result = new IntegerValueInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                processedDefSites.remove(processedDefSites.length - 1)
                result
            case Assignment(_, _, expr: FloatConst) ⇒
                val result = new FloatValueInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                processedDefSites.remove(processedDefSites.length - 1)
                result
            case Assignment(_, _, expr: DoubleConst) ⇒
                val result = new DoubleValueInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                processedDefSites.remove(processedDefSites.length - 1)
                result
            case Assignment(_, _, expr: ArrayLoad[V]) ⇒
                new ArrayPreparationInterpreter(cfg, this, state, params).interpret(expr, defSite)
            case Assignment(_, _, expr: New) ⇒
                val result = new NewInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                result
            case Assignment(_, _, expr: GetStatic) ⇒
                new InterproceduralGetStaticInterpreter(cfg, this).interpret(expr, defSite)
            case ExprStmt(_, expr: GetStatic) ⇒
                new InterproceduralGetStaticInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: VirtualFunctionCall[V]) ⇒
                val r = new VirtualFunctionCallPreparationInterpreter(
                    cfg, this, ps, state, declaredMethods, params, c
                ).interpret(expr, defSite)
                // In case no final result could be computed, remove this def site from the list of
                // processed def sites to make sure that is can be compute again (when all final
                // results are available)
                if (state.nonFinalFunctionArgs.contains(expr)) {
                    processedDefSites.remove(processedDefSites.indexOf(defSite))
                }
                r
            case Assignment(_, _, expr: StaticFunctionCall[V]) ⇒
                val r = new InterproceduralStaticFunctionCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(expr, defSite)
                // In case no final result could be computed, remove this def site from the list of
                // processed def sites to make sure that is can be compute again (when all final
                // results are available)
                if (state.nonFinalFunctionArgs.contains(expr)) {
                    processedDefSites.remove(processedDefSites.indexOf(defSite))
                }
                r
            case Assignment(_, _, expr: BinaryExpr[V]) ⇒
                val result = new BinaryExprInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                result
            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) ⇒
                new InterproceduralNonVirtualFunctionCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(expr, defSite)
            case Assignment(_, _, expr: GetField[V]) ⇒
                new InterproceduralFieldInterpreter(cfg, this, callees).interpret(expr, defSite)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) ⇒
                val r = new VirtualFunctionCallPreparationInterpreter(
                    cfg, this, ps, state, declaredMethods, params, c
                ).interpret(expr, defSite)
                // In case no final result could be computed, remove this def site from the list of
                // processed def sites to make sure that is can be compute again (when all final
                // results are available)
                if (state.nonFinalFunctionArgs.contains(expr)) {
                    processedDefSites.remove(processedDefSites.indexOf(defSite))
                }
                r
            case ExprStmt(_, expr: StaticFunctionCall[V]) ⇒
                val r = new InterproceduralStaticFunctionCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(expr, defSite)
                // In case no final result could be computed, remove this def site from the list of
                // processed def sites to make sure that is can be compute again (when all final
                // results are available)
                if (!r.isInstanceOf[Result]) {
                    processedDefSites.remove(processedDefSites.length - 1)
                }
                r
            case vmc: VirtualMethodCall[V] ⇒
                new InterproceduralVirtualMethodCallInterpreter(
                    cfg, this, callees
                ).interpret(vmc, defSite)
            case nvmc: NonVirtualMethodCall[V] ⇒
                val result = new InterproceduralNonVirtualMethodCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(nvmc, defSite)
                result match {
                    case r: Result ⇒ state.appendResultToFpe2Sci(defSite, r)
                    case _         ⇒
                }
                result
            case _ ⇒ Result(e, StringConstancyProperty.getNeutralElement)
        }
    }

    def finalizeDefSite(
        defSite: Int, state: InterproceduralComputationState
    ): Unit = {
        stmts(defSite) match {
            case nvmc: NonVirtualMethodCall[V] ⇒
                new NonVirtualMethodCallFinalizer(state).finalizeInterpretation(nvmc, defSite)
            case Assignment(_, _, al: ArrayLoad[V]) ⇒
                new ArrayFinalizer(state, cfg).finalizeInterpretation(al, defSite)
            case Assignment(_, _, vfc: VirtualFunctionCall[V]) ⇒
                new VirtualFunctionCallFinalizer(state, cfg).finalizeInterpretation(vfc, defSite)
            case ExprStmt(_, vfc: VirtualFunctionCall[V]) ⇒
                new VirtualFunctionCallFinalizer(state, cfg).finalizeInterpretation(vfc, defSite)
            case _ ⇒
        }
    }

}

object InterproceduralInterpretationHandler {

    /**
     * @see [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.intraprocedural.IntraproceduralInterpretationHandler]]
     */
    def apply(
        cfg:             CFG[Stmt[V], TACStmts[V]],
        ps:              PropertyStore,
        declaredMethods: DeclaredMethods,
        state:           InterproceduralComputationState,
        c:               ProperOnUpdateContinuation
    ): InterproceduralInterpretationHandler = new InterproceduralInterpretationHandler(
        cfg, ps, declaredMethods, state, c
    )

}