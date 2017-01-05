/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package bugpicker
package core
package analysis

import org.opalj.br.ClassFile
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
        theProject: SomeProject, classFile: ClassFile, method: Method,
        result: AIResult { val domain: UnGuardedAccessAnalysisDomain }
    ): List[Issue] = {

        import result.domain
        val operandsArray = result.operandsArray
        val code = result.code

        var origins = Map.empty[ValueOrigin, PC]
        var timestamps = Map.empty[domain.Timestamp, PC]

        // TODO We should also log those that are assertions related!

        for {
            (pc, instr: IFXNullInstruction) ← code
            if operandsArray(pc) ne null
        } {

            import code.instructions
            import code.pcOfNextInstruction
            // let's check if the guard is related to an assert statement
            val isAssertionRelated: Boolean =
                (instr match {
                    case _: IFNONNULL         ⇒ Some(instructions(pcOfNextInstruction(pc)))
                    case IFNULL(branchOffset) ⇒ Some(instructions(pc + branchOffset))
                    case _                    ⇒ None
                }) match {
                    case Some(NEW(AssertionError)) ⇒ true
                    case _                         ⇒ false
                }

            if (!isAssertionRelated)
                operandsArray(pc).head match {
                    case domain.DomainSingleOriginReferenceValue(sov) ⇒
                        origins += ((sov.origin, pc))
                        timestamps += ((sov.t, pc))
                    case domain.DomainMultipleReferenceValues(mov) ⇒
                        timestamps += ((mov.t, pc))
                }

        }

        if (origins.isEmpty && timestamps.isEmpty)
            // we don't have any "guard instructions"
            return List.empty;

        val unguardedAccesses =
            for {
                (pc, receiver: domain.ReferenceValue) ← code collectWithIndex {
                    case (pc, ARRAYLENGTH | MONITORENTER) if operandsArray(pc) != null ⇒
                        (pc, operandsArray(pc).head)

                    case (pc, AASTORE) if operandsArray(pc) != null ⇒
                        (pc, operandsArray(pc)(2))

                    case (pc, AALOAD) if operandsArray(pc) != null ⇒
                        (pc, operandsArray(pc)(1))

                    case (pc, _: GETFIELD) if operandsArray(pc) != null ⇒
                        (pc, operandsArray(pc).head)

                    case (pc, _: PUTFIELD) if operandsArray(pc) != null ⇒
                        (pc, operandsArray(pc)(1))

                    case (pc, i: INVOKEVIRTUAL) if operandsArray(pc) != null ⇒
                        (pc, operandsArray(pc)(i.methodDescriptor.parametersCount))

                    case (pc, i: INVOKEINTERFACE) if operandsArray(pc) != null ⇒
                        (pc, operandsArray(pc)(i.methodDescriptor.parametersCount))

                    case (pc, i: INVOKESPECIAL) if operandsArray(pc) != null ⇒
                        (pc, operandsArray(pc)(i.methodDescriptor.parametersCount))
                }
                if receiver.isNull.isUnknown
                if timestamps.contains(receiver.t) ||
                    (receiver.isInstanceOf[domain.SingleOriginValue] &&
                        origins.contains(receiver.asInstanceOf[domain.SingleOriginValue].origin))
            } yield {
                if (timestamps.contains(receiver.t))
                    (
                        timestamps(receiver.t),
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
            for ((guardPC, unguardedAccesses) ← unguardedAccesses.groupBy(f ⇒ f._1 /*by guard*/ )) yield {
                val relevance = unguardedAccesses.map(_._2.value).max

                val unguardedLocations: Seq[IssueLocation] =
                    unguardedAccesses.map { ua ⇒
                        val unguardedAccessPC = ua._3
                        new InstructionLocation(
                            Some("unguarded access"), theProject, classFile, method, unguardedAccessPC
                        )
                    }

                val locations =
                    unguardedLocations :+
                        new InstructionLocation(
                            Some("guard"), theProject, classFile, method, guardPC
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
