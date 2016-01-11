/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

import java.net.URL
import scala.xml.Node
import scala.xml.UnprefixedAttribute
import scala.xml.Unparsed
import scala.Console.BLUE
import scala.Console.RED
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.collection.SortedMap
import org.opalj.br.analyses.{Analysis, AnalysisExecutor, BasicReport, Project, SomeProject}
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.{ClassFile, Method}
import org.opalj.br.MethodWithBody
import org.opalj.ai.common.XHTML
import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.br.PC
import org.opalj.br.Code
import org.opalj.ai.collectPCWithOperands
import org.opalj.ai.BoundedInterruptableAI
import org.opalj.ai.domain
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ConditionalBranchInstruction
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import org.opalj.ai.InterpretationFailedException
import org.opalj.br.instructions.ArithmeticInstruction
import org.opalj.br.instructions.BinaryArithmeticInstruction
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.instructions.UnaryArithmeticInstruction
import org.opalj.br.instructions.LNEG
import org.opalj.br.instructions.INEG
import org.opalj.br.instructions.IINC
import org.opalj.br.instructions.ShiftInstruction
import org.opalj.br.instructions.INSTANCEOF
import org.opalj.br.instructions.ISTORE
import org.opalj.br.instructions.IStoreInstruction
import org.opalj.ai.AIResult
import org.opalj.ai.domain.ConcreteIntegerValues
import org.opalj.ai.domain.ConcreteLongValues
import org.opalj.br.instructions.ATHROW
import org.opalj.ai.domain.RecordCFG
import org.opalj.ai.domain.l1.RecordAllThrownExceptions
import org.opalj.ai.domain.l1.ReferenceValues
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

/**
 * Identifies accesses to local
 * reference variables that are once done in a guarded context (w.r.t. its nullness
 * property; guarded by an if instruction) and that are also done in an unguarded context.
 *
 * This is only a very shallow (but always correct) analysis; if we would integrate
 * the analysis with the evaluation process more precise results would be possible.
 *
 * @author Michael Eichberg
 */
object GuardedAndUnguardedAccessAnalysis {

    type UnGuardedAccessAnalysisDomain = Domain with ReferenceValues with RecordCFG with RecordAllThrownExceptions

    def analyze(
        theProject: SomeProject, classFile: ClassFile, method: Method,
        result: AIResult { val domain: UnGuardedAccessAnalysisDomain }
    ): List[StandardIssue] = {

        import result.domain
        val operandsArray = result.operandsArray
        val body = result.code

        var origins = Map.empty[ValueOrigin, PC]
        var timestamps = Map.empty[domain.Timestamp, PC]

        // TODO We should also log those that are assertions related!

        body foreach { (pc, instr) ⇒
            if ((operandsArray(pc) ne null) && instr.isInstanceOf[IFXNullInstruction]) {
                import body.instructions
                import body.pcOfNextInstruction
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
        }

        if (origins.isEmpty && timestamps.isEmpty)
            // we don't have any "guard instructions"
            return List.empty;

        val unguardedAccesses =
            for {
                (pc, receiver: domain.ReferenceValue) ← body collectWithIndex {
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
                val category =
                    if (relevance >= Relevance.VeryHigh.value)
                        IssueCategory.Bug
                    else
                        IssueCategory.Smell
                val issues = unguardedAccesses.map(ua ⇒ (ua._3, "unguarded access"))
                StandardIssue(
                    "GuardedAndUnguardedAccessAnalysis",
                    theProject, classFile, field = None, Some(method), Some(guardPC),
                    Some(operandsArray(guardPC)),
                    None,
                    "guard",
                    Some(s"Unguarded local variable access (${operandsArray(guardPC).head}) though explicit test is done elsewhere."),
                    Set(category),
                    Set(IssueKind.UnguardedUse),
                    issues,
                    Relevance(relevance)
                )
            }

        unguardedAccessesIssues.toList
    }

}

