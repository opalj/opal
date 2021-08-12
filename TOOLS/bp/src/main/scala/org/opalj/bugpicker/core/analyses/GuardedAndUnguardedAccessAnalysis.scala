/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.instructions.IFXNullInstruction
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.ARRAYLENGTH
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.AALOAD
import org.opalj.br.instructions.IFNONNULL
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.IFNULL
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.ai.domain.RecordCFG
import org.opalj.ai.domain.l1.RecordAllThrownExceptions
import org.opalj.ai.domain.l1.ReferenceValues
import org.opalj.issues.Relevance
import org.opalj.issues.Issue
import org.opalj.issues.Relevance
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueLocation
import org.opalj.issues.InstructionLocation
import org.opalj.issues.IssueKind

/**
 * Identifies accesses to local reference variables that are once done in a guarded context
 * (w.r.t. its nullness property; guarded by an if instruction) and that are also done in
 * an unguarded context.
 *
 * This is only a very shallow analysis that is subject to false positives; to filter
 * potential false positives we filter all those issues where we can identify a
 * control and data-dependency to a derived value. E.g.,
 * {{{
 * def printSize(f : File) : Unit = {
 *  val name = if(f eq null) null else f.getName
 *  if(name == null) throw new NullPointerException;
 *  // here... f is not null; because if f is null at the beginning, name would be null to
 *  // and the method call would have returned abnormally (raised a NullPointerException).
 *  println(f.size)
 * }
 * }}}
 *
 * @author Michael Eichberg
 */
object GuardedAndUnguardedAccessAnalysis {

    type UnGuardedAccessAnalysisDomain = Domain with ReferenceValues with RecordCFG with RecordAllThrownExceptions

    def apply(
        theProject: SomeProject, method: Method,
        result: AIResult { val domain: UnGuardedAccessAnalysisDomain }
    ): List[Issue] = {

        import result.domain
        val operandsArray = result.operandsArray
        val code = result.code

        var origins = Map.empty[ValueOrigin, PC]
        var refIds = Map.empty[domain.RefId, PC]

        // TODO We should also log those that are assertions related!

        for {
            (pc, instr: IFXNullInstruction[_]) <- code
            if operandsArray(pc) ne null
        } {

            import code.instructions
            import code.pcOfNextInstruction
            // let's check if the guard is related to an assert statement
            val isAssertionRelated: Boolean =
                (instr match {
                    case _: IFNONNULL         => Some(instructions(pcOfNextInstruction(pc)))
                    case IFNULL(branchOffset) => Some(instructions(pc + branchOffset))
                    case _                    => None
                }) match {
                    case Some(NEW(AssertionError)) => true
                    case _                         => false
                }

            if (!isAssertionRelated)
                operandsArray(pc).head match {
                    case domain.DomainSingleOriginReferenceValue(sov) =>
                        origins += ((sov.origin, pc))
                        refIds += ((sov.refId, pc))
                    case domain.DomainMultipleReferenceValues(mov) =>
                        refIds += ((mov.refId, pc))
                }

        }

        if (origins.isEmpty && refIds.isEmpty)
            // we don't have any "guard instructions"
            return List.empty;

        val unguardedAccesses =
            for {
                (pc, domain.AReferenceValue(receiver)) <- code.collectWithIndex {
                    case (pc, ARRAYLENGTH | MONITORENTER) if operandsArray(pc) != null =>
                        (pc, operandsArray(pc).head)

                    case (pc, AASTORE) if operandsArray(pc) != null =>
                        (pc, operandsArray(pc)(2))

                    case (pc, AALOAD) if operandsArray(pc) != null =>
                        (pc, operandsArray(pc)(1))

                    case (pc, _: GETFIELD) if operandsArray(pc) != null =>
                        (pc, operandsArray(pc).head)

                    case (pc, _: PUTFIELD) if operandsArray(pc) != null =>
                        (pc, operandsArray(pc)(1))

                    case (pc, i: INVOKEVIRTUAL) if operandsArray(pc) != null =>
                        (pc, operandsArray(pc)(i.methodDescriptor.parametersCount))

                    case (pc, i: INVOKEINTERFACE) if operandsArray(pc) != null =>
                        (pc, operandsArray(pc)(i.methodDescriptor.parametersCount))

                    case (pc, i: INVOKESPECIAL) if operandsArray(pc) != null =>
                        (pc, operandsArray(pc)(i.methodDescriptor.parametersCount))
                }
                if receiver.isNull.isUnknown
                if refIds.contains(receiver.refId) ||
                    (receiver.isInstanceOf[domain.SingleOriginValue] &&
                        origins.contains(receiver.asInstanceOf[domain.SingleOriginValue].origin))
            } yield {
                if (refIds.contains(receiver.refId))
                    (
                        refIds(receiver.refId),
                        Relevance.OfUtmostRelevance,
                        pc
                    )
                else
                    (
                        origins(receiver.asInstanceOf[domain.SingleOriginValue].origin),
                        Relevance.High,
                        pc
                    )
            }

        val unguardedAccessesIssues =
            for {
                (guardPC, unguardedAccesses) <- unguardedAccesses.groupBy(f => f._1 /*by guard*/ )
            } yield {
                val relevance = unguardedAccesses.toIterator.map(_._2.value).max

                val unguardedLocations: Seq[IssueLocation] =
                    unguardedAccesses.map { ua =>
                        val unguardedAccessPC = ua._3
                        new InstructionLocation(
                            Some("unguarded access"), theProject, method, unguardedAccessPC
                        )
                    }.toSeq

                val locations =
                    unguardedLocations :+
                        new InstructionLocation(
                            Some("guard"), theProject, method, guardPC
                        )

                Issue(
                    "GuardedAndUnguardedAccessAnalysis",
                    Relevance(relevance),
                    "a local variable is accessed in a guarded (using an explicit check) and also in an unguarded context",
                    Set(IssueCategory.Correctness),
                    Set(IssueKind.UnguardedUse),
                    locations
                )
            }

        unguardedAccessesIssues.toList
    }

}
