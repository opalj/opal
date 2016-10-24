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

import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project

/**
 * Counts the number of methods that override/implement an instance method.
 *
 * @author Michael Eichberg
 */
object CountOverridingMethods extends AnalysisExecutor {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def description: String = "Counts the number of methods that override a method."

        def doAnalyze(
            project:       Project[URL],
            parameters:    Seq[String],
            isInterrupted: () ⇒ Boolean
        ) = {

            val overridingMethodsInfo =
                project.overridingMethods.view.
                    map(ms ⇒ (ms._1, ms._2 - ms._1)).
                    filter(_._2.nonEmpty).map { ms ⇒
                        val (method, allOverridingMethods) = ms
                        val overridingMethods = allOverridingMethods.map(m ⇒ project.classFile(m).fqn)
                        (method, (overridingMethods, overridingMethods.size))
                        val count = overridingMethods.size
                        method.toJava(
                            project.classFile(method),
                            overridingMethods.mkString(s"\n\thas $count overridde(s):\n\t\t", "\n\t\t", "\n")
                        )
                    }

            BasicReport(
                "Overriding\n"+overridingMethodsInfo.mkString("\n")
            )
        }
    }
}
