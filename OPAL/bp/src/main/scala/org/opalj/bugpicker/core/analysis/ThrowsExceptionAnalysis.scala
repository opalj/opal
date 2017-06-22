/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import org.opalj.collection.immutable.Chain
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.instructions.Instruction
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.ReturnInstruction
import org.opalj.ai.Domain
import org.opalj.ai.AIResult
import org.opalj.ai.domain.RecordCFG
import org.opalj.ai.domain.l1.RecordAllThrownExceptions
import org.opalj.ai.domain.l1.ReferenceValues
import org.opalj.issues.Issue
import org.opalj.issues.Relevance
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.LocalVariables
import org.opalj.issues.Operands
import org.opalj.issues.InstructionLocation

/**
 * This analysis identifies those instructions (except of ATHROW) that always lead to an exception.
 *
 * @author Michael Eichberg
 */
object ThrowsExceptionAnalysis {

    type ThrowsExceptionAnalysisDomain = Domain with ReferenceValues with RecordCFG with RecordAllThrownExceptions

    def apply(
        theProject: SomeProject,
        classFile:  ClassFile,
        method:     Method,
        result:     AIResult { val domain: ThrowsExceptionAnalysisDomain }
    ): Chain[Issue] = {

        val operandsArray = result.operandsArray
        val domain = result.domain
        val code = result.code

        //
        // The analysis
        //

        val exceptionThrowingInstructions =
            code collectWithIndex {
                case (pc, i: Instruction) if operandsArray(pc) != null /* <=> i was executed */ &&
                    i != ATHROW && !i.isInstanceOf[ReturnInstruction] /* <=> i may have regular successors  */ &&
                    domain.regularSuccessorsOf(pc).isEmpty /* <=> but i actually does not have a regular successor */ &&
                    (
                        domain.exceptionHandlerSuccessorsOf(pc).nonEmpty ||
                        domain.allThrownExceptions.get(pc).nonEmpty
                    ) ⇒
                    (pc, i)
            }

        //
        // Post-Processing
        //

        val exceptionIssues: Chain[Issue] = {

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
                    allExceptions.map(_.upperTypeBound.head.toJava).mkString(", ")
                }

                // If we have the case:
                // "throw throwsException()", where throwsException always throws an
                // exception then we don't want to report an issue.
                val relevance = {
                    val code = method.body.get
                    val nextPC = code.pcOfNextInstruction(pc)
                    if (nextPC < code.instructions.size && code.instructions(nextPC) == ATHROW)
                        Relevance.CommonIdiom
                    else
                        Relevance.VeryHigh
                }
                Issue(
                    "ThrowsExceptionAnalysis",
                    relevance,
                    "evaluation will always lead to an exception",
                    Set(IssueCategory.Correctness),
                    Set(IssueKind.ThrowsException),
                    List(new InstructionLocation(
                        Some(s"evaluation of $instruction always throws: $exceptions"),
                        theProject,
                        classFile,
                        method,
                        pc,
                        List(
                            new Operands(
                                code,
                                pc,
                                operands.take(instruction.numberOfPoppedOperands { x ⇒ ??? }),
                                result.localsArray(pc)
                            ),
                            new LocalVariables(code, pc, result.localsArray(pc))
                        )
                    ))
                )
            }
        }

        exceptionIssues
    }

}
