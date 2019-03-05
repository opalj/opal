/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

import org.opalj.fpcf.ProperOnUpdateContinuation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.ai.ImmediateVMExceptionsOriginOffset
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
import org.opalj.tac.DUVar
import org.opalj.tac.GetStatic
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode

/**
 * `InterproceduralInterpretationHandler` is responsible for processing expressions that are
 * relevant in order to determine which value(s) a string read operation might have. These
 * expressions usually come from the definitions sites of the variable of interest.
 * <p>
 * For this interpretation handler used interpreters (concrete instances of
 * [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter]]) can
 * either return a final or intermediate result.
 *
 * @author Patrick Mell
 */
class InterproceduralInterpretationHandler(
        tac:             TACode[TACMethodParameter, DUVar[ValueInformation]],
        ps:              PropertyStore,
        declaredMethods: DeclaredMethods,
        state:           InterproceduralComputationState,
        c:               ProperOnUpdateContinuation
) extends InterpretationHandler(tac) {

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
        // implicit parameter for "this" and for exceptions thrown outside the current function)
        if (defSite < 0 &&
            (params.isEmpty || defSite == -1 || defSite <= ImmediateVMExceptionsOriginOffset)) {
            return Result(e, StringConstancyProperty.lb)
        } else if (defSite < 0) {
            return Result(e, StringConstancyProperty(getParam(params, defSite)))
        } else if (processedDefSites.contains(defSite)) {
            return Result(e, StringConstancyProperty.getNeutralElement)
        }
        // Note that def sites referring to constant expressions will be deleted further down
        processedDefSites(defSite) = Unit

        val callees = state.callees
        stmts(defSite) match {
            case Assignment(_, _, expr: StringConst) ⇒
                val result = new StringConstInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                processedDefSites.remove(defSite)
                result
            case Assignment(_, _, expr: IntConst) ⇒
                val result = new IntegerValueInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                processedDefSites.remove(defSite)
                result
            case Assignment(_, _, expr: FloatConst) ⇒
                val result = new FloatValueInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                processedDefSites.remove(defSite)
                result
            case Assignment(_, _, expr: DoubleConst) ⇒
                val result = new DoubleValueInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                processedDefSites.remove(defSite)
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
                // results are available); we use nonFinalFunctionArgs because if it does not
                // contain expr, it can be finalized later on without processing the function again
                if (state.nonFinalFunctionArgs.contains(expr)) {
                    processedDefSites.remove(defSite)
                }
                r
            case Assignment(_, _, expr: StaticFunctionCall[V]) ⇒
                val r = new InterproceduralStaticFunctionCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(expr, defSite)
                if (state.nonFinalFunctionArgs.contains(expr)) {
                    processedDefSites.remove(defSite)
                }
                r
            case Assignment(_, _, expr: BinaryExpr[V]) ⇒
                val result = new BinaryExprInterpreter(cfg, this).interpret(expr, defSite)
                state.appendResultToFpe2Sci(defSite, result.asInstanceOf[Result])
                result
            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) ⇒
                val r = new InterproceduralNonVirtualFunctionCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(expr, defSite)
                if (state.nonFinalFunctionArgs.contains(expr)) {
                    processedDefSites.remove(defSite)
                }
                r
            case Assignment(_, _, expr: GetField[V]) ⇒
                new InterproceduralFieldInterpreter(cfg, this, callees).interpret(expr, defSite)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) ⇒
                val r = new VirtualFunctionCallPreparationInterpreter(
                    cfg, this, ps, state, declaredMethods, params, c
                ).interpret(expr, defSite)
                if (state.nonFinalFunctionArgs.contains(expr)) {
                    processedDefSites.remove(defSite)
                }
                r
            case ExprStmt(_, expr: StaticFunctionCall[V]) ⇒
                val r = new InterproceduralStaticFunctionCallInterpreter(
                    cfg, this, ps, state, declaredMethods, c
                ).interpret(expr, defSite)
                if (state.nonFinalFunctionArgs.contains(expr)) {
                    processedDefSites.remove(defSite)
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

    /**
     * This function takes parameters and a definition site and extracts the desired parameter from
     * the given list of parameters. Note that `defSite` is required to be <= -2.
     */
    private def getParam(
        params: Seq[Seq[StringConstancyInformation]], defSite: Int
    ): StringConstancyInformation = {
        val paramPos = Math.abs(defSite + 2)
        val paramScis = params.map(_(paramPos)).distinct
        StringConstancyInformation.reduceMultiple(paramScis)
    }

    /**
     * Finalized a given definition state.
     */
    def finalizeDefSite(
        defSite: Int, state: InterproceduralComputationState
    ): Unit = {
        if (defSite < 0) {
            state.appendToFpe2Sci(defSite, getParam(state.params, defSite), reset = true)
        } else {
            stmts(defSite) match {
                case nvmc: NonVirtualMethodCall[V] ⇒
                    NonVirtualMethodCallFinalizer(state).finalizeInterpretation(nvmc, defSite)
                case Assignment(_, _, al: ArrayLoad[V]) ⇒
                    ArrayFinalizer(state, cfg).finalizeInterpretation(al, defSite)
                case Assignment(_, _, vfc: VirtualFunctionCall[V]) ⇒
                    VirtualFunctionCallFinalizer(state, cfg).finalizeInterpretation(vfc, defSite)
                case ExprStmt(_, vfc: VirtualFunctionCall[V]) ⇒
                    VirtualFunctionCallFinalizer(state, cfg).finalizeInterpretation(vfc, defSite)
                case _ ⇒ state.appendToFpe2Sci(
                    defSite, StringConstancyProperty.lb.stringConstancyInformation, reset = true
                )
            }
        }
    }

}

object InterproceduralInterpretationHandler {

    /**
     * @see [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.intraprocedural.IntraproceduralInterpretationHandler]]
     */
    def apply(
        tac:             TACode[TACMethodParameter, DUVar[ValueInformation]],
        ps:              PropertyStore,
        declaredMethods: DeclaredMethods,
        state:           InterproceduralComputationState,
        c:               ProperOnUpdateContinuation
    ): InterproceduralInterpretationHandler = new InterproceduralInterpretationHandler(
        tac, ps, declaredMethods, state, c
    )

}