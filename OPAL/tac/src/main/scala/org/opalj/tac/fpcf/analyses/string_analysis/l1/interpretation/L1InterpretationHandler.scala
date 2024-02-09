/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.DoubleValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.FloatValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.IntegerValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.NewInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.StringConstInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l1.finalizer.ArrayLoadFinalizer
import org.opalj.tac.fpcf.analyses.string_analysis.l1.finalizer.GetFieldFinalizer
import org.opalj.tac.fpcf.analyses.string_analysis.l1.finalizer.NewArrayFinalizer
import org.opalj.tac.fpcf.analyses.string_analysis.l1.finalizer.NonVirtualMethodCallFinalizer
import org.opalj.tac.fpcf.analyses.string_analysis.l1.finalizer.StaticFunctionCallFinalizer
import org.opalj.tac.fpcf.analyses.string_analysis.l1.finalizer.VirtualFunctionCallFinalizer

/**
 * Responsible for processing expressions that are relevant in order to determine which value(s) a string read operation
 * might have. These expressions usually come from the definitions sites of the variable of interest.
 * <p>
 * This handler may use [[L1StringInterpreter]]s and general
 * [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.StringInterpreter]]s.
 *
 * @author Patrick Mell
 */
class L1InterpretationHandler(
        ps:                     PropertyStore,
        project:                SomeProject,
        declaredFields:         DeclaredFields,
        fieldAccessInformation: FieldAccessInformation,
        contextProvider:        ContextProvider
) extends InterpretationHandler[L1ComputationState] {

    /**
     * Processed the given definition site in an interprocedural fashion.
     * <p>
     *
     * @inheritdoc
     */
    override def processDefSite(defSite: Int)(implicit
        state: L1ComputationState
    ): EOptionP[Entity, StringConstancyProperty] = {
        // Without doing the following conversion, the following compile error will occur: "the
        // result type of an implicit conversion must be more specific than org.opalj.fpcf.Entity"
        val e: Integer = defSite
        // Function parameters are not evaluated when none are present (this always includes the
        // implicit parameter for "this" and for exceptions thrown outside the current function)
        if (defSite < 0) {
            val params = state.params.toList.map(_.toList)
            if (params.isEmpty || defSite == -1 || defSite <= ImmediateVMExceptionsOriginOffset) {
                state.appendToInterimFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), StringConstancyInformation.lb)
                return FinalEP(e, StringConstancyProperty.lb)
            } else {
                val sci = getParam(params, defSite)
                state.appendToInterimFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), sci)
                return FinalEP(e, StringConstancyProperty(sci))
            }
        } else if (processedDefSites.contains(defSite)) {
            state.appendToInterimFpe2Sci(
                pcOfDefSite(defSite)(state.tac.stmts),
                StringConstancyInformation.getNeutralElement
            )
            return FinalEP(e, StringConstancyProperty.getNeutralElement)
        }
        // Note that def sites referring to constant expressions will be deleted further down
        processedDefSites(defSite) = ()

        state.tac.stmts(defSite) match {
            case Assignment(_, _, expr: StringConst)               => processConstExpr(expr, defSite)
            case Assignment(_, _, expr: IntConst)                  => processConstExpr(expr, defSite)
            case Assignment(_, _, expr: FloatConst)                => processConstExpr(expr, defSite)
            case Assignment(_, _, expr: DoubleConst)               => processConstExpr(expr, defSite) // TODO what about long consts
            case Assignment(_, _, expr: ArrayLoad[V])              => processArrayLoad(expr, defSite)
            case Assignment(_, _, expr: NewArray[V])               => processNewArray(expr, defSite)
            case Assignment(_, _, expr: New)                       => processNew(expr, defSite)
            case Assignment(_, _, expr: GetStatic)                 => processGetField(expr, defSite)
            case ExprStmt(_, expr: GetStatic)                      => processGetField(expr, defSite)
            case Assignment(_, _, expr: VirtualFunctionCall[V])    => processVFC(expr, defSite)
            case ExprStmt(_, expr: VirtualFunctionCall[V])         => processVFC(expr, defSite)
            case Assignment(_, _, expr: StaticFunctionCall[V])     => processStaticFunctionCall(expr, defSite)
            case ExprStmt(_, expr: StaticFunctionCall[V])          => processStaticFunctionCall(expr, defSite)
            case Assignment(_, _, expr: BinaryExpr[V])             => processBinaryExpr(expr, defSite)
            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) => processNonVirtualFunctionCall(expr, defSite)
            case Assignment(_, _, expr: GetField[V])               => processGetField(expr, defSite)
            case vmc: VirtualMethodCall[V]                         => processVirtualMethodCall(vmc, defSite)
            case nvmc: NonVirtualMethodCall[V]                     => processNonVirtualMethodCall(nvmc, defSite)
            case _ =>
                state.appendToInterimFpe2Sci(
                    pcOfDefSite(defSite)(state.tac.stmts),
                    StringConstancyInformation.getNeutralElement
                )
                FinalEP(e, StringConstancyProperty.getNeutralElement)
        }
    }

    /**
     * Helper / utility function for processing [[StringConst]], [[IntConst]], [[FloatConst]], and
     * [[DoubleConst]].
     */
    private def processConstExpr(
        constExpr: SimpleValueConst,
        defSite:   Int
    )(implicit state: L1ComputationState): FinalEP[Entity, StringConstancyProperty] = {
        val finalEP = constExpr match {
            case ic: IntConst    => IntegerValueInterpreter.interpret(ic)
            case fc: FloatConst  => FloatValueInterpreter.interpret(fc)
            case dc: DoubleConst => DoubleValueInterpreter.interpret(dc)
            case sc: StringConst => StringConstInterpreter.interpret(sc)
            case c               => throw new IllegalArgumentException(s"Unsupported const value: $c")
        }
        val sci = finalEP.p.stringConstancyInformation
        state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), sci)
        state.appendToInterimFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), sci)
        processedDefSites.remove(defSite)
        finalEP
    }

    /**
     * Helper / utility function for processing [[ArrayLoad]]s.
     */
    private def processArrayLoad(
        expr:    ArrayLoad[V],
        defSite: Int
    )(implicit state: L1ComputationState): EOptionP[Entity, StringConstancyProperty] = {
        val r = new L1ArrayAccessInterpreter(this).interpret(expr, defSite)
        val sci = if (r.isFinal) {
            r.asFinal.p.stringConstancyInformation
        } else {
            processedDefSites.remove(defSite)
            StringConstancyInformation.lb
        }
        state.appendToInterimFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), sci)
        r
    }

    /**
     * Helper / utility function for processing [[NewArray]]s.
     */
    private def processNewArray(
        expr:    NewArray[V],
        defSite: Int
    )(implicit state: L1ComputationState): EOptionP[Entity, StringConstancyProperty] = {
        val r = new L1NewArrayInterpreter(this).interpret(expr, defSite)
        val sci = if (r.isFinal) {
            r.asFinal.p.stringConstancyInformation
        } else {
            processedDefSites.remove(defSite)
            StringConstancyInformation.lb
        }
        state.appendToInterimFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), sci)
        r
    }

    /**
     * Helper / utility function for processing [[New]] expressions.
     */
    private def processNew(expr: New, defSite: Int)(implicit
        state: L1ComputationState
    ): FinalEP[Entity, StringConstancyProperty] = {
        val finalEP = NewInterpreter.interpret(expr)
        state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), finalEP.p.stringConstancyInformation)
        state.appendToInterimFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), finalEP.p.stringConstancyInformation)
        finalEP
    }

    /**
     * Helper / utility function for interpreting [[VirtualFunctionCall]]s.
     */
    private def processVFC(
        expr:    VirtualFunctionCall[V],
        defSite: Int
    )(implicit state: L1ComputationState): EOptionP[Entity, StringConstancyProperty] = {
        val r = new L1VirtualFunctionCallInterpreter(this, ps, contextProvider).interpret(expr, defSite)
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
            (state.nonFinalFunctionArgs.contains(expr) || !isFinalResult)
        ) {
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
        expr:    StaticFunctionCall[V],
        defSite: Int
    )(implicit state: L1ComputationState): EOptionP[Entity, StringConstancyProperty] = {
        val r = new L1StaticFunctionCallInterpreter(this, ps, contextProvider).interpret(expr, defSite)
        if (r.isRefinable || state.nonFinalFunctionArgs.contains(expr)) {
            processedDefSites.remove(defSite)
        }
        doInterimResultHandling(r, defSite)

        r
    }

    /**
     * Helper / utility function for processing [[BinaryExpr]]s.
     */
    private def processBinaryExpr(expr: BinaryExpr[V], defSite: Int)(implicit
        state: L1ComputationState
    ): FinalEP[Entity, StringConstancyProperty] = {
        // TODO: For binary expressions, use the underlying domain to retrieve the result of such expressions
        val result = BinaryExprInterpreter.interpret(expr)
        val sci = result.p.stringConstancyInformation
        state.appendToInterimFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), sci)
        state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), sci)
        result
    }

    /**
     * Helper / utility function for processing [[GetField]]s.
     */
    private def processGetField(expr: FieldRead[V], defSite: Int)(implicit
        state: L1ComputationState
    ): EOptionP[Entity, StringConstancyProperty] = {
        val r = L1FieldReadInterpreter(ps, fieldAccessInformation, project, declaredFields, contextProvider)
            .interpret(expr, defSite)(state)
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
        expr:    NonVirtualFunctionCall[V],
        defSite: Int
    )(implicit state: L1ComputationState): EOptionP[Entity, StringConstancyProperty] = {
        val r = L1NonVirtualFunctionCallInterpreter(ps, contextProvider).interpret(expr, defSite)
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
        expr:    VirtualMethodCall[V],
        defSite: Int
    )(implicit state: L1ComputationState): EOptionP[Entity, StringConstancyProperty] = {
        val r = L1VirtualMethodCallInterpreter().interpret(expr, defSite)(state)
        doInterimResultHandling(r, defSite)
        r
    }

    /**
     * Helper / utility function for processing [[NonVirtualMethodCall]]s.
     */
    private def processNonVirtualMethodCall(
        nvmc:    NonVirtualMethodCall[V],
        defSite: Int
    )(implicit state: L1ComputationState): EOptionP[Entity, StringConstancyProperty] = {
        val r = L1NonVirtualMethodCallInterpreter(this).interpret(nvmc, defSite)
        r match {
            case FinalEP(_, p: StringConstancyProperty) =>
                state.appendToInterimFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), p.stringConstancyInformation)
                state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), p.stringConstancyInformation)
            case _ =>
                state.appendToInterimFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), StringConstancyInformation.lb)
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
        result:  EOptionP[Entity, Property],
        defSite: Int
    )(implicit state: L1ComputationState): Unit = {
        val sci = if (result.isFinal) {
            result.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
        } else {
            StringConstancyInformation.lb
        }
        state.appendToInterimFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), sci)
    }

    /**
     * Finalized a given definition state.
     */
    def finalizeDefSite(defSite: Int, state: L1ComputationState): Unit = {
        if (defSite < 0) {
            state.appendToFpe2Sci(
                pcOfDefSite(defSite)(state.tac.stmts),
                getParam(state.params.toSeq.map(_.toSeq), defSite),
                reset = true
            )
        } else {
            state.tac.stmts(defSite) match {
                case nvmc: NonVirtualMethodCall[V] =>
                    NonVirtualMethodCallFinalizer(state).finalizeInterpretation(nvmc, defSite)
                case Assignment(_, _, al: ArrayLoad[V]) =>
                    ArrayLoadFinalizer(state).finalizeInterpretation(al, defSite)
                case Assignment(_, _, na: NewArray[V]) =>
                    NewArrayFinalizer(state).finalizeInterpretation(na, defSite)
                case Assignment(_, _, vfc: VirtualFunctionCall[V]) =>
                    VirtualFunctionCallFinalizer(state).finalizeInterpretation(vfc, defSite)
                case ExprStmt(_, vfc: VirtualFunctionCall[V]) =>
                    VirtualFunctionCallFinalizer(state).finalizeInterpretation(vfc, defSite)
                case Assignment(_, _, fr: FieldRead[V]) =>
                    GetFieldFinalizer(state).finalizeInterpretation(fr, defSite)
                case ExprStmt(_, fr: FieldRead[V]) =>
                    GetFieldFinalizer(state).finalizeInterpretation(fr, defSite)
                case Assignment(_, _, sfc: StaticFunctionCall[V]) =>
                    StaticFunctionCallFinalizer(state).finalizeInterpretation(sfc, defSite)
                case ExprStmt(_, sfc: StaticFunctionCall[V]) =>
                    StaticFunctionCallFinalizer(state).finalizeInterpretation(sfc, defSite)
                case _ =>
                    state.appendToFpe2Sci(
                        pcOfDefSite(defSite)(state.tac.stmts),
                        StringConstancyInformation.lb,
                        reset = true
                    )
            }
        }
    }
}

object L1InterpretationHandler {

    def apply(
        ps:                     PropertyStore,
        project:                SomeProject,
        declaredFields:         DeclaredFields,
        fieldAccessInformation: FieldAccessInformation,
        contextProvider:        ContextProvider
    ): L1InterpretationHandler = new L1InterpretationHandler(
        ps,
        project,
        declaredFields,
        fieldAccessInformation,
        contextProvider
    )
}
