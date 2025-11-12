/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.PC
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.tac.fpcf.analyses.cg.uVarForDefSites

/**
 * Determines whether a field write access corresponds to a lazy initialization of the field.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Maximilian Rüsch
 */
trait ClonePatternAnalysis private[fieldassignability]
    extends FieldAssignabilityAnalysisPart {

    val considerLazyInitialization: Boolean =
        project.config.getBoolean(
            "org.opalj.fpcf.analyses.L2FieldAssignabilityAnalysis.considerLazyInitialization"
        )

    /**
     * @note To be provided by the part user.
     */
    protected val isSameInstance: (tac: TACode[TACMethodParameter, V], firstVar: V, secondVar: V) => Answer

    /**
     * Determines whether a use site of a written instance is "safe", i.e. is recognizable as a pattern that does not
     * make the field assignable in combination with the given write itself.
     *
     * @note To be provided by the part user.
     */
    protected val isWrittenInstanceUseSiteSafe: (tac: TACode[TACMethodParameter, V], writePC: Int, use: Int) => Boolean

    /**
     * Provided with two PCs, determines irreflexively whether there exists a path from the first PC to the second PC in
     * the context of the provided TAC and attached CFG.
     *
     * @note To be provided by the part user.
     */
    protected val pathExists: (fromPC: Int, toPC: Int, tac: TACode[TACMethodParameter, V]) => Boolean

    override def completePatternWithInitializerRead()(implicit state: AnalysisState): Option[FieldAssignability] = None

    override def completePatternWithNonInitializerRead(
        context: Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:  Int,
        receiver: Option[V]
    )(implicit state: AnalysisState): Option[FieldAssignability] = {
        val pathFromReadToSomeWriteExists = state.nonInitializerWrites(context).exists {
            case (writePC, writeReceiver) =>
                val writeReceiverVar = writeReceiver.map(uVarForDefSites(_, tac.pcToIndex))
                if (writeReceiverVar.isDefined && isSameInstance(tac, receiver.get, writeReceiverVar.get).isNo) {
                    false
                } else {
                    pathExists(readPC, writePC, tac)
                }
        }

        if (pathFromReadToSomeWriteExists)
            Some(Assignable)
        else
            None
    }

    override def completePatternWithInitializerWrite()(implicit state: AnalysisState): Option[FieldAssignability] = None

    override def completePatternWithNonInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Option[FieldAssignability] = {
        if (state.field.isStatic)
            return None;

        if (receiver.isEmpty)
            return Some(Assignable);

        if (receiver.get.definedBy.size > 1)
            return Some(Assignable);

        if (receiver.get.definedBy.head <= OriginOfThis)
            return None;

        // Check for a clone pattern: Must be on a fresh instance (i.e. cannot be "this" or a parameter) ...
        val defSite = receiver.get.definedBy.head
        if (!tac.stmts(defSite).asAssignment.expr.isNew)
            return Some(Assignable);

        // ... free of dangerous uses, ...
        val useSites = tac.stmts(defSite).asAssignment.targetVar.usedBy
        if (!useSites.forall(isWrittenInstanceUseSiteSafe(tac, writePC, _)))
            return Some(Assignable);

        // ... and not contain read-write paths, whenever their receivers cannot be proven to be disjoint
        val pathFromSomeReadToWriteExists = state.nonInitializerReads(context).exists {
            case (readPC, readReceiver) =>
                val readReceiverVar = readReceiver.map(uVarForDefSites(_, tac.pcToIndex))
                if (readReceiverVar.isDefined && isSameInstance(tac, receiver.get, readReceiverVar.get).isNo) {
                    false
                } else {
                    pathExists(readPC, writePC, tac)
                }
        }

        if (pathFromSomeReadToWriteExists)
            Some(Assignable)
        else
            Some(NonAssignable)
    }
}
