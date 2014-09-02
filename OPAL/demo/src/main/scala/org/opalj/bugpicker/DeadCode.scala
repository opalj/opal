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

/**
 * A shallow analysis that tries to identify dead code based on the evaluation
 * of branches following if instructions that are not followed.
 *
 * @author Michael Eichberg
 */
object DeadCode extends AnalysisExecutor { analysis ⇒

    private final val deadCodeAnalysis = new DeadCodeAnalysis
    val analysis = new Analysis[URL, BasicReport] {

        override def title: String = deadCodeAnalysis.title

        override def description: String = deadCodeAnalysis.description

        override def analyze(theProject: Project[URL], parameters: Seq[String]) = {
            val results @ (analysisTime, methodsWithDeadCode) =
                deadCodeAnalysis.analyze(theProject, parameters)

            val doc = XHTML.createXHTML(Some(title), DeadCodeAnalysis.resultsAsXHTML(results))
            XHTML.writeAndOpenDump(doc)

            BasicReport(
                methodsWithDeadCode.toList.sortWith((l, r) ⇒
                    l.classFile.thisType < r.classFile.thisType ||
                        (l.classFile.thisType == r.classFile.thisType && (
                            l.method < r.method || (
                                l.method == r.method &&
                                l.ctiPC < r.ctiPC
                            )
                        ))
                ).mkString(
                    "Dead code (number of dead branches: "+methodsWithDeadCode.size+"): \n",
                    "\n",
                    f"%nIdentified in: ${ns2sec(analysisTime)}%2.2f seconds."))

        }
    }
}

class DeadCodeAnalysisDomain(
    override val project: Project[java.net.URL],
    override val method: Method,
    override val maxCardinalityOfIntegerRanges: Long = 16l)
        extends Domain
        with domain.DefaultDomainValueBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l1.DefaultReferenceValuesBinding
        with domain.l1.DefaultIntegerRangeValues
        with domain.l1.ConstraintsBetweenIntegerValues
        // with domain.l1.DefaultIntegerSetValues
        with domain.l1.DefaultLongValues
        with domain.l1.LongValuesShiftOperators
        with domain.l1.DefaultConcretePrimitiveValuesConversions
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.TheProject[java.net.URL]
        with domain.TheMethod
        with domain.ProjectBasedClassHierarchy

class DeadCodeAnalysis extends Analysis[URL, (Long, Iterable[DeadCode])] {

    override def title: String = "Dead/Useless/Buggy Code Identification"

    override def description: String = "Identifies dead/useless/buggy code using abstract interpretation."

