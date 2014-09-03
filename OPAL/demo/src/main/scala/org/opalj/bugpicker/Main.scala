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
import org.opalj.br.analyses.{ Analysis, AnalysisExecutor, BasicReport, Project }
import org.opalj.ai.debug.XHTML

/**
 * A shallow analysis that tries to identify dead code based on the evaluation
 * of branches following if instructions that are not followed.
 *
 * @author Michael Eichberg
 */
object Main extends AnalysisExecutor { analysis ⇒

    private final val deadCodeAnalysis = new DeadCodeAnalysis
    val analysis = new Analysis[URL, BasicReport] {

        override def title: String = deadCodeAnalysis.title

        override def description: String = deadCodeAnalysis.description

        override def analyze(theProject: Project[URL], parameters: Seq[String]) = {
            val results @ (analysisTime, methodsWithDeadCode) =
                deadCodeAnalysis.analyze(theProject, parameters)

            val doc = XHTML.createXHTML(Some(title), DeadCodeAnalysis.resultsAsXHTML(results))
            XHTML.writeAndOpenDump(doc)

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
}

