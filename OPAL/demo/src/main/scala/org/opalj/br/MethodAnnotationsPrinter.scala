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

import analyses.{ Analysis, AnalysisExecutor, BasicReport, Project }
import java.net.URL

/**
 * Prints out the method-level annotations of all methods. (I.e., class, field and
 * parameter annotations are not printed.)
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object MethodAnnotationsPrinter extends AnalysisExecutor {

    val analysis = new Analysis[URL, BasicReport] {

        def description: String = "Prints out the annotations of methods."

        def analyze(project: Project[URL], parameters: Seq[String]) = {
            val annotations =
                for {
                    classFile ← project.classFiles
                    method ← classFile.methods
                    annotation ← method.runtimeVisibleAnnotations ++ method.runtimeInvisibleAnnotations
                } yield {
                    "on method: "+classFile.thisType.toJava+"."+method.name +
                        method.parameterTypes.map(_.toJava).mkString("(", ",", ")\n") +
                        annotation.elementValuePairs.map(pair ⇒ "%-15s: %s".format(pair.name, pair.value.toJava)).
                        mkString("\t@"+annotation.annotationType.toJava+"\n\t", "\n\t", "\n")
                }

            BasicReport(
                annotations.size+" annotations found: "+
                    annotations.mkString("\n", "\n", "\n"))
        }
    }
}