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
package analyses

import java.net.URL
import org.opalj.util.NanoSeconds

/**
 * Basic information about (non-)instantiable classes.
 *
 * ==Example Usage==
 * {{{
 * run
 * }}}
 *
 * @author Michael Eichberg
 */
object NonInstantiableClasses extends DefaultOneStepAnalysis {

    override def description: String = "Provides information about instantiable classes."

    def doAnalyze(
        project: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean) = {

        import org.opalj.util.PerformanceEvaluation.{ time, memory, asMB }
        var overallExecutionTime = NanoSeconds.None
        var memoryUsageInMB = ""

        val instantiableClasses =
            memory {
                time {
                    project.get(InstantiableClassesKey)
                } { t ⇒ overallExecutionTime += t }
            } { memoryUsage ⇒ memoryUsageInMB = asMB(memoryUsage) }

        val notInstantiableClasse = instantiableClasses.notInstantiable.map { ot ⇒
            val methods = project.classFile(ot).get.methods
            val instanceMethods = methods.filter { m ⇒ !m.isStatic && !m.isConstructor }
            ot.toJava+"("+instanceMethods.size+")"
        }
        val sortedNotInstantiableClasse = notInstantiableClasse.toSeq.sorted

        BasicReport(
            instantiableClasses.statistics.mkString(
                "determing non-instantiable classes "+
                    "took "+overallExecutionTime.toSeconds+" "+
                    "and required "+memoryUsageInMB+":\n",
                "\n",
                "\n"
            ) +
                sortedNotInstantiableClasse.mkString("List of all non-instantiable classes:\n\t", "\n\t", "\n")
        )
    }
}
