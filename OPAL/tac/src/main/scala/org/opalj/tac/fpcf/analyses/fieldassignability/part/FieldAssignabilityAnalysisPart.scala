/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability
package part

import org.opalj.br.fpcf.FPCFAnalysis

/**
 * A collection of common tools for field assignability analysis parts, as well as a convenience type bound for state.
 * Implementors should call `registerPart` (see [[PartAnalysisAbstractions]] and for some example
 * [[LazyInitializationAnalysis]]) to realise the part as a mixin for [[AbstractFieldAssignabilityAnalysis]] subclasses.
 *
 * @author Maximilian Rüsch
 */
trait FieldAssignabilityAnalysisPart private[fieldassignability]
    extends FPCFAnalysis with PartAnalysisAbstractions {

    override type AnalysisState <: AbstractFieldAssignabilityAnalysisState

    /**
     * Provided with two PCs, determines whether there exists a path from the first PC to the second PC in the context
     * of the provided TAC and attached CFG. This check is not reflexive.
     *
     * IMPROVE abort on path found instead of computing all reachable BBs
     */
    protected def pathExists(fromPC: Int, toPC: Int, tac: TACode[TACMethodParameter, V]): Boolean = {
        val firstBB = tac.cfg.bb(tac.pcToIndex(fromPC))
        val secondBB = tac.cfg.bb(tac.pcToIndex(toPC))

        if (firstBB == secondBB) fromPC < toPC
        else firstBB.reachable().contains(secondBB)
    }

    /**
     * Identifies easy cases of definitively answering instance equality, considering that (at least) as long as DU-UD
     * chains are not collapsed in TAC one cannot be sure that two instances are disjoint, i.e. that they are different.
     */
    protected def isSameInstance(tac: TACode[TACMethodParameter, V], firstVar: V, secondVar: V): Answer = {
        def isFresh(value: V): Boolean = {
            value.definedBy.size == 1 && {
                val defSite = value.definedBy.head
                defSite >= 0 && tac.stmts(defSite).asAssignment.expr.isNew
            }
        }

        // The two given instances may be categorized into: 1. fresh, 2. this, 3. easy case of formal parameter,
        // 4. none of the above. If both instances fall into some category 1-3, but not into the same, they are secured
        // to be disjoint.
        val instanceInfo = Seq(
            (isFresh(firstVar), isFresh(secondVar)),
            (firstVar.definedBy == SelfReferenceParameter, secondVar.definedBy == SelfReferenceParameter),
            (firstVar.definedBy.forall(_ < OriginOfThis), secondVar.definedBy.forall(_ < OriginOfThis))
        )
        if (instanceInfo.contains((true, false)) &&
            instanceInfo.contains((false, true)) &&
            !instanceInfo.contains(true, true)
        ) {
            No
        } else if (firstVar.definedBy.intersect(secondVar.definedBy).nonEmpty)
            Yes
        else
            Unknown
    }

    /**
     * Determines whether a use site of a written instance is "safe", i.e. is recognizable as a pattern that does not
     * make the field assignable in combination with the given write itself.
     */
    protected def isWrittenInstanceUseSiteSafe(tac: TACode[TACMethodParameter, V], writePC: Int, use: Int): Boolean = {
        val stmt = tac.stmts(use)

        // We consider the access safe when ...
        stmt.pc == writePC || // ... the use site is a known write OR ...
            // ... we easily identify the use site to be a field update OR ...
            stmt.isFieldWriteAccessStmt ||
            // ... we identify easy instances of field reads (interaction with potentially disruptive reads
            // is checked separately; note that this inference relies on a flat TAC) OR ...
            stmt.isAssignment && stmt.asAssignment.expr.isGetField ||
            // ... we easily identify the use site to initialize an object (fine as initializer reads outside the
            // current method are forbidden) OR ...
            stmt.isMethodCall && stmt.asMethodCall.name == "<init>" ||
            // ... the statement is irrelevant, since there is no instance where the write did not take effect yet
            !pathExists(stmt.pc, writePC, tac)
    }
}
