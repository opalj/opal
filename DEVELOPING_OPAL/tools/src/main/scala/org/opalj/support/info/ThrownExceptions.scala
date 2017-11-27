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
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AllThrownExceptions
import org.opalj.fpcf.properties.ThrownExceptionsFallbackAnalysis
import org.opalj.fpcf.analyses.L1ThrownExceptionsAnalysis
import org.opalj.fpcf.analyses.ThrownExceptionsByOverridingMethodsAnalysis

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
            // IMPROVE ThrownExceptionsByOverridingMethods.startLazily(project, ps) is for no apparent reason totally crashing..
            ThrownExceptionsByOverridingMethodsAnalysis.start(project, ps)
        } else {
            val fallbackAnalysis = new ThrownExceptionsFallbackAnalysis(ps)
            ps.scheduleForEntities(project.allMethods)(fallbackAnalysis)
        }

        ps.waitOnPropertyComputationCompletion(true)

        val methodsWithAllThrownExceptions = {
            ps.entities((p: Property) ⇒ p.isInstanceOf[AllThrownExceptions]).map { e ⇒
                e.asInstanceOf[Method]
            }
        }
        val methodsWhichDoNotThrowExceptions = methodsWithAllThrownExceptions.filter { m ⇒
            ps(m, fpcf.properties.ThrownExceptions.Key).p.throwsNoExceptions
        }

        val methodsWithAllThrownExceptionsCount = methodsWithAllThrownExceptions.size
        val privateMethodsWithAllThrownExceptionsCount =
            methodsWithAllThrownExceptions.count(_.isPrivate)
        val methodsWhichDoNotThrowExceptionsCount =
            methodsWhichDoNotThrowExceptions.view.count(_.isPrivate)

        val report = methodsWithAllThrownExceptions.map { e ⇒
            val m: Method = e.asInstanceOf[Method]
            val thrownExceptions = ps(m, fpcf.properties.ThrownExceptions.Key).p
            s"${m.toJava} ⇒ $thrownExceptions"
        }.toList.sorted.mkString("\n")

        BasicReport(
            report+
                "\n\nNumber of methods for which the set of thrown exceptions is known: "+
                methodsWithAllThrownExceptionsCount+"\n"+
                s" ... private methods: $privateMethodsWithAllThrownExceptionsCount\n"+
                s" ... which throw no exceptions: ${methodsWhichDoNotThrowExceptions.size}\n"+
                s" ... ... private methods: $methodsWhichDoNotThrowExceptionsCount"
        )
    }
}
