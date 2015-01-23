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
import org.opalj.ai.domain.l1.ReferenceValues
import org.opalj.br.instructions.IFNONNULL
import org.opalj.br.instructions.IFNULL

/**
 * @author Michael Eichberg
 */
object UselessComputationsAnalysis {

    def analyze(
        theProject: SomeProject, classFile: ClassFile, method: Method,
        result: AIResult { val domain: Domain with ConcreteIntegerValues with ConcreteLongValues with ReferenceValues }): Seq[StandardIssue] = {

        val methodsWithUselessComputations = {

            val body = result.code

            import result.domain
            import result.operandsArray
            import domain.ConcreteIntegerValue
            import domain.ConcreteLongValue

            val defaultRelevance = Relevance.DefaultRelevance
            val defaultIIncRelevance = Relevance(5)

            collectPCWithOperands(domain)(body, operandsArray) {

                // HANDLING INT VALUES
                //
                case (
                    pc,
                    instr @ BinaryArithmeticInstruction(ComputationalTypeInt),
                    Seq(ConcreteIntegerValue(a), ConcreteIntegerValue(b), _*)
                    ) ⇒
                    // The java "~" operator has no direct representation in bytecode
                    // instead, compilers generate an "ixor" with "-1" as the
                    // second value.
                    if (instr.operator == "^" && a == -1)
                        (
                            pc,
                            s"constant computation: ~$b (<=> $b ${instr.operator} $a).",
                            defaultRelevance

                        )
                    else
                        (
                            pc,
                            s"constant computation: $b ${instr.operator} $a.",
                            defaultRelevance
                        )

                case (pc, instr: INEG.type, Seq(ConcreteIntegerValue(a), _*)) ⇒
                    (
                        pc,
                        s"constant computation: -${a}",
                        defaultRelevance
                    )

                case (pc, instr @ IINC(index, increment), _) if result.domain.intValueOption(result.localsArray(pc)(index)).isDefined ⇒
                    val v = result.domain.intValueOption(result.localsArray(pc)(index)).get
                    val relevance =
                        if (increment == 1 || increment == -1)
                            defaultIIncRelevance
                        else
                            defaultRelevance
                    (pc, s"constant computation (inc): ${v} + $increment", relevance)

                // HANDLING LONG VALUES
                //
                case (
                    pc,
                    instr @ BinaryArithmeticInstruction(ComputationalTypeLong),
                    Seq(ConcreteLongValue(a), ConcreteLongValue(b), _*)
                    ) ⇒
                    (
                        pc,
                        s"constant computation: ${b}l ${instr.operator} ${a}l.",
                        defaultRelevance
                    )
                case (
                    pc,
                    instr @ ShiftInstruction(ComputationalTypeLong),
                    Seq(ConcreteLongValue(a), ConcreteIntegerValue(b), _*)
                    ) ⇒
                    (
                        pc,
                        s"constant computation: ${b}l ${instr.operator} ${a}l.",
                        defaultRelevance
                    )

                case (pc, instr: LNEG.type, Seq(ConcreteLongValue(a), _*)) ⇒
                    (pc, s"constant computation: -${a}l", defaultRelevance)

                // HANDLING REFERENCE VALUES
                //

                case (
                    pc,
                    INSTANCEOF(referenceType),
                    Seq(rv: domain.ReferenceValue, _*)
                    ) if domain.intValueOption(
                    result.operandsArray(pc + INSTANCEOF.length).head).isDefined ⇒
                    (
                        pc,
                        s"useless type test: ${rv.upperTypeBound.map(_.toJava).mkString("", " with ", "")} instanceof ${referenceType.toJava}",
                        defaultRelevance
                    )

                case (
                    pc,
                    (IFNONNULL(_) | IFNULL(_)),
                    Seq(rv: domain.ReferenceValue, _*)
                    ) if rv.isNull.isYesOrNo ⇒
                    (
                        pc,
                        s"useless null check: if($rv != null)",
                        defaultRelevance
                    )

            }.map { issue ⇒
                val (pc, message, relevance) = issue
                StandardIssue(
                    theProject, classFile, Some(method),
                    Some(pc),
                    Some(result.operandsArray(pc)), Some(result.localsArray(pc)),
                    "the expression always evalutes to the same value",
                    Some(message),
                    Set(IssueCategory.Comprehensibility, IssueCategory.Performance),
                    Set(IssueKind.ConstantComputation),
                    Seq.empty,
                    relevance
                )
            }
        }

        methodsWithUselessComputations
    }
}

