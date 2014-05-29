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
 * @author Michael Eichberg
 */
object MaxLocalsEvaluation extends AnalysisExecutor {

    val analysis = new Analysis[URL, BasicReport] {

        def description: String =
            "Collects information about the maxium number of registers required per method."

        def analyze(project: Project[URL], parameters: Seq[String] = List.empty) = {
            import scala.collection.immutable.TreeMap// <= Sorted...
            var methodParametersDistribution: Map[Int, Int] = TreeMap.empty
            var maxLocalsDistrbution: Map[Int, Int] = TreeMap.empty

            for {
                classFile ← project.classFiles;
                method @ MethodWithBody(body) ← classFile.methods
            } {
                val parametersCount = method.descriptor.parametersCount + (if (method.isStatic) 0 else 1)
                require(body.maxLocals >= parametersCount)
                
                methodParametersDistribution = methodParametersDistribution.updated(
                    parametersCount,
                    methodParametersDistribution.getOrElse(parametersCount, 0) + 1
                )

                maxLocalsDistrbution = maxLocalsDistrbution.updated(
                    body.maxLocals,
                    maxLocalsDistrbution.getOrElse(body.maxLocals, 0) + 1
                )
            }

            BasicReport("Results\n\n"+
                "Method Parameters Distribution:\n"+
                methodParametersDistribution.map(kv ⇒ { val (k, v) = kv; k+"\t"+v }).mkString("\n")+"\n"+
                "MaxLocals Distribution:\n"+
                maxLocalsDistrbution.map(kv ⇒ { val (k, v) = kv; k+"\t"+v }).mkString("\n")
            )
        }
    }
}