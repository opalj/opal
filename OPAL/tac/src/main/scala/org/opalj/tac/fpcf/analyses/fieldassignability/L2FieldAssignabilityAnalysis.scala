/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.SelfReferenceParameter
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode

/**
 * Determines the assignability of a field.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 * @author Maximilian Rüsch
 */
class L2FieldAssignabilityAnalysis private[fieldassignability] (val project: SomeProject)
    extends AbstractFieldAssignabilityAnalysis
    with FPCFAnalysis {

    case class State(field: Field) extends AbstractFieldAssignabilityAnalysisState
        with LazyInitializationAnalysisState
    type AnalysisState = State
    override def createState(field: Field): AnalysisState = State(field)

    private class L2LazyInitializationPart(val project: SomeProject) extends LazyInitializationAnalysis {
        override type AnalysisState = L2FieldAssignabilityAnalysis.this.AnalysisState
    }
    private class L2ClonePatternPart(val project: SomeProject) extends ClonePatternAnalysis {
        override type AnalysisState = L2FieldAssignabilityAnalysis.this.AnalysisState
        override val isSameInstance = L2FieldAssignabilityAnalysis.this.isSameInstance
        override val isWrittenInstanceUseSiteSafe = L2FieldAssignabilityAnalysis.this
            .isWrittenInstanceUseSiteSafe
        override val pathExists = L2FieldAssignabilityAnalysis.this.pathExists
    }
    private class L2ReadWritePathPart(val project: SomeProject) extends ExtensiveReadWritePathAnalysis {
        override type AnalysisState = L2FieldAssignabilityAnalysis.this.AnalysisState
        override val isSameInstance = L2FieldAssignabilityAnalysis.this.isSameInstance
        override val isWrittenInstanceUseSiteSafe = L2FieldAssignabilityAnalysis.this
            .isWrittenInstanceUseSiteSafe
        override val pathExists = L2FieldAssignabilityAnalysis.this.pathExists
    }

    override protected lazy val parts: Seq[FieldAssignabilityAnalysisPart] = List(
        new L2LazyInitializationPart(project),
        new L2ClonePatternPart(project),
        new L2ReadWritePathPart(project)
    )

    /**
     * Identifies easy cases of definitively answering instance equality, considering that (at least) as long as DU-UD
     * chains are not collapsed in TAC one cannot be sure that two instances are disjoint, i.e. that they are different.
     */
    private def isSameInstance(tac: TACode[TACMethodParameter, V], firstVar: V, secondVar: V): Answer = {
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
    private def isWrittenInstanceUseSiteSafe(tac: TACode[TACMethodParameter, V], writePC: Int, use: Int): Boolean = {
        val stmt = tac.stmts(use)

        // We consider the access safe when ...
        stmt.pc == writePC || // ... the use site is a known write OR ...
            // ... we easily identify the use site to be a field update OR ...
            // IMPROVE: Can we use field access information to care about reflective accesses here?
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

object EagerL2FieldAssignabilityAnalysis extends AbstractEagerFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L2FieldAssignabilityAnalysis(p)
}

object LazyL2FieldAssignabilityAnalysis extends AbstractLazyFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L2FieldAssignabilityAnalysis(p)
}
