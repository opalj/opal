/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.UBP
import org.opalj.fpcf.UBPS
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

/**
 * A [[APIBasedCallGraphAnalysis]] that ensures that whenever `processNewCaller` gets called,
 * some (interim) version of the three-address code is available in the property store.
 * For each update of [[org.opalj.tac.fpcf.properties.TACAI]] that actually contains a three-address
 * code, `processNewCaller` is invoked, i.e. it might be called multiple times for the same caller.
 * Due to monotonicity required for all results, this is still sound.
 *
 * @author Florian Kuebler
 */
trait TACAIBasedAPIBasedCallGraphAnalysis extends APIBasedCallGraphAnalysis {
    final override def handleNewCaller(
        caller: DefinedMethod, pc: Int, isDirect: Boolean
    ): ProperPropertyComputationResult = {
        val tacEOptP = ps(caller.definedMethod, TACAI.key)
        if (isDirect)
            continueDirectCallWithTAC(caller, pc)(tacEOptP)
        else {
            val calleesEOptP = ps(caller, Callees.key)
            continueIndirectCallWithTACOrCallees(caller, pc, tacEOptP, calleesEOptP)(tacEOptP)
        }
    }

    private[this] def continueDirectCallWithTAC(
        caller: DefinedMethod, pc: Int
    )(tacEOptP: SomeEOptionP): ProperPropertyComputationResult = tacEOptP match {
        case UBPS(tac: TheTACAI, isFinal) ⇒
            val theTAC = tac.theTAC
            val callStmt = theTAC.stmts(theTAC.pcToIndex(pc))
            val call = retrieveCall(callStmt)
            val tgtVarOpt =
                if (callStmt.isAssignment)
                    Some(callStmt.asAssignment.targetVar)
                else
                    None
            val result = processNewCaller(
                caller,
                pc,
                theTAC,
                call.receiverOption,
                call.params.map(Some(_)),
                tgtVarOpt,
                isDirect = true
            )
            if (isFinal)
                result
            else {
                val continuationResult =
                    InterimPartialResult(Some(tacEOptP), continueDirectCallWithTAC(caller, pc))
                Results(result, continuationResult)
            }

        case _ ⇒ InterimPartialResult(Some(tacEOptP), continueDirectCallWithTAC(caller, pc))
    }

    private[this] def processNewCaller(
        caller:     DefinedMethod,
        pc:         Int,
        calleesEPS: EPS[DeclaredMethod, Callees],
        tacEPS:     EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        val tac = tacEPS.ub.tac.get
        val callees = calleesEPS.ub
        val receiverOption = callees.indirectCallReceiver(pc, apiMethod).map(uVarForDefSites(_, tac.pcToIndex))
        val params = callees.indirectCallParameters(pc, apiMethod).map(_.map(uVarForDefSites(_, tac.pcToIndex)))

        val callStmt = tac.stmts(tac.pcToIndex(pc))
        val tgtVarOpt =
            if (callStmt.isAssignment)
                Some(callStmt.asAssignment.targetVar)
            else
                None

        val result = processNewCaller(caller, pc, tac, receiverOption, params, tgtVarOpt, isDirect = false)
        if (tacEPS.isFinal)
            result
        else {
            val continuationResult =
                InterimPartialResult(
                    Some(tacEPS),
                    continueIndirectCallWithTACOrCallees(
                        caller, pc, tacEPS, calleesEPS
                    )
                )
            Results(result, continuationResult)
        }
    }

    private[this] def continueIndirectCallWithTACOrCallees(
        caller:       DefinedMethod,
        pc:           Int,
        tacEOptP:     EOptionP[Method, TACAI],
        calleesEOptP: EOptionP[DeclaredMethod, Callees]
    )(someEOptionP: SomeEOptionP): ProperPropertyComputationResult = someEOptionP match {
        case UBP(_: TheTACAI) if calleesEOptP.isEPS && calleesEOptP.ub.indirectCallees(pc).contains(apiMethod) ⇒
            processNewCaller(
                caller, pc, calleesEOptP.asEPS, someEOptionP.asInstanceOf[EPS[Method, TACAI]]
            )

        case UBP(callees: Callees) if tacEOptP.isEPS && tacEOptP.ub.tac.isDefined && callees.indirectCallees(pc).contains(apiMethod) ⇒
            processNewCaller(
                caller, pc, someEOptionP.asInstanceOf[EPS[DeclaredMethod, Callees]], tacEOptP.asEPS
            )

        case _ ⇒
            InterimPartialResult(
                List(tacEOptP, calleesEOptP),
                continueIndirectCallWithTACOrCallees(caller, pc, tacEOptP, calleesEOptP)
            )
    }

    private[this] def retrieveCall(callStmt: Stmt[V]): Call[V] = callStmt match {
        case VirtualFunctionCallStatement(call)    ⇒ call
        case NonVirtualFunctionCallStatement(call) ⇒ call
        case StaticFunctionCallStatement(call)     ⇒ call
        case call: MethodCall[V]                   ⇒ call
    }

    def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult
}
