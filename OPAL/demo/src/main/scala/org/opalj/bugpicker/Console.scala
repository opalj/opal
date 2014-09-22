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

import scala.collection.SortedMap

import org.opalj.util.PerformanceEvaluation.ns2sec
import org.opalj.util.writeAndOpen
import org.opalj.br.analyses.{ Analysis, AnalysisExecutor, BasicReport, Project }
import org.opalj.br.analyses.ProgressManagement
import org.opalj.ai.debug.XHTML

/**
 * A shallow analysis that tries to identify dead code based on the evaluation
 * of branches following if instructions that are not followed.
 *
 * @author Michael Eichberg
 */
object Console extends AnalysisExecutor { analysis ⇒

    private final val deadCodeAnalysis = new DeadCodeAnalysis
    val analysis = new Analysis[URL, BasicReport] {

        override def title: String = deadCodeAnalysis.title

        override def description: String = deadCodeAnalysis.description

        override def analyze(
            theProject: Project[URL],
            parameters: Seq[String],
            initProgressManagement: (Int) ⇒ ProgressManagement) = {
            val results @ (analysisTime, methodsWithDeadCode) =
                deadCodeAnalysis.analyze(theProject, parameters, initProgressManagement)

            val doc = XHTML.createXHTML(Some(title), DeadCodeAnalysis.resultsAsXHTML(results))
            writeAndOpen(doc, "DeadCodeAnalysisResults", ".html")

            //            BasicReport(
            //                methodsWithDeadCode.toList.sortWith((l, r) ⇒
            //                    l.classFile.thisType < r.classFile.thisType ||
            //                        (l.classFile.thisType == r.classFile.thisType && (
            //                            l.method < r.method || (
            //                                l.method == r.method &&
            //                                l.ctiPC < r.ctiPC
            //                            )
            //                        ))
            //                ).mkString(
            //                    "Dead code (number of dead branches: "+methodsWithDeadCode.size+"): \n",
            //                    "\n",
            //                    f"%nIdentified in: ${ns2sec(analysisTime)}%2.2f seconds."))

            BasicReport(
                "Dead code (number of dead branches: "+methodsWithDeadCode.size+") "+
                    f"identified in: ${ns2sec(analysisTime)}%2.2f seconds."
            )
        }
    }

    final override val analysisSpecificParametersDescription: String =
        """[-maxEvalFactor=<DoubleValue [0.1,15.0]=1.75> determines the maximum effort that the analysis 
            |               will spend when analyzing a specific method. The effort is always relative 
            |               to the size of the method. For the vast majority of methods a value 
            |               between 0.5 and 1.5 is sufficient to completely analyze the method.
            |               A value greater than 1.5 can already lead to very long evaluation times.
            |               If the threshold is exceeded the analysis is aborted and no result can be drawn.]""".stripMargin('|')

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean =
        parameters.length == 0 ||
            (parameters.length == 1 &&
                (parameters(0) match {
                    case DeadCodeAnalysis.maxEvalFactorPattern(d) ⇒
                        try {
                            val factor = java.lang.Double.parseDouble(d).toDouble
                            factor >= 0.1d && factor <= 15.0d
                        } catch {
                            case nfe: NumberFormatException ⇒ false
                        }
                    case _ ⇒ false
                })
            )

}