    override def analyze(
        theProject: Project[URL],
        parameters: Seq[String]): (Long, Iterable[DeadCode]) = {

        val cpus = Runtime.getRuntime().availableProcessors()
        var analysisTime: Long = 0l
        val methodsWithDeadCode = time {
            val results = new java.util.concurrent.ConcurrentLinkedQueue[DeadCode]()

            for {
                classFiles ← theProject.groupedClassFilesWithCode(cpus).par
                classFile ← classFiles
                method @ MethodWithBody(body) ← classFile.methods
            } {
                val domain = new DeadCodeAnalysisDomain(theProject, method)
                val result = BaseAI(classFile, method, domain)
                val operandsArray = result.operandsArray
                val methodWithDeadCode =
                    for {
                        (ctiPC, instruction, branchTargetPCs) ← body collectWithIndex {
                            case (ctiPC, i: ConditionalBranchInstruction) if operandsArray(ctiPC) != null ⇒
                                (ctiPC, i, i.nextInstructions(ctiPC, /*not required*/ null))
                        }
                        branchTarget ← branchTargetPCs.iterator
                        if operandsArray(branchTarget) == null
                    } yield {
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

case class DeadCode(
        classFile: ClassFile,
        method: Method,
        ctiPC: PC,
        operands: List[_],
        deadPC: PC,
        accuracy: Option[Percentage]) {

    def ctiInstruction = method.body.get.instructions(ctiPC)

    def ctiLineNumber: Option[PC] =
        method.body.get.lineNumber(ctiPC)

    def deadLineNumber: Option[PC] =
        method.body.get.lineNumber(deadPC)

    def message: String = {
        ctiInstruction match {
            case i: SimpleConditionalBranchInstruction ⇒
                val conditionIsAlwaysTrue =
                    method.body.get.pcOfNextInstruction(ctiPC) == deadPC
                "the condition is always "+conditionIsAlwaysTrue
            case i: CompoundConditionalBranchInstruction ⇒
                val (caseValues, defaultCase) = i.caseValueOfJumpOffset(deadPC - ctiPC)
                var message = ""
                if (caseValues.nonEmpty)
                    message += "dead case "+caseValues.mkString(" or ")
                if (defaultCase) {
                    if (message.nonEmpty)
                        message += "; "
                    message += "default case is dead"
                }

                message
        }
    }

    def toXHTML: Node = {

        val iNode =
            ctiInstruction match {
                case cbi: SimpleConditionalBranchInstruction ⇒

                    val condition =
                        if (cbi.operandCount == 1)
                            List(
                                <span class="value">{ operands.head }</span>,
                                <span class="operator">{ cbi.operator }</span>
                            )
                        else
                            List(
                                <span class="value">{ operands.tail.head }</span>,
                                <span class="operator">{ cbi.operator }</span>,
                                <span class="value">{ operands.head }</span>
                            )
                    <span class="keyword">if</span> :: condition

                case cbi: CompoundConditionalBranchInstruction ⇒
                    Seq(
                        <span class="keyword">switch</span>,
                        <span class="value">{ operands.head }</span>
                        <span> (case values: { cbi.caseValues.mkString(", ") } )</span>
                    )
            }

        val pcNode =
            <span class="tooltip">
                { ctiPC }
                <span>{ iNode }</span>
            </span>

        val node =
            <tr style={
                val color = accuracy.map(a ⇒ a.asHTMLColor).getOrElse("rgb(255, 126, 3)")
                s"color:$color;"
            }>
                <td>
                    { XHTML.typeToXHTML(classFile.thisType) }
                </td>
                <td>{ XHTML.methodToXHTML(method.name, method.descriptor) }</td>
                <td>{ pcNode }{ "/ "+ctiLineNumber.getOrElse("N/A") }</td>
                <td>{ message }</td>
            </tr>

        accuracy match {
            case Some(a) ⇒
                node % (
                    new UnprefixedAttribute("data-accuracy", a.value.toString(), scala.xml.Null)
                )
            case None ⇒
                node
        }

    }

    override def toString = {
        import Console._
        val declaringClassOfMethod = classFile.thisType.toJava

        "Dead code in "+BOLD + BLUE +
            declaringClassOfMethod+"{ "+method.toJava+"{ "+
            GREEN+"PC: "+ctiPC + ctiLineNumber.map("; Line: "+_+" - ").getOrElse("; Line: N/A - ") +
            ctiInstruction + operands.reverse.mkString("(", ",", ")") +
            RESET+" }}"
    }
}

object DeadCodeOrdering extends scala.math.Ordering[DeadCode] {

    def compare(x: DeadCode, y: DeadCode): Int = {
        if (x.classFile.fqn < y.classFile.fqn) {
            -1
        } else if (x.classFile.fqn == y.classFile.fqn) {
            val methodComparison = x.method.compare(y.method)
            if (methodComparison == 0) {
                if (x.ctiLineNumber.isDefined)
                    x.ctiLineNumber.get - y.ctiLineNumber.get
                else
                    x.ctiPC - y.ctiPC
            } else {
                methodComparison
            }
        } else {
            1
        }
    }

}

case class Percentage(val value: Int) extends AnyVal {

    /**
     * The lower the percentage, the "whiter" the color. If the percentage is 100%
     * then the color will be black.
     */
    def asHTMLColor = {
        val rgbValue = 0 + (100 - value) * 2
        s"rgb($rgbValue,$rgbValue,$rgbValue)"
    }
}
