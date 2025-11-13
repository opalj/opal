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
     * Analyzing read-write paths interprocedurally is not fully supported yet. Instead, this identifies particular
     * cases where read-write paths are provably impossible, given the information present in the framework / TAC.
     *
     * IMPROVE: The analysis of these accesses does not change over time, thus cache this derivation in the state
     *
     * @note Assumes that the two contexts point to different methods, i.e. that intraprocedural path existence is
     *       handled separately.
     */
    private def canPathExistFromInitializerReadsToInitializerWrites(
        readContext:  Context,
        writeContext: Context
    )(implicit state: AnalysisState): Boolean = {
        if (state.field.isStatic) {
            // Writes to static fields in instance constructors are forbidden, see the write analysis.

            // We can only guarantee that no paths exist when the writing static initializer is guaranteed to run before
            // the reading method, which is in turn only guaranteed when the writing static initializer is in the same
            // type as the field declaration, i.e. it is run when the field is used for the first time.

            // Note that this also implies that there may be no supertype static initializer that can read the field,
            // preventing read-write paths in the inheritance chain.
            writeContext.method.declaringClassType ne state.field.classFile.thisType
        } else {
            true
        }
    }

    override def completePatternWithInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Some[FieldAssignability] = {
        // Initializer reads are incompatible with arbitrary writes in other methods
        if (state.nonInitializerWrites.nonEmpty)
            return Some(Assignable);

        // First, check whether there are any interprocedural writes where paths are not excluded from existence
        if (state.initializerWrites.exists {
                case (writeContext, _) =>
                    (writeContext.method ne context.method) &&
                        canPathExistFromInitializerReadsToInitializerWrites(context, writeContext)
            }
        )
            return Some(Assignable);

        // Second, check read-write paths intraprocedurally, context sensitive to the same access context as the read
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
        // Writing fields outside their initialization scope is not supported.
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

        if (state.initializerReads.exists {
                case (readContext, _) =>
                    (readContext.method ne context.method) &&
                        canPathExistFromInitializerReadsToInitializerWrites(readContext, context)
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
