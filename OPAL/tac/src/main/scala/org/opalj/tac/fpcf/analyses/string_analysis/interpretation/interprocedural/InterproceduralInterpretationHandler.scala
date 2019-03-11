/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.ProperOnUpdateContinuation
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.fpcf.cg.properties.Callees
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
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer.GetFieldFinalizer
import org.opalj.tac.SimpleValueConst
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer.StaticFunctionCallFinalizer

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
        tac:                    TACode[TACMethodParameter, DUVar[ValueInformation]],
        ps:                     PropertyStore,
        declaredMethods:        DeclaredMethods,
        fieldAccessInformation: FieldAccessInformation,
        state:                  InterproceduralComputationState,
        c:                      ProperOnUpdateContinuation
) extends InterpretationHandler(tac) {

    /**
     * Processed the given definition site in an interprocedural fashion.
     * <p>
     *
     * @inheritdoc
     */
    override def processDefSite(
        defSite: Int, params: List[Seq[StringConstancyInformation]] = List()
    ): EOptionP[Entity, StringConstancyProperty] = {
        // Without doing the following conversion, the following compile error will occur: "the
        // result type of an implicit conversion must be more specific than org.opalj.fpcf.Entity"
        val e: Integer = defSite.toInt
        // Function parameters are not evaluated when none are present (this always includes the
        // implicit parameter for "this" and for exceptions thrown outside the current function)
        if (defSite < 0 &&
            (params.isEmpty || defSite == -1 || defSite <= ImmediateVMExceptionsOriginOffset)) {
            state.setInterimFpe2Sci(defSite, StringConstancyInformation.lb)
            return FinalEP(e, StringConstancyProperty.lb)
        } else if (defSite < 0) {
            val sci = getParam(params, defSite)
            state.setInterimFpe2Sci(defSite, sci)
            return FinalEP(e, StringConstancyProperty(sci))
        } else if (processedDefSites.contains(defSite)) {
            state.setInterimFpe2Sci(defSite, StringConstancyInformation.getNeutralElement)
            return FinalEP(e, StringConstancyProperty.getNeutralElement)
        }
        // Note that def sites referring to constant expressions will be deleted further down
        processedDefSites(defSite) = Unit

        val callees = state.callees
        stmts(defSite) match {
            case Assignment(_, _, expr: StringConst) ⇒ processConstExpr(expr, defSite)
            case Assignment(_, _, expr: IntConst)    ⇒ processConstExpr(expr, defSite)
            case Assignment(_, _, expr: FloatConst)  ⇒ processConstExpr(expr, defSite)
            case Assignment(_, _, expr: DoubleConst) ⇒ processConstExpr(expr, defSite)
            case Assignment(_, _, expr: ArrayLoad[V]) ⇒
                processArrayLoad(expr, defSite, params)
            case Assignment(_, _, expr: New)                    ⇒ processNew(expr, defSite)
            case Assignment(_, _, expr: GetStatic)              ⇒ processGetStatic(expr, defSite)
            case ExprStmt(_, expr: GetStatic)                   ⇒ processGetStatic(expr, defSite)
            case Assignment(_, _, expr: VirtualFunctionCall[V]) ⇒ processVFC(expr, defSite, params)
            case ExprStmt(_, expr: VirtualFunctionCall[V])      ⇒ processVFC(expr, defSite, params)
            case Assignment(_, _, expr: StaticFunctionCall[V]) ⇒
                processStaticFunctionCall(expr, defSite, params)
            case ExprStmt(_, expr: StaticFunctionCall[V]) ⇒
                processStaticFunctionCall(expr, defSite, params)
            case Assignment(_, _, expr: BinaryExpr[V]) ⇒ processBinaryExpr(expr, defSite)
            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) ⇒
                processNonVirtualFunctionCall(expr, defSite)
            case Assignment(_, _, expr: GetField[V]) ⇒ processGetField(expr, defSite)
            case vmc: VirtualMethodCall[V] ⇒
                processVirtualMethodCall(vmc, defSite, callees)
            case nvmc: NonVirtualMethodCall[V] ⇒ processNonVirtualMethodCall(nvmc, defSite)
            case _ ⇒
                state.setInterimFpe2Sci(defSite, StringConstancyInformation.getNeutralElement)
                FinalEP(e, StringConstancyProperty.getNeutralElement)
        }
    }

    /**
     * Helper / utility function for processing [[StringConst]], [[IntConst]], [[FloatConst]], and
     * [[DoubleConst]].
     */
    private def processConstExpr(
        constExpr: SimpleValueConst, defSite: Int
    ): EOptionP[Entity, StringConstancyProperty] = {
        val finalEP = constExpr match {
            case ic: IntConst    ⇒ new IntegerValueInterpreter(cfg, this).interpret(ic, defSite)
            case fc: FloatConst  ⇒ new FloatValueInterpreter(cfg, this).interpret(fc, defSite)
            case dc: DoubleConst ⇒ new DoubleValueInterpreter(cfg, this).interpret(dc, defSite)
            case sc ⇒ new StringConstInterpreter(cfg, this).interpret(
                sc.asInstanceOf[StringConst], defSite
            )
        }
        val sci = finalEP.asFinal.p.stringConstancyInformation
        state.appendToFpe2Sci(defSite, sci)
        state.setInterimFpe2Sci(defSite, sci)
        processedDefSites.remove(defSite)
        finalEP
    }

    /**
     * Helper / utility function for processing [[ArrayLoad]]s.
     */
    private def processArrayLoad(
        expr: ArrayLoad[V], defSite: Int, params: List[Seq[StringConstancyInformation]]
    ): EOptionP[Entity, StringConstancyProperty] = {
        val r = new ArrayPreparationInterpreter(
            cfg, this, state, params
        ).interpret(expr, defSite)
        val sci = if (r.isFinal) {
            r.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
        } else {
            processedDefSites.remove(defSite)
            StringConstancyInformation.lb
        }
        state.setInterimFpe2Sci(defSite, sci)
        r
    }

    /**
     * Helper / utility function for processing [[New]] expressions.
     */
    private def processNew(expr: New, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        val finalEP = new NewInterpreter(cfg, this).interpret(
            expr, defSite
        )
        val sci = finalEP.asFinal.p.stringConstancyInformation
        state.appendToFpe2Sci(defSite, sci)
        state.setInterimFpe2Sci(defSite, sci)
        finalEP
    }

    /**
     * Helper / utility function for processing [[GetStatic]]s.
     */
    private def processGetStatic(
        expr: GetStatic, defSite: Int
    ): EOptionP[Entity, StringConstancyProperty] = {
        val result = new InterproceduralGetStaticInterpreter(cfg, this).interpret(
            expr, defSite
        )
        doInterimResultHandling(result, defSite)
        result
    }

    /**
     * Helper / utility function for interpreting [[VirtualFunctionCall]]s.
     */
    private def processVFC(
        expr:    VirtualFunctionCall[V],
        defSite: Int,
        params:  List[Seq[StringConstancyInformation]]
    ): EOptionP[Entity, StringConstancyProperty] = {
        val r = new VirtualFunctionCallPreparationInterpreter(
            cfg, this, ps, state, declaredMethods, params, c
        ).interpret(expr, defSite)
        // Set whether the virtual function call is fully prepared. This is the case if 1) the
        // call was not fully prepared before (no final result available) or 2) the preparation is
        // now done (methodPrep2defSite makes sure we have the TAC ready for a method required by
        // this virtual function call).
        val isFinalResult = r.isFinal
        if (!isFinalResult && !state.isVFCFullyPrepared.contains(expr)) {
            state.isVFCFullyPrepared(expr) = false
        } else if (state.isVFCFullyPrepared.contains(expr) && state.methodPrep2defSite.isEmpty) {
            state.isVFCFullyPrepared(expr) = true
        }
        val isPrepDone = !state.isVFCFullyPrepared.contains(expr) || state.isVFCFullyPrepared(expr)

        // In case no final result could be computed, remove this def site from the list of
        // processed def sites to make sure that is can be compute again (when all final
        // results are available); we use nonFinalFunctionArgs because if it does not
        // contain expr, it can be finalized later on without processing the function again.
        // A differentiation between "toString" and other calls is made since toString calls are not
        // prepared in the same way as other calls are as toString does not take any arguments that
        // might need to be prepared (however, toString needs a finalization procedure)
        if (expr.name == "toString" &&
            (state.nonFinalFunctionArgs.contains(expr) || !isFinalResult)) {
            processedDefSites.remove(defSite)
        } else if (state.nonFinalFunctionArgs.contains(expr) || !isPrepDone) {
            processedDefSites.remove(defSite)
        }

        doInterimResultHandling(r, defSite)
        r
    }

    /**
     * Helper / utility function for processing [[StaticFunctionCall]]s.
     */
    private def processStaticFunctionCall(
        expr: StaticFunctionCall[V], defSite: Int, params: List[Seq[StringConstancyInformation]]
    ): EOptionP[Entity, StringConstancyProperty] = {
        val r = new InterproceduralStaticFunctionCallInterpreter(
            cfg, this, ps, state, params, declaredMethods, c
        ).interpret(expr, defSite)
        if (!r.isInstanceOf[Result] || state.nonFinalFunctionArgs.contains(expr)) {
            processedDefSites.remove(defSite)
        }
        doInterimResultHandling(r, defSite)

        r
    }

    /**
     * Helper / utility function for processing [[BinaryExpr]]s.
     */
    private def processBinaryExpr(
        expr: BinaryExpr[V], defSite: Int
    ): EOptionP[Entity, StringConstancyProperty] = {
        val result = new BinaryExprInterpreter(cfg, this).interpret(expr, defSite)
        val sci = result.asFinal.p.stringConstancyInformation
        state.setInterimFpe2Sci(defSite, sci)
        state.appendToFpe2Sci(defSite, sci)
        result
    }

    /**
     * Helper / utility function for processing [[GetField]]s.
     */
    private def processGetField(
        expr: GetField[V], defSite: Int
    ): EOptionP[Entity, StringConstancyProperty] = {
        val r = new InterproceduralFieldInterpreter(
            state, this, ps, fieldAccessInformation, c
        ).interpret(expr, defSite)
        if (r.isRefinable) {
            processedDefSites.remove(defSite)
        }
        doInterimResultHandling(r, defSite)
        r
    }

    /**
     * Helper / utility function for processing [[NonVirtualMethodCall]]s.
     */
    private def processNonVirtualFunctionCall(
        expr: NonVirtualFunctionCall[V], defSite: Int
    ): EOptionP[Entity, StringConstancyProperty] = {
        val r = new InterproceduralNonVirtualFunctionCallInterpreter(
            cfg, this, ps, state, declaredMethods, c
        ).interpret(expr, defSite)
        if (r.isRefinable || state.nonFinalFunctionArgs.contains(expr)) {
            processedDefSites.remove(defSite)
        }
        doInterimResultHandling(r, defSite)
        r
    }

    /**
     * Helper / utility function for processing [[VirtualMethodCall]]s.
     */
    def processVirtualMethodCall(
        expr: VirtualMethodCall[V], defSite: Int, callees: Callees
    ): EOptionP[Entity, StringConstancyProperty] = {
        val r = new InterproceduralVirtualMethodCallInterpreter(
            cfg, this, callees
        ).interpret(expr, defSite)
        doInterimResultHandling(r, defSite)
        r
    }

    /**
     * Helper / utility function for processing [[NonVirtualMethodCall]]s.
     */
    private def processNonVirtualMethodCall(
        nvmc: NonVirtualMethodCall[V], defSite: Int
    ): EOptionP[Entity, StringConstancyProperty] = {
        val r = new InterproceduralNonVirtualMethodCallInterpreter(
            cfg, this, ps, state, declaredMethods, c
        ).interpret(nvmc, defSite)
        r match {
            case FinalEP(_, p: StringConstancyProperty) ⇒
                state.setInterimFpe2Sci(defSite, p.stringConstancyInformation)
                state.appendToFpe2Sci(defSite, p.stringConstancyInformation)
            case _ ⇒
                state.setInterimFpe2Sci(defSite, StringConstancyInformation.lb)
                processedDefSites.remove(defSite)
        }
        r
    }

    /**
     * This function takes a result, which can be final or not, as well as a definition site. This
     * function handles the steps necessary to provide information for computing intermediate
     * results.
     */
    private def doInterimResultHandling(
        result: EOptionP[Entity, Property], defSite: Int
    ): Unit = {
        val sci = if (result.isFinal) {
            result.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
        } else {
            StringConstancyInformation.lb
        }
        state.setInterimFpe2Sci(defSite, sci)
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
                case Assignment(_, _, gf: GetField[V]) ⇒
                    GetFieldFinalizer(state).finalizeInterpretation(gf, defSite)
                case ExprStmt(_, gf: GetField[V]) ⇒
                    GetFieldFinalizer(state).finalizeInterpretation(gf, defSite)
                case Assignment(_, _, sfc: StaticFunctionCall[V]) ⇒
                    StaticFunctionCallFinalizer(state).finalizeInterpretation(sfc, defSite)
                case ExprStmt(_, sfc: StaticFunctionCall[V]) ⇒
                    StaticFunctionCallFinalizer(state).finalizeInterpretation(sfc, defSite)
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
        tac:                    TACode[TACMethodParameter, DUVar[ValueInformation]],
        ps:                     PropertyStore,
        declaredMethods:        DeclaredMethods,
        fieldAccessInformation: FieldAccessInformation,
        state:                  InterproceduralComputationState,
        c:                      ProperOnUpdateContinuation
    ): InterproceduralInterpretationHandler = new InterproceduralInterpretationHandler(
        tac, ps, declaredMethods, fieldAccessInformation, state, c
    )

}