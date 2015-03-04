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

import java.net.URL
import scala.collection.SortedMap
import scala.xml.Node
import org.opalj.util.PerformanceEvaluation.ns2sec
import org.opalj.io.writeAndOpen
import org.opalj.io.process
import org.opalj.br.analyses.{ Analysis, AnalysisExecutor, BasicReport, Project }
import org.opalj.br.analyses.ProgressManagement
import org.opalj.ai.common.XHTML
import org.opalj.bugpicker.core.analysis.BugPickerAnalysis
import org.opalj.bugpicker.core.analysis.IssueKind
import org.opalj.log.OPALLogger

/**
 * A data-flow analysis that tries to identify dead code based on the evaluation
 * of branches following if instructions that are not followed.
 *
 * @author Michael Eichberg
 */
object Console extends AnalysisExecutor { analysis ⇒

    val HTMLFileOutputNameMatcher = """-html=([\w\.\:/\\]+)""".r
    val DebugFileOutputNameMatcher = """-debug=([\w\.\:/\\]+)""".r

    private final val bugPickerAnalysis = new BugPickerAnalysis

    val analysis = new Analysis[URL, BasicReport] {

        override def title: String = bugPickerAnalysis.title

        override def description: String = bugPickerAnalysis.description

        override def analyze(
            theProject: Project[URL],
            parameters: Seq[String],
            initProgressManagement: (Int) ⇒ ProgressManagement) = {

            val (analysisTime, issues0, exceptions) =
                bugPickerAnalysis.analyze(theProject, parameters, initProgressManagement)

            //
            // PREPARE THE GENERATION OF THE REPORT OF THE FOUND ISSUES
            // (HTML/Eclipse)
            //

            // Filter the report
            val minRelevance: Int =
                parameters.collectFirst {
                    case minRelevancePattern(i) ⇒ java.lang.Integer.parseInt(i)
                }.getOrElse(
                    Console.minRelevance
                )
            val issues1 = issues0.filter { i ⇒ i.relevance.value >= minRelevance }

            val issues = parameters.collectFirst { case issueKindsPattern(ks) ⇒ ks } match {
                case Some(ks) ⇒
                    val relevantKinds = ks.split(',').toSet
                    issues1.filter(issue ⇒ (issue.kind intersect (relevantKinds)).nonEmpty)
                case None ⇒
                    issues1
            }

            // Generate the report well suited for the eclipse console
            //
            if (parameters.contains("-eclipse")) {
                val formattedIssues = issues.map { issue ⇒ issue.asEclipseConsoleString }
                println(formattedIssues.toSeq.sorted.mkString("\n"))
            }

            // Generate the HTML report
            //
            var htmlReport: String = null
            def getHTMLReport = {
                if (htmlReport eq null)
                    htmlReport = BugPickerAnalysis.resultsAsXHTML(issues).toString
                htmlReport
            }
            parameters.collectFirst { case HTMLFileOutputNameMatcher(name) ⇒ name } match {
                case Some(fileName) ⇒
                    process { new java.io.FileOutputStream(fileName) } { fos ⇒
                        fos.write(getHTMLReport.getBytes("UTF-8"))
                    }
                case _ ⇒ // Nothing to do
            }
            if (parameters.contains("-html")) {
                writeAndOpen(getHTMLReport, "BugPickerAnalysisResults", ".html")
            }

            //
            // PREPARE THE GENERATION OF THE REPORT OF THE OCCURED EXCEPTIONS
            //
            if (exceptions.nonEmpty) {
                OPALLogger.error(
                    "internal error",
                    s"the analysis threw ${exceptions.size} exceptions")(
                        theProject.logContext)
                exceptions.foreach { e ⇒
                    OPALLogger.error(
                        "internal error", "the analysis failed", e)(theProject.logContext)
                }

                var exceptionsReport: Node = null
                def getExceptionsReport = {
                    if (exceptionsReport eq null) {
                        val exceptionNodes =
                            exceptions.take(10).map { e ⇒
                                <p>{ XHTML.throwableToXHTML(e) }</p>
                            }
                        exceptionsReport =
                            XHTML.createXHTML(
                                Some(s"${exceptions.size}/${exceptionNodes.size} Thrown Exceptions"),
                                <div>{ exceptionNodes }</div>
                            )
                    }
                    exceptionsReport
                }
                parameters.collectFirst { case DebugFileOutputNameMatcher(name) ⇒ name } match {
                    case Some(fileName) ⇒
                        process { new java.io.FileOutputStream(fileName) } { fos ⇒
                            fos.write(getExceptionsReport.toString.getBytes("UTF-8"))
                        }
                    case _ ⇒ // Nothing to do
                }
                if (parameters.contains("-debug")) {
                    org.opalj.io.writeAndOpen(getExceptionsReport, "Exceptions", ".html")
                }
            }

            //
            // Print some statistics and "return"
            //
            val groupedIssues =
                issues.groupBy(_.relevance).toList.
                    sortWith((e1, e2) ⇒ e1._1.value < e2._1.value)
            val groupedAndCountedIssues = groupedIssues.map(e ⇒ e._1+": "+e._2.size)

            BasicReport(
                groupedAndCountedIssues.mkString(
                    s"Issues (∑${issues.size}):\n\t",
                    "\n\t",
                    f"\nIdentified in: ${ns2sec(analysisTime)}%2.2f seconds.\n")
            )
        }
    }

