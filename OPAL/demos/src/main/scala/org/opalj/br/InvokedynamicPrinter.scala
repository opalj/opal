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
import org.opalj.br.instructions.INVOKEDYNAMIC

/**
 * Prints out the immediately available information about invokedynamic instructions.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object InvokedynamicPrinter extends AnalysisExecutor with OneStepAnalysis[URL, BasicReport] {

    val analysis = this

    override def description: String =
        "Prints information about invokedynamic instructions."

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {
        import scala.collection.JavaConversions._
        val invokedynamics = new java.util.concurrent.ConcurrentLinkedQueue[String]
        project.parForeachMethodWithBody(isInterrupted) { e ⇒
            val (_, classFile, method) = e
            invokedynamics.addAll(
                method.body.get.collectWithIndex {
                    case (pc, INVOKEDYNAMIC(bootstrap, name, descriptor)) ⇒
                        classFile.thisType.toJava+" {\n  "+method.toJava()+"{ "+pc+": \n"+
                            s"    ${bootstrap.toJava}\n"+
                            bootstrap.bootstrapArguments.mkString("    Arguments: {", ",", "}\n") +
                            s"    Calling:   ${descriptor.toJava(name)}\n"+
                            "} }\n"
                }
            )
        }
        val result = invokedynamics.toSeq.sorted
        BasicReport(
            result.mkString(result.size+" invokedynamic instructions found:\n", "\n", "\n")
        )
    }

}
