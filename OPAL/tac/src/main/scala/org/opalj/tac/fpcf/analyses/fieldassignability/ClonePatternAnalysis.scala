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
 * Determines the assignability of a field based on whether a clone pattern is detected. Aborts the analysis when
 * rejecting a clone pattern match would lead to unsoundness based on the available properties.
 *
 * @author Maximilian Rüsch
 */
trait ClonePatternAnalysis private[fieldassignability]
    extends FieldAssignabilityAnalysisPart {

    override def completePatternWithInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Option[FieldAssignability] = None

    override def completePatternWithNonInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   Int,
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

    override def completePatternWithInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Option[FieldAssignability] = None

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