    final val minRelevancePattern = """-minRelevance=(\d\d?)""".r
    final val minRelevance = 0

    final val issueKindsPattern = """-kinds=([\w, ]+)""".r

    final override val analysisSpecificParametersDescription: String =
        """[-maxEvalFactor=<DoubleValue {[0.1,100),Infinity}=1.75> determines the maximum effort that
            |               the analysis will spend when analyzing a specific method. The effort is
            |               always relative to the size of the method. For the vast majority of methods
            |               a value between 0.5 and 1.5 is sufficient to completely analyze a single
            |               method using the default settings.
            |               A value greater than 1.5 can already lead to very long evaluation times.
            |               If the threshold is exceeded the analysis of the method is aborted and no
            |               result can be drawn.]
            |[-maxEvalTime=<IntValue [10,1000000]=10000> determines the time (in ms) that the analysis is allowed
            |               to take for one method before the analysis is terminated.]
            |[-maxCardinalityOfIntegerRanges=<IntValue [1,1024]=16> basically determines for each integer
            |               value how long the value is "precisely" tracked. Internally the analysis
            |               computes the range of values that an integer value may have at runtime. The
            |               maximum size/cardinality of this range is controlled by this setting. If
            |               the range is exceeded the precise tracking of the respective value is
            |               terminated.
            |               Increasing this value may significantly increase the analysis time and
            |               may require the increase of -maxEvalFactor.]
            |[-maxCallChainLength=<IntValue [0..9]=0> determines the maximum length of the call chain
            |               that is analyzed.
            |               If you increase this value by one it is typically also necessary
            |               to also increase the maxEvalFactor by a factor of 2 to 3. Otherwise it
            |               may happen that many analyses are aborted because the evaluation time
            |               is exhausted and – overall – the analysis reports less issues!]
            |[-minRelevance=<IntValue [0..99]=0> the minimum relevance of the shown issues.
            |[-kinds=<Issue Kinds="constant computation,dead path,throws exception,
            |                unguarded use,unused">] a comma seperated list of issue kinds
            |                that should be reported
            |[-eclipse      creates an eclipse console compatible output).]
            |[-html[=<FileName>] generates an HTML report which is written to the optionally
            |               specified location.]
            |[-debug[=<FileName>] turns on the debug mode (more information are logged and
            |               internal, recoverable exceptions are logged) the report is optionally
            |               written to the specified location.]""".stripMargin('|')

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean = {
        var outputFormatGiven = false

        parameters.forall(parameter ⇒
            parameter match {
                case BugPickerAnalysis.maxEvalFactorPattern(d) ⇒
                    try {
                        val factor = java.lang.Double.parseDouble(d).toDouble
                        (factor >= 0.1d && factor < 100.0d) ||
                            factor == Double.PositiveInfinity
                    } catch {
                        case nfe: NumberFormatException ⇒ false
                    }
                case BugPickerAnalysis.maxEvalTimePattern(l) ⇒
                    try {
                        val maxTime = java.lang.Long.parseLong(l).toLong
                        maxTime >= 10 && maxTime <= 1000000
                    } catch {
                        case nfe: NumberFormatException ⇒ false
                    }
                case BugPickerAnalysis.maxCardinalityOfIntegerRangesPattern(i) ⇒
                    try {
                        val cardinality = java.lang.Integer.parseInt(i).toInt
                        cardinality >= 1 && cardinality <= 1024
                    } catch {
                        case nfe: NumberFormatException ⇒ false
                    }
                case BugPickerAnalysis.maxCallChainLengthPattern(_) ⇒
                    // the pattern ensures that the value is legal...
                    true

                case issueKindsPattern(ks) ⇒
                    val kinds = ks.split(',')
                    kinds.length > 0 &&
                        kinds.forall { k ⇒ IssueKind.AllKinds.contains(k) }

                case minRelevancePattern(_) ⇒
                    // the pattern ensures that the value is legal...
                    true

                case HTMLFileOutputNameMatcher(_) ⇒
                    outputFormatGiven = true; true
                case "-html" ⇒
                    outputFormatGiven = true; true
                case "-eclipse" ⇒
                    outputFormatGiven = true; true
                case "-debug"                      ⇒ true
                case DebugFileOutputNameMatcher(_) ⇒ true
                case _                             ⇒ false
            }
        ) &&
            outputFormatGiven
    }

}

