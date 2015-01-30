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
import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }
import org.opalj.br.analyses.{ Analysis, AnalysisExecutor, BasicReport, Project, SomeProject }
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.{ ClassFile, Method }
import org.opalj.br.MethodWithBody
import org.opalj.ai.common.XHTML
import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.br.Code
import org.opalj.ai.collectPCWithOperands
import org.opalj.ai.BoundedInterruptableAI
import org.opalj.ai.domain
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ConditionalBranchInstruction
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import org.opalj.br.AnalysisFailedException
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

/**
 * This analysis identifies those instructions
 * (except ATHROW of course) that always just throw an exception.
 *
 * @author Michael Eichberg
 */
object ThrowsExceptionAnalysis {

    type ThrowsExceptionAnalysisDomain = Domain with ReferenceValues with RecordCFG with RecordAllThrownExceptions

    def analyze(
        theProject: SomeProject, classFile: ClassFile, method: Method,
        result: AIResult { val domain: ThrowsExceptionAnalysisDomain }): Seq[StandardIssue] = {

        val operandsArray = result.operandsArray
        val domain = result.domain

        val exceptionThrowingInstructions =
            result.code collectWithIndex {
                case (pc, i: Instruction) if operandsArray(pc) != null &&
                    i != ATHROW && domain.regularSuccessorsOf(pc).isEmpty &&
                    (
                        domain.exceptionHandlerSuccessorsOf(pc).nonEmpty ||
                        domain.allThrownExceptions.get(pc).nonEmpty
                    ) ⇒
                    (pc, i)
            }

        val exceptionIssues: Seq[StandardIssue] = {

            for { (pc, instruction) ← exceptionThrowingInstructions } yield {
                val operands = operandsArray(pc)
                val exceptions = {
                    var allExceptions: Set[domain.DomainSingleOriginReferenceValue] = {
                        if (domain.allThrownExceptions.get(pc).nonEmpty)
                            Set.empty ++ domain.allThrownExceptions.get(pc).get
                        else
                            Set.empty
                    }

                    domain.exceptionHandlerSuccessorsOf(pc).foreach { handlerPC ⇒
                        operandsArray(handlerPC).head match {
                            case domain.DomainSingleOriginReferenceValue(sorv) ⇒
                                allExceptions += sorv
                            case domain.DomainMultipleReferenceValues(morv) ⇒
                                allExceptions ++= morv.values
                        }
                    }
                    allExceptions.map(_.upperTypeBound.first().toJava).mkString(", ")
                }
                StandardIssue(
                    theProject, classFile, Some(method), Some(pc),
                    Some(operands),
                    Some(result.localsArray(pc)),
                    "causes exception",
                    Some("The evaluation of the instruction always throws the exception(s): "+exceptions),
                    Set(IssueCategory.Bug),
                    Set(IssueKind.ThrowsException),
                    Seq.empty,
                    Relevance.VeryHigh
                )
            }
        }

        exceptionIssues
    }

}

