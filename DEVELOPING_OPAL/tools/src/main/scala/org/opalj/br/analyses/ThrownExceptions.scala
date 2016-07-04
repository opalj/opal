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
import org.opalj.br.analyses.{DefaultOneStepAnalysis, BasicReport, Project}
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.properties.ThrownExceptionsFallbackAnalysis
import org.opalj.fpcf.properties.AllThrownExceptions
import org.opalj.fpcf.properties.NoExceptionsAreThrown

/**
 * Prints out the string constants found in the code.
 *
 * @author Michael Eichberg
 */
object ThrownExceptions extends DefaultOneStepAnalysis {

    override def description: String = "approximates the set of exceptions thrown by methods"

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {
        val ps = project.get(SourceElementsPropertyStoreKey)

        ps <||< (PropertyStore.entitySelector[Method], new ThrownExceptionsFallbackAnalysis(ps))

        ps.waitOnPropertyComputationCompletion(true)

        val report = ps.collect {
            case (m: Method, ts: AllThrownExceptions) if ts != NoExceptionsAreThrown.MethodIsAbstract ⇒ {
                s"${m.toJava(project.classFile(m))} ⇒ $ts"
            }
        }.toList.sorted.mkString("\n")

        val methodsThatDoNotThrowExceptionsCount =
            ps.entities(NoExceptionsAreThrown.NoInstructionThrowsExceptions).size

        BasicReport(
            report+
                "\n\nNumber of methods which do not throw exceptions: "+
                methodsThatDoNotThrowExceptionsCount
        )
    }
}
