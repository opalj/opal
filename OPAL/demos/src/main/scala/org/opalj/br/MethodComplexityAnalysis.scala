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
package br

import java.net.URL

import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project

/**
 * Very primitive rating of the complexity of methods.
 *
 * @author Michael Eichberg
 */
object MethodComplexityAnalysis
        extends OneStepAnalysis[URL, BasicReport]
        with AnalysisExecutor {

    val analysis = this

    override def description: String = "Estimates the complexity of interpreting the method."

    def doAnalyze(
        project: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean) = {

        import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }
        var executionTimeInSecs = 0d

        val analysisResults = time {

            import org.opalj.br.analyses.{ MethodComplexityAnalysis ⇒ TheAnalysis }
            TheAnalysis.doAnalyze(project, 100, isInterrupted)

        } { executionTime ⇒ executionTimeInSecs = ns2sec(executionTime) }

        BasicReport(
            analysisResults.
                toList.map(m ⇒ (m._2, m._1)).
                sorted.map(m ⇒ m._1+":"+m._2.fullyQualifiedSignature(project.classFile(m._2).thisType)).
                mkString("\n")+"\n"+
                s"Rated ${analysisResults.size} methods in ${executionTimeInSecs} secs."
        )
    }
}
