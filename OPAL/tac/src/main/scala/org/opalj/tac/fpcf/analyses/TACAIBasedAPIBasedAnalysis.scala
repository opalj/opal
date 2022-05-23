/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.UBP
import org.opalj.fpcf.UBPS
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.tac.fpcf.analyses.cg.uVarForDefSites
import org.opalj.tac.fpcf.analyses.cg.V

/**
 * An [[APIBasedAnalysis]] that ensures that whenever `processNewCaller` gets called,
 * some (interim) version of the three-address code is available in the property store.
 * For each update of [[org.opalj.tac.fpcf.properties.TACAI]] that actually contains a three-address
 * code, `processNewCaller` is invoked, i.e., it might be called multiple times for the same caller.
 * Due to monotonicity required for all results, this is still sound.
 *
 * @author Florian Kuebler
 */
trait TACAIBasedAPIBasedAnalysis extends APIBasedAnalysis {
    final override def handleNewCaller(
        calleeContext: ContextType, callerContext: ContextType, pc: Int, isDirect: Boolean
    ): ProperPropertyComputationResult = {
        val tacEOptP = ps(callerContext.method.definedMethod, TACAI.key)
        if (isDirect)
            continueDirectCallWithTAC(calleeContext, callerContext, pc)(tacEOptP)
        else {
            val calleesEOptP = ps(callerContext.method, Callees.key)
            continueIndirectCallWithTACOrCallees(calleeContext, callerContext, pc, tacEOptP, calleesEOptP)(tacEOptP)
        }
    }

    private[this] def continueDirectCallWithTAC(
        calleeContext: ContextType, callerContext: ContextType, pc: Int
    )(tacEOptP: SomeEOptionP): ProperPropertyComputationResult = tacEOptP match {
        case UBPS(tac: TheTACAI, isFinal) =>
            val theTAC = tac.theTAC
            val callStmt = theTAC.stmts(theTAC.properStmtIndexForPC(pc))
            val call = retrieveCall(callStmt)
            val tgtVarOpt =
                if (callStmt.isAssignment)
                    Some(callStmt.asAssignment.targetVar)
                else
                    None
            val result = processNewCaller(
                calleeContext,
                callerContext,
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
                val continuationResult = InterimPartialResult(
                    Set(tacEOptP),
                    continueDirectCallWithTAC(calleeContext, callerContext, pc)
                )
                Results(result, continuationResult)
            }

        case _ =>
            InterimPartialResult(
                Set(tacEOptP),
                continueDirectCallWithTAC(calleeContext, callerContext, pc)
            )
    }

    private[this] def processNewCaller(
        calleeContext: ContextType,
        callerContext: ContextType,
        pc:            Int,
        calleesEPS:    EPS[DeclaredMethod, Callees],
        tacEPS:        EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        val tac = tacEPS.ub.tac.get
        val callees = calleesEPS.ub
        val receiverOption = callees.indirectCallReceiver(
            callerContext, pc, calleeContext
        ).map(uVarForDefSites(_, tac.pcToIndex))
        val params = callees.indirectCallParameters(
            callerContext, pc, calleeContext
        ).map(_.map(uVarForDefSites(_, tac.pcToIndex)))

        val callStmt = tac.stmts(tac.properStmtIndexForPC(pc))
        val tgtVarOpt =
            if (callStmt.isAssignment)
                Some(callStmt.asAssignment.targetVar)
            else
                None

        val result = processNewCaller(
            calleeContext, callerContext, pc, tac, receiverOption, params, tgtVarOpt, isDirect = false
        )
        if (tacEPS.isFinal)
            result
        else {
            val continuationResult =
                InterimPartialResult(
                    Set(tacEPS),
                    continueIndirectCallWithTACOrCallees(
                        calleeContext, callerContext, pc, tacEPS, calleesEPS
                    )
                )
            Results(result, continuationResult)
        }
    }

    private[this] def continueIndirectCallWithTACOrCallees(
        calleeContext: ContextType,
        callerContext: ContextType,
        pc:            Int,
        tacEOptP:      EOptionP[Method, TACAI],
        calleesEOptP:  EOptionP[DeclaredMethod, Callees]
    )(someEOptionP: SomeEOptionP): ProperPropertyComputationResult = someEOptionP match {
        case UBP(_: TheTACAI) if calleesEOptP.isEPS && calleesEOptP.ub.containsIndirectCall(callerContext, pc, calleeContext) =>
            processNewCaller(
                calleeContext,
                callerContext,
                pc,
                calleesEOptP.asEPS,
                someEOptionP.asInstanceOf[EPS[Method, TACAI]]
            )

        case UBP(callees: Callees) if tacEOptP.isEPS && tacEOptP.ub.tac.isDefined && callees.containsIndirectCall(callerContext, pc, calleeContext) =>
            processNewCaller(
                calleeContext,
                callerContext,
                pc,
                someEOptionP.asInstanceOf[EPS[DeclaredMethod, Callees]],
                tacEOptP.asEPS
            )

        case _ =>
            InterimPartialResult(
                Set(tacEOptP, calleesEOptP),
                continueIndirectCallWithTACOrCallees(
                    calleeContext, callerContext, pc, tacEOptP, calleesEOptP
                )
            )
    }

    private[this] def retrieveCall(callStmt: Stmt[V]): Call[V] = callStmt match {
        case VirtualFunctionCallStatement(call)    => call
        case NonVirtualFunctionCallStatement(call) => call
        case StaticFunctionCallStatement(call)     => call
        case call: MethodCall[V]                   => call
        case _                                     => throw new MatchError(callStmt)
    }

    def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult
}
