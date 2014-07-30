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

import org.opalj.br.analyses.Analysis
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project

/**
 * Shows the inner classes attributes of given class files.
 *
 * @author Daniel Klauer
 * @author Michael Eichberg
 */
object ShowInnerClassesInformation extends AnalysisExecutor {
    val analysis = new Analysis[URL, BasicReport] {
        def description: String = "Prints out the inner classes tables."

        def analyze(
            project: Project[URL],
            parameters: Seq[String] = List.empty): BasicReport = {

            val messages =
                for {
                    classFile ← project.classFiles
                    if classFile.innerClasses.isDefined
                } yield {
                    val header =
                        classFile.fqn+"(ver:"+classFile.majorVersion+")"+":\n\t"+(
                            if (classFile.enclosingMethod.isDefined)
                                classFile.enclosingMethod.get.toString
                            else
                                "<no enclosing method defined>"
                        )+"\n\t"
                    classFile.innerClasses.get.mkString(header, "\n\t", "\n")
                }

            BasicReport(messages.mkString("\n"))
        }
    }
}
