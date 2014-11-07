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
package l0

import java.net.URL
import org.opalj.collection.immutable.UIDSet
import org.opalj.br.analyses.{ OneStepAnalysis, AnalysisExecutor, BasicReport, Project }
import org.opalj.br.{ ClassFile, Method }
import org.opalj.br.{ ReferenceType }
import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.ai.IsAReferenceValue
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.NoUpdate
import org.opalj.ai.SomeUpdate

import org.opalj.ai.analyses.{ MethodReturnValuesAnalysis ⇒ TheAnalysis }
import org.opalj.ai.analyses.{ MethodReturnValuesAnalysisDomain ⇒ TheAnalysisDomain }

/**
 * A shallow analysis that tries to refine the return types of methods.
 *
 * @author Michael Eichberg
 */
object MethodReturnValuesAnalysis
        extends AnalysisExecutor
        with OneStepAnalysis[URL, BasicReport] {

    val analysis = this

    override def title: String = TheAnalysis.title

    override def description: String = TheAnalysis.description

    override def doAnalyze(
        theProject: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean) = {
        import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }

        val candidates = new java.util.concurrent.atomic.AtomicInteger(0)

        val methodsWithRefinedReturnTypes: Map[Method, Option[TheAnalysisDomain#DomainValue]] = time {
            TheAnalysis.doAnalyze(theProject, parameters, isInterrupted)
        } { t ⇒ println(f"Analysis time: ${ns2sec(t)}%2.2f seconds.") }

        val results =
            methodsWithRefinedReturnTypes.map { result ⇒
                val (method, value) = result
                RefinedReturnType[TheAnalysisDomain](theProject.classFile(method), method, value)
            }

        BasicReport(
            results.mkString(
                "Methods with refined return types ("+
                    results.size+" out of "+candidates.get+
                    "): \n",
                "\n",
                "\n"))
    }
}

case class RefinedReturnType[D <: Domain](
        classFile: ClassFile,
        method: Method,
        refinedType: Option[D#DomainValue]) {

    override def toString = {
        import Console._
        val declaringClassOfMethod = classFile.thisType.toJava

        "Refined the return type of "+BOLD + BLUE +
            declaringClassOfMethod+"{ "+method.toJava+" }"+
            " => "+GREEN + refinedType.getOrElse("\"NONE\" (the method never returns normally)") + RESET
    }

}

