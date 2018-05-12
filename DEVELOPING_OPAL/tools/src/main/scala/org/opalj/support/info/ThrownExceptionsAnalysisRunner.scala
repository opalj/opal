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
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.analyses.LazyVirtualMethodThrownExceptionsAnalysis
import org.opalj.fpcf.analyses.EagerL1ThrownExceptionsAnalysis
import org.opalj.fpcf.properties.ThrownExceptions
import org.opalj.fpcf.properties.ThrownExceptionsByOverridingMethods
import org.opalj.fpcf.properties.ThrownExceptionsFallback
import org.opalj.util.PerformanceEvaluation._

/**
 * Prints out the information about the exceptions thrown by methods.
 *
 * @author Michael Eichberg
 * @author Andreas Muttschelller
 */
object ThrownExceptionsAnalysisRunner extends DefaultOneStepAnalysis {

    override def title: String = "Thrown Exceptions"

    override def description: String = "computes the set of exceptions thrown by methods"

    final val L1TEParameter = "-analysis=L1ThrownExceptionsAnalysis"
    final val suppressPerMethodReports = "-suppressPerMethodReports"

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        super.checkAnalysisSpecificParameters(
            parameters.filter(p ⇒ p != L1TEParameter && p != suppressPerMethodReports)
        )
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val ps = project.get(PropertyStoreKey)
        ps.setupPhase(Set(ThrownExceptions.key, ThrownExceptionsByOverridingMethods.key))

        time {
            if (parameters.contains(L1TEParameter)) {
                LazyVirtualMethodThrownExceptionsAnalysis.startLazily(project, ps)
                EagerL1ThrownExceptionsAnalysis.start(project, ps)
            } else {
                val fallbackAnalysis = new ThrownExceptionsFallback(ps)
                ps.scheduleForEntities(project.allMethods)(fallbackAnalysis)
            }

            ps.waitOnPhaseCompletion()
        } { n ⇒
            println(s"ThrownExceptionsAnalysis took ${n.toSeconds.toString}")
        }

        val allMethods = ps.entities(ThrownExceptions.key).toIterable
        val (epsWithThrownExceptions, epsWhichDoNotThrowExceptions) =
            allMethods.partition(_.ub.throwsNoExceptions)
        val methodsWithThrownExceptions = epsWithThrownExceptions.map(_.e.asInstanceOf[Method])
        val privateMethodsWhichDoNotThrowExceptions =
            epsWhichDoNotThrowExceptions.map(_.e.asInstanceOf[Method])

        val methodsWithThrownExceptionsCount = methodsWithThrownExceptions.size
        val privateMethodsWithThrownExceptionsCount =
            methodsWithThrownExceptions.count(_.isPrivate)
        val methodsWhichDoNotThrowExceptionsCount =
            privateMethodsWhichDoNotThrowExceptions.count(_.isPrivate)

        val report =
            if (parameters.contains(suppressPerMethodReports))
                ""
            else
                epsWithThrownExceptions.map { p ⇒
                    s"${p.e.asInstanceOf[Method].toJava} ⇒ ${p.ub}"
                }.toList.sorted.mkString("\n")

        BasicReport(
            report +
                ps.toString(false)+
                "\n\nNumber of methods for which the set of thrown exceptions is known: "+
                methodsWithThrownExceptionsCount + s" / ${allMethods.size}\n"+
                s" ... private methods: $privateMethodsWithThrownExceptionsCount\n"+
                s" ... which throw no exceptions: $methodsWhichDoNotThrowExceptionsCount\n"+
                s" ... ... private methods: ${privateMethodsWhichDoNotThrowExceptions.size}"
        )
    }
}
