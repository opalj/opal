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
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.util.NanoSeconds

/**
 * Basic field access information.
 *
 * ==Example Usage==
 * {{{
 * run -cp=/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/ -field="java.util.HashMap entrySet"
 * }}}
 *
 * @author Michael Eichberg
 */
object FieldAccessInformationAnalysis
        extends OneStepAnalysis[URL, BasicReport]
        with AnalysisExecutor {

    val analysis = this

    override def description: String = "Provides information about field accesses."

    override def analysisSpecificParametersDescription: String =
        "[-field=\"<The field for which we want read/write access information (e.g., -field=\"java.util.HashMap entrySet\">\"]"

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Seq[String] =
        if (parameters.isEmpty || (parameters.size == 1 && parameters.head.startsWith("-field=")))
            Seq.empty
        else
            Seq("unknown parameters: "+parameters.mkString(" "))

    def doAnalyze(
        project: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean) = {

        import org.opalj.util.PerformanceEvaluation.{ time, memory, asMB }
        var overallExecutionTime = NanoSeconds.None
        var memoryUsageInMB = ""

        val accessInformation = memory {
            time {
                project.get(FieldAccessInformationKey)
            } { t ⇒ overallExecutionTime += t }
        } { memoryUsage ⇒ memoryUsageInMB = asMB(memoryUsage) }

        if (parameters.nonEmpty) {
            val Array(declaringClassName, fieldName) =
                parameters.head.substring(7).replace('.', '/').split(' ')
            val declaringClassType = ObjectType(declaringClassName)
            val writes = accessInformation.writeAccesses(declaringClassType, fieldName)
            val reads = accessInformation.readAccesses(declaringClassType, fieldName)

            def accessInformationToString(data: Seq[(Method, PCs)]): String = {
                (
                    data.map { e ⇒
                        val (method, pcs) = e
                        project.classFile(method).thisType.toJava+" { "+
                            method.toJava() + pcs.mkString("{ pcs: ", ", ", " }")+
                            " }"
                    }
                ).mkString("\t ", "\n\t ", "\n")
            }

            BasicReport(
                declaringClassName+" "+fieldName+"\n"+
                    "writes:\n"+accessInformationToString(writes)+
                    "reads:\n"+accessInformationToString(reads)
            )

        } else {
            BasicReport(
                accessInformation.statistics.mkString(
                    s"determing field access information took ${overallExecutionTime.toSeconds} "+
                        s"and required $memoryUsageInMB:\n",
                    "\n",
                    "\n"
                )
            )
        }
    }
}
