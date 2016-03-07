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
package ai
package domain
package l1

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Sometimes calculations are done, but the result is only used
 * in some cases after a following branch and discarded otherwise.
 * Using a Data-Dependence Graph and Control-Flow Graph, cases like this
 * can be identified
 *
 * @author Stephan Neumann
 */

object PostDominatorSpeedTest extends DefaultOneStepAnalysis {

    override def title: String =
        "Identifies early calculations"

    override def description: String =
        "Identifies calculations that could be done after a branch so the dont have to be run in some cases"

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {

        var analysisTime: Seconds = new Seconds(0d)
        time {

            theProject.parForeachMethodWithBody() { m ⇒
                val (_, classFile, method) = m
                if (!method.isSynthetic) {
                    val domain = new DefaultDomainWithCFGAndDefUse(theProject, classFile, method)
                    
                    val dt = domain.dominatorTree
                    if(dt eq null) println("DomTree is null")
                    
                    val pdt = domain.postDominatorTree
                    if(pdt eq null) println("PostDomTree is null")
                    
                }
            }
        } { t ⇒ analysisTime = t.toSeconds }

        BasicReport(
          "The analysis took "+analysisTime
        )
    }

}