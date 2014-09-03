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

import java.net.URL
import scala.xml.Node
import scala.xml.UnprefixedAttribute
import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.collection.SortedMap
import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }
import org.opalj.br.analyses.{ Analysis, AnalysisExecutor, BasicReport, Project }
import org.opalj.br.{ ClassFile, Method }
import org.opalj.br.MethodWithBody
import org.opalj.ai.debug.XHTML
import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.ai.domain
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ConditionalBranchInstruction
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import scala.xml.Unparsed

class DeadCodeAnalysis extends Analysis[URL, (Long, Iterable[DeadCode])] {

    override def title: String = "Dead/Useless/Buggy Code Identification"

    override def description: String = "Identifies dead/useless/buggy code using abstract interpretation."

    override def analyze(
        theProject: Project[URL],
        parameters: Seq[String]): (Long, Iterable[DeadCode]) = {

        var analysisTime: Long = 0l
        val methodsWithDeadCode = time {
            /*D*/ val methodsCount = new java.util.concurrent.atomic.AtomicInteger(0)
            /*D*/ val instrCount = new java.util.concurrent.atomic.AtomicInteger(0)
            /*D*/ val trgtsCount = new java.util.concurrent.atomic.AtomicInteger(0)
            /*D*/ val opsCount = new java.util.concurrent.atomic.AtomicInteger(0)
            /*D*/ val resultsCount = new java.util.concurrent.atomic.AtomicInteger(0)

            val results = new java.util.concurrent.ConcurrentLinkedQueue[DeadCode]()
            for {
                classFile ← theProject.classFiles.par
                method @ MethodWithBody(body) ← classFile.methods
            } {
                methodsCount.incrementAndGet()
                val domain = new DeadCodeAnalysisDomain(theProject, method)
                val result = BaseAI(classFile, method, domain)
                val operandsArray = result.operandsArray
                val methodWithDeadCode =
                    for {
                        (ctiPC, instruction, branchTargetPCs) ← body collectWithIndex {
                            case (ctiPC, i: ConditionalBranchInstruction) if operandsArray(ctiPC) != null ⇒
                                (ctiPC, i, i.nextInstructions(ctiPC, /*not required*/ null))
                        }
                        i = instrCount.incrementAndGet()
                        branchTarget ← branchTargetPCs.iterator
                        j = trgtsCount.incrementAndGet()
                        if operandsArray(branchTarget) == null
                        k = opsCount.incrementAndGet()
                    } yield {
                        resultsCount.incrementAndGet()
                        val operands = operandsArray(ctiPC).take(instruction.operandCount)
                        DeadCode(classFile, method, ctiPC, operands, branchTarget, None)
                    }
                for ((ln, dc) ← methodWithDeadCode.groupBy(_.ctiLineNumber)) {
                    ln match {
                        case None ⇒
                            if (dc.tail.isEmpty) {
                                // we have just one message, but since we have 
                                // no line number we are still "doubtful"
                                results.add(dc.head.copy(accuracy = Some(Percentage(75))))
                            } else {
                                dc.foreach(i ⇒ results.add(i.copy(accuracy = Some(Percentage(5)))))
                            }

                        case Some(ln) ⇒
                            if (dc.tail.isEmpty)
                                // we have just one message,...
                                results.add(dc.head.copy(accuracy = Some(Percentage(100))))
                            else
                                dc.foreach(i ⇒ results.add(i.copy(accuracy = Some(Percentage(10)))))
                    }
                }
            }
            Console.println(Console.RED+"METHODS: "+methodsCount.get+" INSTR: "+instrCount.get+" TRGTS: "+trgtsCount.get+" OPS: "+opsCount.get+"....RESULT SIZE====================="+results.size()+"...."+resultsCount.get)
            scala.collection.JavaConversions.collectionAsScalaIterable(results)
        } { t ⇒ analysisTime = t }

        (analysisTime, methodsWithDeadCode)
    }
}

object DeadCodeAnalysis {

    def resultsAsXHTML(results: (Long, Iterable[DeadCode])): Node = {
        val (analysisTime, methodsWithDeadCode) = results
        val methodWithDeadCodeCount = methodsWithDeadCode.size

        <div id="dead_code_results">
            <script type="text/javascript">
                {
                    new Unparsed(
                        """function updateAccuracy(value) {
                            document.querySelectorAll("tr[data-accuracy]").forEach(
                                function(tr){
                                    tr.dataset.accuracy < value ? tr.style.display="none" : tr.style.display="table-row"
                                }
                            )
                        }""")
                }
            </script>
            <div>Number of identified issues: { methodWithDeadCodeCount }</div>
            <div>
                Suppress identified issues with an estimated
                <span class="tooltip">importance<span>The importance is calculated using the available context information.<br/>E.g., a dead <i>default case</i> in a switch statement is often the result of defensive programming and, hence, not important.</span></span>
                less than:
                <span class="tooltip">1<span>The identified issue is probably not important or is just a technical artifact.</span></span>
                <input type="range" name="accuracy" id="accuracy" min="1" max="100" onchange="updateAccuracy(this.valueAsNumber)"/>
                <span class="tooltip">100<span>The identified issue is probably very important.</span></span>
            </div>
            <table>
                <tr>
                    <th>Class</th>
                    <th>Method</th>
                    <th class="pc">Program Counter /<br/>Line Number</th>
                    <th>Message</th>
                </tr>
                {
                    import scala.collection.SortedMap
                    val groupedMessages =
                        SortedMap.empty[String, Seq[DeadCode]] ++
                            methodsWithDeadCode.groupBy(dc ⇒ dc.classFile.thisType.packageName)
                    for { (pkg, mdc) ← groupedMessages } yield {
                        Seq(
                            <tr><td class="caption" colspan="4">{ pkg.replace('/', '.') }</td></tr>,
                            mdc.toSeq.sorted(DeadCodeOrdering).map(_.toXHTML)
                        )
                    }.flatten
                }
            </table>
            <script type="text/javascript">
                document.getElementById('accuracy').value=75;
                updateAccuracy(75);
            </script>
        </div>
    }
}

