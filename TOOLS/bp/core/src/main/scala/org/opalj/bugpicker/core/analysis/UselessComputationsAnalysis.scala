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

import org.opalj.br.analyses.SomeProject
import org.opalj.br.{ ClassFile, Method }
import org.opalj.ai.Domain
import org.opalj.br.PC
import org.opalj.ai.collectPCWithOperands
import org.opalj.br.instructions.BinaryArithmeticInstruction
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.instructions.LNEG
import org.opalj.br.instructions.INEG
import org.opalj.br.instructions.IINC
import org.opalj.br.instructions.ShiftInstruction
import org.opalj.br.instructions.INSTANCEOF
import org.opalj.ai.AIResult
import org.opalj.ai.domain.ConcreteIntegerValues
import org.opalj.ai.domain.ConcreteLongValues
import org.opalj.ai.domain.l1.ReferenceValues
import org.opalj.issues.Relevance
import org.opalj.issues.Issue
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.InstructionLocation
import org.opalj.issues.Operands
import org.opalj.br.instructions.IAND
import org.opalj.br.instructions.IOR

/**
 * Identifies computations that are useless (i.e., computations that could be done
 * in the source code.)
 *
 * @author Michael Eichberg
 */
object UselessComputationsAnalysis {

    type UselessComputationsAnalysisDomain = Domain with ConcreteIntegerValues with ConcreteLongValues with ReferenceValues

    def apply(
        theProject: SomeProject, classFile: ClassFile, method: Method,
        result: AIResult { val domain: UselessComputationsAnalysisDomain }): Seq[Issue] = {

        val defaultRelevance = Relevance.DefaultRelevance
        val defaultIIncRelevance = Relevance(5)

        val code = result.code

        def createIssue(pc: PC, message: String, relevance: Relevance): Issue = {
            val operands = result.operandsArray(pc)
            val localVariables = result.localsArray(pc)
            Issue(
                "UselessComputationsAnalysis",
                relevance,
                s"the expression ($message) always evaluates to the same value",
                Set(IssueCategory.Comprehensibility, IssueCategory.Performance),
                Set(IssueKind.ConstantComputation),
                List(new InstructionLocation(
                    None, theProject, classFile, method, pc,
                    List(new Operands(code, pc, operands, localVariables))
                ))
            )
        }

        import result.domain
        import result.operandsArray
        import domain.ConcreteIntegerValue
        import domain.ConcreteLongValue

        collectPCWithOperands(domain)(code, operandsArray) {

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
                    createIssue(
                        pc,
                        s"constant computation: ~$b (<=> $b ${instr.operator} $a).",
                        defaultRelevance

                    )
                else
                    createIssue(
                        pc, s"constant computation: $b ${instr.operator} $a.", defaultRelevance
                    )

            case (pc, IOR, Seq(ConcreteIntegerValue(0), _*)) ⇒
                createIssue(
                    pc, "0 | x: bit or operation with 0 left operand is useless", Relevance.High
                )

                        case (pc,IOR, Seq(_,ConcreteIntegerValue(0), _*)) ⇒                
                            createIssue(
                    pc,("x | 0: bit or operation with 0 right operand is useless"),Relevance.High
                    )
            //                
            //            case (IOR, IntValue(-1), _) ⇒
            //                Some("-1 | x: bit or operation with -1 left operand always returns -1")
            //            case (IOR, _, IntValue(-1)) ⇒
            //                Some("x | -1: bit or operation with -1 right operand always returns -1")
                
            //            case (IAND, IntValue(0), _) ⇒
            //                Some("0 & x: bit and operation with 0 left operand always returns 0")
            //            case (IAND, IntValue(-1), _) ⇒
            //                Some("-1 & x: bit and operation with -1 left operand is useless")
            //            case (IAND, _, IntValue(0)) ⇒
            //                Some("x & 0: bit and operation with 0 right operand always returns 0")
            //            case (IAND, _, IntValue(-1)) ⇒
            //                Some("x & -1: bit and operation with -1 right operand is useless")
            //            
            case (pc, IAND, Seq(_, ConcreteIntegerValue(-1), _*)) ⇒
                createIssue(
                    pc,
                    s"x & -1: bit and operation with -1 right operand is useless",
                    Relevance.High
                )

            case (pc, instr @ INEG, Seq(ConcreteIntegerValue(a), _*)) ⇒
                createIssue(pc, s"constant computation: -${a}", defaultRelevance)

            case (
                pc,
                instr @ IINC(index, increment),
                _
                ) if domain.intValueOption(result.localsArray(pc)(index)).isDefined ⇒
                val v = domain.intValueOption(result.localsArray(pc)(index)).get
                val relevance =
                    if (increment == 1 || increment == -1)
                        defaultIIncRelevance
                    else
                        defaultRelevance
                createIssue(pc, s"constant computation (inc): ${v} + $increment", relevance)

            // HANDLING LONG VALUES
            //
            case (
                pc,
                instr @ BinaryArithmeticInstruction(ComputationalTypeLong),
                Seq(ConcreteLongValue(a), ConcreteLongValue(b), _*)
                ) ⇒
                createIssue(
                    pc, s"constant computation: ${b}l ${instr.operator} ${a}l.", defaultRelevance
                )
            case (
                pc,
                instr @ ShiftInstruction(ComputationalTypeLong),
                Seq(ConcreteLongValue(a), ConcreteIntegerValue(b), _*)
                ) ⇒
                createIssue(
                    pc, s"constant computation: ${b}l ${instr.operator} ${a}l.", defaultRelevance
                )

            case (pc, LNEG, Seq(ConcreteLongValue(a), _*)) ⇒
                createIssue(pc, s"constant computation: -${a}l", defaultRelevance)

            // HANDLING REFERENCE VALUES
            //

            case (
                pc,
                INSTANCEOF(referenceType),
                Seq(rv: domain.ReferenceValue, _*)
                ) if domain.intValueOption(
                operandsArray(pc + INSTANCEOF.length).head
            ).isDefined ⇒
                createIssue(
                    pc,
                    rv.upperTypeBound.map(_.toJava).mkString(
                        "useless type test:",
                        " with ",
                        " instanceof "+referenceType.toJava
                    ),
                    defaultRelevance
                )

        }
    }
}

