/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package l0

import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._

import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.br.instructions.ReturnInstruction
import org.opalj.br.analyses.BasicMethodInfo

/**
 * Simple analysis that identifies those parameters that are directly returned by the
 * given method. Note that some (non-)usages are never a problem.
 *
 * @author Michael Eichberg
 */
object ParameterUsageAnalysis extends DefaultOneStepAnalysis {

    override def title: String = "Identifies unused parameters"

    override def description: String = {
        "Identifies parameters that are not used by the current method (they may, however,"+
            " be used by methods which override the current one)"
    }

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        var analysisTime: Seconds = Seconds.None
        val returnedParameters = time {

            val results = new ConcurrentLinkedQueue[String]
            val ai = new InterruptableAI[Domain]

            theProject.parForeachMethodWithBody() { m ⇒
                val BasicMethodInfo(classFile, method) = m
                val parametersCount = method.parametersCount
                if (parametersCount > 0) {
                    val isStatic = method.isStatic
                    val descriptor = method.descriptor
                    val domain = new BaseDomainWithDefUse(theProject, classFile, method)
                    val result = ai(classFile, method, domain)
                    val instructions = result.domain.code.instructions

                    var parameterIndex = parametersCount - 1
                    while (parameterIndex >= 0) {
                        val vo = parameterIndexToValueOrigin(isStatic, descriptor, parameterIndex)
                        val usedBy = result.domain.usedBy(vo)
                        usedBy.foreach { usage ⇒
                            instructions(usage) match {
                                case r: ReturnInstruction ⇒
                                    val methodSignature = method.toJava(classFile)
                                    results.add(
                                        s"$methodSignature: the $parameterIndex. parameter is returned by instruction $usage"
                                    )
                                case _ ⇒ // we don't care about other usages..
                            }
                        }
                        parameterIndex -= 1
                    }
                }
            }
            results.asScala.toList.sorted

        } { t ⇒ analysisTime = t.toSeconds }

        val occurences = returnedParameters.size
        BasicReport(
            returnedParameters.mkString(
                "Methods which directly return a parameter:\n",
                "\n",
                s"\nThe analysis took $analysisTime and found $occurences direct returns"
            )

        )
    }

}

