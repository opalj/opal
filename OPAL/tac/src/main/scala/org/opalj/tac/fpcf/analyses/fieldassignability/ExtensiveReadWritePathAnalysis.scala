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
import org.opalj.tac.SelfReferenceParameter
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.cg.uVarForDefSites

/**
 * Determines the assignability of a field based on whether it is possible for a path to exist from a read to a write
 * of the same field on the same instance.
 *
 * This part will always return a value, and will soundly abort if it cannot guarantee that no such path exists. This
 * may be the case e.g. if a path from some read of the field to the write exists, but the analysis cannot prove that
 * the two instances are disjoint.
 *
 * @author Maximilian Rüsch
 */
trait ExtensiveReadWritePathAnalysis private[fieldassignability]
    extends FieldAssignabilityAnalysisPart {

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

    override def completePatternWithInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Some[FieldAssignability] = {
        // Initializer reads are incompatible with arbitrary writes in other methods
        if (state.nonInitializerWrites.nonEmpty)
            return Some(Assignable);

        // Analyzing read-write paths interprocedurally is not supported yet. For static fields, initializer writes are
        // harmless (i.e. executed before) when the read is in a constructor, so the class is already initialized.
        if (state.initializerWrites.exists(_._1.method ne context.method)) {
            if (state.field.isNotStatic)
                return Some(Assignable);
            else if (!context.method.definedMethod.isConstructor)
                return Some(Assignable);
        }

        val pathFromReadToSomeWriteExists = state.initializerWrites(context).exists {
            case (writePC, _) =>
                pathExists(readPC, writePC, tac)
        }

        if (pathFromReadToSomeWriteExists)
            Some(Assignable)
        else
            Some(NonAssignable)
    }

    override def completePatternWithNonInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Option[FieldAssignability] = None

    override def completePatternWithInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Some[FieldAssignability] = {
        val method = context.method.definedMethod
        if (state.field.isStatic && method.isConstructor || state.field.isNotStatic && method.isStaticInitializer)
            return Some(Assignable);

        // For instance writes, there might be premature value escapes through arbitrary use sites.
        // If we cannot guarantee that all use sites of the written instance are safe, we must soundly abort.
        if (state.field.isNotStatic) {
            // We only support modifying fields in initializers on newly created instances (which is always "this")
            if (receiver.isEmpty || receiver.get.definedBy != SelfReferenceParameter)
                return Some(Assignable);

            if (!tac.params.thisParameter.useSites.forall(isWrittenInstanceUseSiteSafe(tac, writePC, _)))
                return Some(Assignable);
        }

        // Analyzing read-write paths interprocedurally is not supported yet. However, for static fields, all
        // reads that take place in instance constructors are harmless.
        if (state.field.isNotStatic && state.initializerReads.exists(_._1.method ne context.method))
            return Some(Assignable);
        if (state.field.isStatic && state.initializerReads.exists {
                case (readContext, _) =>
                    (readContext.method ne context.method) && (
                        !readContext.method.hasSingleDefinedMethod ||
                        readContext.method.definedMethod.isStaticInitializer
                    )
            }
        )
            return Some(Assignable);

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
            Some(Assignable)
        else
            Some(NonAssignable)
    }

    override def completePatternWithNonInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Option[FieldAssignability] = None
}
