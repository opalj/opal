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
package org.opalj.fpcf

import java.net.URL

import org.opalj.br.Method
import org.opalj.br.analyses.{BasicReport, DefaultOneStepAnalysis, Project, PropertyStoreKey}
import org.opalj.fpcf.properties.AllThrownExceptions
import org.opalj.fpcf.properties.NoExceptionsAreThrown.MethodIsAbstract

/**
 * Runs the transitive thrown exception analysis.
 *
 * @author Andreas Muttschelller
 */
object TransitiveThrownExceptionsAnalysisRunner extends DefaultOneStepAnalysis {

    override def title: String = "assess the thrown exceptions of methods"

    override def description: String = { "assess the thrown exceptions of some methods" }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val ps = project.get(PropertyStoreKey)

        org.opalj.fpcf.analyses.TransitiveThrownExceptionsAnalysis.start(project, ps)

        ps.waitOnPropertyComputationCompletion(true)

        val methodsWithCompleteThrownExceptionsInfo = ps.collect {
            case (m: Method, ts: AllThrownExceptions) if ts != MethodIsAbstract ⇒ (m, ts)
        }
        val methodsWhichDoNotThrowExceptions = methodsWithCompleteThrownExceptionsInfo.collect {
            case e @ (m: Method, ts: AllThrownExceptions) if ts.types.isEmpty ⇒ e
        }

        val methodsWithCompleteThrownExceptionsInfoCount =
            methodsWithCompleteThrownExceptionsInfo.size
        val privateMethodsWithCompleteThrownExceptionsInfoCount =
            methodsWithCompleteThrownExceptionsInfo.view.filter(_._1.isPrivate).size
        val methodsWhichDoNotThrowExceptionsCount =
            methodsWhichDoNotThrowExceptions.view.filter(_._1.isPrivate).size

        val report = methodsWithCompleteThrownExceptionsInfo.map {
            case (m: Method, ts: AllThrownExceptions) ⇒ { s"${m.toJava} ⇒ $ts" }
        }.toList.sorted.mkString("\n")

        BasicReport(
            report+
                "\n\nNumber of methods for which the set of thrown exceptions is known: "+
                methodsWithCompleteThrownExceptionsInfoCount+"\n"+
                s" ... private methods: ${privateMethodsWithCompleteThrownExceptionsInfoCount}\n"+
                s" ... which throw no exceptions: ${methodsWhichDoNotThrowExceptions.size}\n"+
                s" ... ... private methods: $methodsWhichDoNotThrowExceptionsCount"
        )
    }
}
