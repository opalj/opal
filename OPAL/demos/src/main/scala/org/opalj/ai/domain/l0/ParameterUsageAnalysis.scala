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

/**
 * Simple analysis that identifies unused and passed through parameters; i.e., those that are
 * directly returned by the given method.
 *
 * @author Michael Eichberg
 */
object ParameterUsageAnalysis extends DefaultOneStepAnalysis {

    override def title: String = "Identifies methods which return a given parameter"

    override def description: String = {
        "Identifies parameters that are - at least on some paths - directly returned"
    }

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        var analysisTime: Seconds = Seconds.None
        val (returnedParameters, unusedParameters) = time {

            val unusedParameters = new ConcurrentLinkedQueue[String]
            val returnedParameters = new ConcurrentLinkedQueue[String]
            val ai = new InterruptableAI[Domain]

            val exceptions = theProject.parForeachMethodWithBody() { m ⇒
                val method = m.method
                val psCount = method.actualArgumentsCount // includes "this" in case of instance methods
                if (psCount > 0) {
                    val isStatic = method.isStatic
                    val descriptor = method.descriptor
                    val domain = new BaseDomainWithDefUse(theProject, method)
                    val result = ai(method, domain)
                    val instructions = result.domain.code.instructions
                    val methodSignature = method.toJava
                    def validateArgument(valueOrigin: ValueOrigin): Unit = {
                        val usedBy = result.domain.usedBy(valueOrigin)
                        if (usedBy eq null) {
                            val use = s" the value with origin $valueOrigin is not used"
                            unusedParameters.add(methodSignature + use)
                        } else {
                            usedBy.foreach { usage ⇒
                                if (instructions(usage).isReturnInstruction) {
                                    val use = s" the argument with origin $valueOrigin is returned by $usage"
                                    returnedParameters.add(methodSignature + use)
                                }
                            }
                        }
                    }

                    var pIndex = method.descriptor.parametersCount - 1
                    while (pIndex >= 0) {
                        validateArgument(parameterIndexToValueOrigin(isStatic, descriptor, pIndex))
                        pIndex -= 1
                    }

                    if (!isStatic) { // check implicit "this" parameter
                        validateArgument(valueOrigin = -1)
                    }
                }
            }

            exceptions.foreach { e ⇒
                e.printStackTrace()
                if (e.getCause() != null) e.getCause().printStackTrace()
                println()
            }

            (returnedParameters.asScala.toList.sorted, unusedParameters.asScala.toList.sorted)

        } { t ⇒ analysisTime = t.toSeconds }

        val occurences = returnedParameters.size
        BasicReport(
            returnedParameters.mkString("Directly returned parameters:\n", "\n", "\n\n") +
                unusedParameters.mkString("Unused parameters:\n", "\n", "\n\n") +
                s"\nThe analysis took $analysisTime and found $occurences direct returns"

        )
    }

}
