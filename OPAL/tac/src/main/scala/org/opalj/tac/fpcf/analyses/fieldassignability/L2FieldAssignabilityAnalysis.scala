/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.Field
import org.opalj.br.PC
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.tac.SelfReferenceParameter
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.cg.uVarForDefSites

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
    with LazyInitializationAnalysis
    with FPCFAnalysis {

    case class State(field: Field) extends AbstractFieldAssignabilityAnalysisState
        with LazyInitializationAnalysisState
    type AnalysisState = State
    override def createState(field: Field): AnalysisState = State(field)

    override def analyzeInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: State): FieldAssignability = {
        val assignability = completeLazyInitPatternForInitializerRead()
        if (assignability == Assignable)
            return Assignable;

        // Initializer reads are incompatible with arbitrary writes in other methods
        if (state.nonInitializerWrites.nonEmpty)
            return Assignable;

        // Analyzing read-write paths interprocedurally is not supported yet
        if (state.initializerWrites.exists(_._1.method ne context.method))
            return Assignable;

        val pathFromReadToSomeWriteExists = state.initializerWrites(context).exists {
            case (writePC, _) =>
                pathExists(readPC, writePC, tac)
        }

        if (pathFromReadToSomeWriteExists)
            Assignable
        else
            NonAssignable
    }

    override def analyzeNonInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: State): FieldAssignability = {
        val assignability = completeLazyInitPatternForNonInitializerRead(context, readPC)
        if (assignability == Assignable)
            return Assignable;

        // Completes the analysis of the clone pattern by recognizing unsafe read-write paths on the same field
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
            Assignable
        else
            NonAssignable
    }

    override def analyzeInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: State): FieldAssignability = {
        val assignability = completeLazyInitPatternForInitializerWrite()
        if (assignability == Assignable)
            return Assignable;

        val method = context.method.definedMethod
        if (state.field.isStatic && method.isConstructor || state.field.isNotStatic && method.isStaticInitializer)
            return Assignable; // TODO check this generally above

        // For instance writes, there might be premature value escapes through arbitrary use sites.
        // If we cannot guarantee that all use sites of the written instance are safe, we must soundly abort.
        if (state.field.isNotStatic) {
            // We only support modifying fields in initializers on newly created instances (which is always "this")
            if (receiver.isEmpty || receiver.get.definedBy != SelfReferenceParameter)
                return Assignable;

            if (!tac.params.thisParameter.useSites.forall(isWrittenInstanceUseSiteSafe(tac, writePC)))
                return Assignable;
        }

        // Analyzing read-write paths interprocedurally is not supported yet
        if (state.initializerReads.exists(_._1.method ne context.method))
            return Assignable;

        val pathFromSomeReadToWriteExists = state.initializerReads(context).exists {
            case (readPC, readReceiver) =>
                val readReceiverVar = readReceiver.map(uVarForDefSites(_, tac.pcToIndex))
                if (readReceiverVar.isDefined && isSameInstance(tac, receiver.get, readReceiverVar.get).isNo) {
                    false
                } else {
                    pathExists(readPC, writePC, tac)
                }
        }

        if (pathFromSomeReadToWriteExists)
            Assignable
        else
            NonAssignable
    }

    override def analyzeNonInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: State): FieldAssignability = {
        if (state.field.isStatic || receiver.isDefined && receiver.get.definedBy == SelfReferenceParameter) {
            completeLazyInitPatternForNonInitializerWrite(context, tac, writePC)
        } else if (receiver.isDefined) {
            // Check for a clone pattern: Must be on a fresh instance (i.e. cannot be "this" or a parameter) ...
            if (receiver.get.definedBy.exists(_ <= OriginOfThis) || receiver.get.definedBy.size > 1)
                return Assignable;
            val defSite = receiver.get.definedBy.head
            if (!tac.stmts(defSite).asAssignment.expr.isNew)
                return Assignable;

            // ... free of dangerous uses, ...
            val useSites = tac.stmts(defSite).asAssignment.targetVar.usedBy
            if (!useSites.forall(isWrittenInstanceUseSiteSafe(tac, writePC)))
                return Assignable;

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
                Assignable
            else
                NonAssignable
        } else
            Assignable
    }

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
    private def isWrittenInstanceUseSiteSafe(tac: TACode[TACMethodParameter, V], writePC: Int)(use: Int): Boolean = {
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
