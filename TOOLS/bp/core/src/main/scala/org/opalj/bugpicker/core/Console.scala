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

import org.opalj.util.PerformanceEvaluation.ns2sec
import org.opalj.io.writeAndOpen
import org.opalj.io.process
import org.opalj.br.analyses.{ Analysis, AnalysisExecutor, BasicReport, Project }
import org.opalj.br.analyses.ProgressManagement
import org.opalj.ai.common.XHTML
import org.opalj.bugpicker.core.analysis.BugPickerAnalysis

/**
 * A data-flow analysis that tries to identify dead code based on the evaluation
 * of branches following if instructions that are not followed.
 *
 * @author Michael Eichberg
 */
object Console extends AnalysisExecutor { analysis ⇒

    val FileOutputNameMatcher = """-o=([\w\.\:/\\]+)""".r

    private final val bugPickerAnalysis = new BugPickerAnalysis

    val analysis = new Analysis[URL, BasicReport] {

        override def title: String = bugPickerAnalysis.title

        override def description: String = bugPickerAnalysis.description

        override def analyze(
            theProject: Project[URL],
            parameters: Seq[String],
            initProgressManagement: (Int) ⇒ ProgressManagement) = {

            val (analysisTime, issues, exceptions) =
                bugPickerAnalysis.analyze(theProject, parameters, initProgressManagement)

            val doc = BugPickerAnalysis.resultsAsXHTML(issues).toString

            parameters.collectFirst { case FileOutputNameMatcher(name) ⇒ name } match {

                case Some(fileName) ⇒
                    process { new java.io.FileOutputStream(fileName) } { fos ⇒
                        fos.write(doc.getBytes("UTF-8"))
                    }

                case None ⇒
                    writeAndOpen(doc, "BugPickerAnalysisResults", ".html")
            }

            if (parameters.contains("-debug") && exceptions.nonEmpty) {
                val exceptionNodes = exceptions.map(e ⇒ <p>{ XHTML.throwableToXHTML(e) }</p>)
                val exceptionsDoc =
                    XHTML.createXHTML(
                        Some("Thrown Exceptions"),
                        <div>{ exceptionNodes }</div>)
                org.opalj.io.writeAndOpen(exceptionsDoc, "Exceptions", ".html")
            }

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

    final override val analysisSpecificParametersDescription: String =
        """[-maxEvalFactor=<DoubleValue [0.1,15.0]=1.75> determines the maximum effort that the analysis
            |               will spend when analyzing a specific method. The effort is always relative
            |               to the size of the method. For the vast majority of methods a value
            |               between 0.5 and 1.5 is sufficient to completely analyze the method using
            |               the default settings.
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
            |[-o=<FileName> determines the name of the output file (if an output file is specified
            |               no browser will be opened.]
            |[-debug        turns on the debug mode.]""".stripMargin('|')

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean =
        parameters.forall(parameter ⇒
            parameter match {
                case BugPickerAnalysis.maxEvalFactorPattern(d) ⇒
                    try {
                        val factor = java.lang.Double.parseDouble(d).toDouble
                        factor >= 0.1d && factor <= 15.0d
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
                case BugPickerAnalysis.maxCallChainLengthPattern(i) ⇒
                    try {
                        // the pattern ensures that the value is legal...
                        true
                    } catch {
                        case nfe: NumberFormatException ⇒ false
                    }
                case FileOutputNameMatcher(_) ⇒ true
                case "-debug"                 ⇒ true
                case _                        ⇒ false
            })

}

