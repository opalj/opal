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
package ai

import java.net.URL

import org.opalj.br.Method
import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.IF0Instruction
import org.opalj.br.instructions.IFICMPInstruction
import org.opalj.br.instructions.IFNULL
import org.opalj.br.instructions.IFNONNULL
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis

/**
 * A shallow analysis that identifies useless computations.
 *
 * @author Michael Eichberg
 */
object UselessComputations extends DefaultOneStepAnalysis {

    class AnalysisDomain(val project: Project[java.net.URL], val method: Method)
        extends CorrelationalDomain
        with domain.DefaultDomainValueBinding
        with domain.TheProject
        with domain.TheMethod
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l1.DefaultReferenceValuesBinding
        with domain.l1.DefaultIntegerRangeValues
        with domain.l1.DefaultLongValues
        with domain.l1.ConcretePrimitiveValuesConversions
        with domain.l1.LongValuesShiftOperators {

        override def maxCardinalityOfIntegerRanges: Long = 4L
    }

    override def title: String = "useless computations"

    override def description: String = {
        "identifies computations that are useless, e.g., "+
            "comparison against null if the value is known not be null"
    }

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val results = {
            val results = for {
                classFile ← theProject.allProjectClassFiles.par
                method @ MethodWithBody(body) ← classFile.methods
                result = BaseAI(method, new AnalysisDomain(theProject, method))
            } yield {
                import result._
                val results = collectPCWithOperands(domain)(body, operandsArray) {
                    case (
                        pc,
                        IFNULL(_) | IFNONNULL(_),
                        Seq(domain.DomainReferenceValue(value), _*)
                        ) if value.isNull.isYesOrNo ⇒
                        UselessComputation(method, pc, "useless comparison with null")
                    case (
                        pc,
                        _: IFICMPInstruction[_],
                        Seq(domain.ConcreteIntegerValue(a), domain.ConcreteIntegerValue(b), _*)
                        ) ⇒
                        UselessComputation(method, pc, "comparison of constant values: "+a+", "+b)
                    case (
                        pc,
                        _: IF0Instruction[_],
                        Seq(domain.ConcreteIntegerValue(a), _*)
                        ) ⇒
                        UselessComputation(method, pc, "comparison of 0 with constant value: "+a)
                }
                // Let's do some filtering to get rid of some "false positives"
                // (from the point of view of the Java source code).

                // As a first step we group the results by line.
                var resultsGroupedByLine: Map[Int, Iterable[UselessComputation]] =
                    results.groupBy(_.line.getOrElse(-1))

                // If we have more than one message per line, we are probably dealing
                // we duplicated code (which is automatically generated by the compiler
                // for try-catch-finally statements).
                resultsGroupedByLine = resultsGroupedByLine.map { e ⇒
                    val (ln, uc) = e
                    val results = uc.groupBy(_.opcode).values.filter(_.size == 1)
                    (ln, results.flatten)
                }
                resultsGroupedByLine.values.filter(_.nonEmpty).flatten
            }
            results.flatten
        }

        BasicReport(
            results.mkString("Useless computations: "+results.size+"): \n", "\n", "\n")
        )
    }
}

case class UselessComputation(method: Method, pc: Int, message: String) {

    def opcode: Int = method.body.get.instructions(pc).opcode

    def line: Option[Int] = method.body.get.lineNumber(pc)

    override def toString: String = {
        import Console._

        val line = this.line.map("(line:"+_+")").getOrElse("")

        "useless computation "+method.toJava(s"$BLUE$pc$line: $RED$message$RESET")
    }

}
