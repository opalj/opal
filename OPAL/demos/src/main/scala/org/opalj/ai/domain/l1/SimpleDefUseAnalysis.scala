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
import scala.language.existentials
import java.util.concurrent.ConcurrentLinkedQueue
import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.graphs.DefaultMutableNode
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Simple analysis that takes the "unused"-Node from the def-use graph
 * and returns all its children,
 * i.e. definitions and assignments that are not used again locally.
 *
 * @author Stephan Neumann
 */
object SimpleDefUseAnalysis extends DefaultOneStepAnalysis {

    override def title: String =
        "Identifies unused variables and unnecessary calculations"

    override def description: String =
        "Identifies variable declarations or assignments that are not used again locally"

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {

        var analysisTime: Seconds = Seconds.None
        val UnusedDefUseNodes = time {

            val results = new ConcurrentLinkedQueue[String]
            val ai = new InterruptableAI[Domain]

            theProject.parForeachMethodWithBody() { m ⇒
                val (_, classFile, method) = m

                val domain = new DefaultDomainWithCFGAndDefUse(theProject, classFile, method)
                val result = ai(classFile, method, domain)

                val defUseGraph =
                    result.domain.createDefUseGraph(result.domain.code)

                def isUnusedNode(n: DefaultMutableNode[ValueOrigin]): Boolean =
                    n.identifier == Int.MinValue

                val unusedNode = defUseGraph.find(isUnusedNode).get

                if (!unusedNode.children.isEmpty)
                    results.add( //TODO Think about nicer output formatting
                        method.toJava(classFile)+","+
                            unusedNode.children.map(e ⇒ e.toHRR).mkString+"\n"
                    )
            }
            results

        } { t ⇒ analysisTime = t.toSeconds }

        BasicReport(
            UnusedDefUseNodes.toString()+"\nThe analysis took "+analysisTime
        )
    }

}