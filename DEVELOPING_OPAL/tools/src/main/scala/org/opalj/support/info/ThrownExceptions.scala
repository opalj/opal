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
package support
package info

import java.net.URL

import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.fpcf.properties.AllThrownExceptions
import org.opalj.fpcf.properties.ThrownExceptionsFallbackAnalysis
import org.opalj.fpcf.analyses.L1ThrownExceptionsAnalysis
import org.opalj.fpcf.analyses.ThrownExceptionsByOverridingMethods
import org.opalj.fpcf.properties.NoExceptionsAreThrown.MethodIsAbstract

/**
 * Prints out the information about the exceptions thrown by methods.
 *
 * @author Michael Eichberg
 * @author Andreas Muttschelller
 */
object ThrownExceptions extends DefaultOneStepAnalysis {

    override def title: String = "Thrown Exceptions"

    override def description: String = "computes the set of exceptions thrown by methods"

    final val L1TEParameter = "-analysis=L1ThrownExceptionsAnalysis"

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        super.checkAnalysisSpecificParameters(parameters.filter(_ != L1TEParameter))
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val ps = project.get(PropertyStoreKey)

        if (parameters.contains(L1TEParameter)) {
            L1ThrownExceptionsAnalysis.start(project, ps)
            // IMPROVE ThrownExceptionsByOverridingMethods.startLazily(project, ps)
            ThrownExceptionsByOverridingMethods.start(project, ps)
        } else {
            val fallbackAnalysis = new ThrownExceptionsFallbackAnalysis(ps)
            ps.scheduleForEntities(project.allMethods)(fallbackAnalysis)
        }

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
            methodsWithCompleteThrownExceptionsInfo.view.count(_._1.isPrivate)
        val methodsWhichDoNotThrowExceptionsCount =
            methodsWhichDoNotThrowExceptions.view.count(_._1.isPrivate)

        val report = methodsWithCompleteThrownExceptionsInfo.map {
            case (m: Method, ts: AllThrownExceptions) ⇒ { s"${m.toJava} ⇒ $ts" }
        }.toList.sorted.mkString("\n")

        BasicReport(
            report+
                "\n\nNumber of methods for which the set of thrown exceptions is known: "+
                methodsWithCompleteThrownExceptionsInfoCount+"\n"+
                s" ... private methods: $privateMethodsWithCompleteThrownExceptionsInfoCount\n"+
                s" ... which throw no exceptions: ${methodsWhichDoNotThrowExceptions.size}\n"+
                s" ... ... private methods: $methodsWhichDoNotThrowExceptionsCount"
        )
    }
}
