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
import org.opalj.ai.debug.XHTML
import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.br.Code
import org.opalj.ai.collectWithOperandsAndIndex
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
import org.opalj.br.instructions.LStoreInstruction
import org.opalj.ai.analyses.FieldValuesKey
import org.opalj.ai.AIResult
import org.opalj.ai.domain.ConcreteIntegerValues
import org.opalj.ai.domain.ConcreteLongValues

/**
 * A static analysis that analyzes the data-flow to identify various issues in the
 * source code of projects.
 *
 * ==Precision==
 * The analysis is complete; i.e., every reported case is a true case. However, given
 * that we analyze Java bytecode, some findings may be the result of the compilation
 * scheme employed by the compiler and, hence, cannot be resolved at the
 * sourcecode level. This is in particular true for finally blocks in Java programs. In
 * this case compiler typically include the same block two times in the code.
 *
 */
class BugPickerAnalysis extends Analysis[URL, (Long, Iterable[Issue])] {

    override def title: String =
        "Dead/Useless/Buggy Code Identification"

    override def description: String =
        "Identifies dead/useless/buggy code by analyzing expressions."

    /**
     * Executes the analysis of the projects concrete methods.
     *
     * @param Either an empty sequence or a sequence that contains one or more of
     *      the following parameters:
     *      - a string that matches the following pattern: `-maxEvalFactor=(\d+(?:.\d+)?)`; e.g.,
     *      `-maxEvalFactor=0.5` or `-maxEvalFactor=1.5`. A value below 0.05 is usually
     *      not useable.
     *      - a string that matches the following pattern: `-maxEvalTime=(\d+)`.
     *      - a string that matches the following pattern: `-maxCardinalityOfIntegerRanges=(\d+)`.
     */
    override def analyze(
        theProject: Project[URL],
        parameters: Seq[String],
        initProgressManagement: (Int) ⇒ ProgressManagement): (Long, Iterable[Issue]) = {

        val maxEvalFactor: Double =
            parameters.collectFirst {
                case BugPickerAnalysis.maxEvalFactorPattern(d) ⇒
                    java.lang.Double.parseDouble(d).toDouble
            }.getOrElse(
                BugPickerAnalysis.defaultMaxEvalFactor
            )
        val maxEvalTime: Int =
            parameters.collectFirst {
                case BugPickerAnalysis.maxEvalTimePattern(l) ⇒
                    java.lang.Integer.parseInt(l).toInt
            }.getOrElse(
                BugPickerAnalysis.defaultMaxEvalTime
            )
        val maxCardinalityOfIntegerRanges: Int =
            parameters.collectFirst {
                case BugPickerAnalysis.maxCardinalityOfIntegerRangesPattern(i) ⇒
                    java.lang.Integer.parseInt(i).toInt
            }.getOrElse(
                BugPickerAnalysis.defaultMaxCardinalityOfIntegerRanges
            )

        if (parameters.contains("-debug")) {
            val cp = System.getProperty("java.class.path")
            val cpSorted = cp.split(java.io.File.pathSeparatorChar).sorted
            println("ClassPath:\n\t"+cpSorted.mkString("\n\t"))

            println("Settings:")
            println(s"\tmaxEvalFactor=$maxEvalFactor")
            println(s"\tmaxEvalTime=${maxEvalTime}ms")
            println(s"\tmaxCardinalityOfIntegerRanges=$maxCardinalityOfIntegerRanges")
        }

        // related to managing the analysis progress
        val classFilesCount = theProject.projectClassFilesCount
        val progressManagement =
            initProgressManagement(1 /*for the FieldValues analysis*/ + classFilesCount)

        //
        //
        // DO PREANALYSES
        //
        //
        progressManagement.start(1, "Analyzing field declarations")
        theProject.get(FieldValuesKey)
        progressManagement.end(1)

        //
        //
        // MAIN ANALYSIS
        //
        //

        val doInterrupt: () ⇒ Boolean = progressManagement.isInterrupted

        val results = new java.util.concurrent.ConcurrentLinkedQueue[Issue]()
        val fieldValueInformation = theProject.get(FieldValuesKey)

        def analyzeMethod(classFile: ClassFile, method: Method, body: Code) {
            val analysisDomain =
                new BugPickerAnalysisDomain(
                    theProject, fieldValueInformation,
                    method, maxCardinalityOfIntegerRanges)
            val ai =
                new BoundedInterruptableAI[analysisDomain.type](
                    body,
                    maxEvalFactor,
                    maxEvalTime,
                    doInterrupt)
            val result = ai(classFile, method, analysisDomain)

            if (!result.wasAborted) {

                //
                // FIND DEAD CODE
                //
                results.addAll(
                    scala.collection.JavaConversions.asJavaCollection(
                        DeadCodeAnalysis.analyze(theProject, classFile, method, result)
                    )
                )

                //
                // FIND USELESS COMPUTATIONS
                //
                results.addAll(
                    scala.collection.JavaConversions.asJavaCollection(
                        UselessComputationsAnalysis.analyze(theProject, classFile, method, result)
                    )
                )

                //
                // FIND USELESS EXPRESSION EVALUATIONS
                //

                import result.domain.ConcreteIntegerValue
                import result.domain.ConcreteLongValue
                import result.domain

                if (domain.code.localVariableTable.isDefined) {
                    // This analysis requires debug information to increase the likelihood
                    // the we identify the correct local variable re-assignments. Otherwise
                    // we are not able to distinguish the reuse of a "register variable"/
                    // local variable for a new/different purpose or the situation where
                    // the same variable is updated the second time using the same
                    // value.

                    val operandsArray = result.operandsArray
                    val localsArray = result.localsArray
                    val code = domain.code

                    val methodsWithValueReassignment =
                        collectWithOperandsAndIndex(domain)(body, operandsArray) {
                            case (
                                pc,
                                IStoreInstruction(index),
                                Seq(ConcreteIntegerValue(a), _*)
                                ) if localsArray(pc) != null &&
                                domain.intValueOption(localsArray(pc)(index)).map(_ == a).getOrElse(false) &&
                                code.localVariable(pc, index).map(lv ⇒ lv.startPC < pc).getOrElse(false) ⇒

                                val lv = code.localVariable(pc, index).get

                                StandardIssue(
                                    theProject, classFile, Some(method), Some(pc),
                                    Some(result.operandsArray(pc)),
                                    Some(result.localsArray(pc)),
                                    "useless (re-)assignment",
                                    Some("(Re-)Assigned the same value ("+a+") to the same variable ("+lv.name+")."),
                                    Set(IssueCategory.Flawed, IssueCategory.Comprehensibility),
                                    Set(IssueKind.ConstantComputation),
                                    Seq.empty,
                                    new Relevance(20)
                                )

                            case (
                                pc,
                                LStoreInstruction(index),
                                Seq(ConcreteLongValue(a), _*)
                                ) if localsArray(pc) != null &&
                                domain.longValueOption(localsArray(pc)(index)).map(_ == a).getOrElse(false) &&
                                code.localVariable(pc, index).map(lv ⇒ lv.startPC < pc).getOrElse(false) ⇒

                                val lv = code.localVariable(pc, index).get

                                StandardIssue(
                                    theProject, classFile, Some(method), Some(pc),
                                    Some(result.operandsArray(pc)),
                                    Some(result.localsArray(pc)),
                                    "useless (re-)assignment",
                                    Some("(Re-)Assigned the same value ("+a+") to the same variable ("+lv.name+")."),
                                    Set(IssueCategory.Flawed, IssueCategory.Comprehensibility),
                                    Set(IssueKind.ConstantComputation),
                                    Seq.empty,
                                    new Relevance(20)
                                )
                        }

                    results.addAll(
                        scala.collection.JavaConversions.asJavaCollection(methodsWithValueReassignment)
                    )
                }

            } else if (!doInterrupt()) {
                println(
                    s"[warn] analysis of ${method.fullyQualifiedSignature(classFile.thisType)} aborted "+
                        s"after ${ai.currentEvaluationCount} steps "+
                        s"(code size: ${method.body.get.instructions.length})")
            } /* else (doInterrupt === true) the analysis as such was interrupted*/
        }

        var analysisTime: Long = 0l
        val identifiedIssues = time {
            val stepIds = new java.util.concurrent.atomic.AtomicInteger(1 /*.. the FieldValuesAnalysis */ )

            for {
                classFile ← theProject.projectClassFiles.par
                if !progressManagement.isInterrupted()
            } {
                val stepId = stepIds.incrementAndGet()
                try {
                    progressManagement.start(stepId, classFile.thisType.toJava)
                    for (method @ MethodWithBody(body) ← classFile.methods) {
                        try {
                            analyzeMethod(classFile, method, body)
                        } catch {
                            case afe: InterpretationFailedException ⇒
                                val ms = method.fullyQualifiedSignature(classFile.thisType)
                                val steps = afe.ai.asInstanceOf[BoundedInterruptableAI[_]].currentEvaluationCount
                                val cause = afe.cause
                                println(
                                    s"[error] the analysis of ${ms} failed after $steps steps: "+cause
                                )
                                afe.printStackTrace()
                        }
                    }
                } finally {
                    progressManagement.end(stepId)
                }
            }
            scala.collection.JavaConversions.collectionAsScalaIterable(results)
        } { t ⇒ analysisTime = t }

        (analysisTime, identifiedIssues)
    }
}

object BugPickerAnalysis {

    // we want to match expressions such as:
    // -maxEvalFactor=1
    // -maxEvalFactor=20
    // -maxEvalFactor=1.25
    // -maxEvalFactor=10.5
    final val maxEvalFactorPattern = """-maxEvalFactor=(\d+(?:.\d+)?)""".r
    final val defaultMaxEvalFactor = 1.75d

    final val maxEvalTimePattern = """-maxEvalTime=(\d+)""".r
    final val defaultMaxEvalTime = 10000

    final val maxCardinalityOfIntegerRangesPattern =
        """-maxCardinalityOfIntegerRanges=(\d+)""".r
    final val defaultMaxCardinalityOfIntegerRanges = 16

    lazy val reportCSS: String =
        process(this.getClass.getResourceAsStream("report.css"))(
            scala.io.Source.fromInputStream(_).mkString
        )

    lazy val reportJS: String =
        process(this.getClass.getResourceAsStream("report.js"))(
            scala.io.Source.fromInputStream(_).mkString
        )

    def resultsAsXHTML(results: (Long, Iterable[Issue])): Node = {
        val (analysisTime, methodsWithDeadCode) = results
        val methodWithDeadCodeCount = methodsWithDeadCode.size

        val issuesNode: Iterable[Node] = {
            import scala.collection.SortedMap
            val groupedMessages =
                SortedMap.empty[String, Seq[Issue]] ++
                    methodsWithDeadCode.groupBy(dc ⇒ dc.classFile.thisType.packageName)
            val result =
                (for { (pkg, mdc) ← groupedMessages } yield {
                    <details class="package_summary">
                        <summary class="package_summary">{ pkg.replace('/', '.') }</summary>
                        { mdc.toSeq.sorted(IssueOrdering).map(_.asXHTML) }
                    </details>
                })
            result
        }

        <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta http-equiv='Content-Type' content='application/xhtml+xml; charset=utf-8'/>
                <script type="text/javascript">{ Unparsed(bugpicker.htmlJS) }</script>
                <script type="text/javascript">{ Unparsed(reportJS) }</script>
                <style>{ Unparsed(bugpicker.htmlCSS) }</style>
                <style>{ Unparsed(reportCSS) }</style>
            </head>
            <body>
                <div id="analysis_controls">
                    <div>
                        <span>Number of issues: { methodWithDeadCodeCount }.</span>
                    </div>
                    <div>
                        Suppress issues with an estimated
                        <abbr title='The importance is calculated using the available context information. E.g., a dead "default case" in a switch statement is often the result of defensive programming and, hence, not important.'>importance</abbr>
                        less than:
                        <abbr title="The identified issue is probably not important or is just a technical artifact.">1</abbr>
                        <input type="range" name="relevance" id="relevance" min="1" max="100" onchange="updateRelevance(this.valueAsNumber)"/>
                        <abbr title="The identified issue is probably very important.">100</abbr>
                    </div>
                    <div>
                        Show all Packages:<a class="onclick" onclick="openAllPackages()">+</a><a class="onclick" onclick="closeAllPackages()">-</a>
                    </div>
                </div>
                <div id="analysis_results">
                    { issuesNode }
                </div>
                <script type="text/javascript">
                    document.getElementById('relevance').value=75;
                    updateRelevance(75);
                    openAllPackages();
                </script>
            </body>
        </html>
    }
}

