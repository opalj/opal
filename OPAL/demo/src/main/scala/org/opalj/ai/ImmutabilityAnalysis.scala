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

import java.net.URL

import org.opalj.ai.analyses.Immutability
import org.opalj.ai.analyses.{ ImmutabilityAnalysis ⇒ IA }
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project

/**
 * A analysis that collects all classes that are immutable inside a jar.
 *
 * @author Andre Pacak
 */
object ImmutabilityAnalysis extends AnalysisExecutor {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def doAnalyze(
            theProject: Project[URL],
            parameters: Seq[String],
            isInterrupted: () ⇒ Boolean): BasicReport = {

            val result = IA.doAnalyze(theProject, isInterrupted)
            val classFiles = theProject.classFiles.filter {
                classFile ⇒
                    classFile.isClassDeclaration &&
                        !classFile.isInnerClass
            }
            val relevantClasses = result.filter {
                x ⇒
                    val classFile = theProject.classFile(x._1)
                    classFile.nonEmpty &&
                        classFile.get.isClassDeclaration &&
                        !classFile.get.isInnerClass
            }

            val immutableClasses = relevantClasses.filter {
                _._2 == Immutability.Immutable
            }
            val condimmutableClasses = relevantClasses.filter {
                _._2 == Immutability.ConditionallyImmutable
            }
            val mutableClasses = relevantClasses.filter { _._2 == Immutability.Mutable }
            val unknownClasses = relevantClasses.filter { _._2 == Immutability.Unknown }
            val message = new StringBuffer("The Jar contains "+classFiles.size+" Classes\n")
            message append immutableClasses.size+" classes are immutable\n"
            message append condimmutableClasses.size+" classes are conditionally immutable\n"
            message append mutableClasses.size+" classes are mutable\n"
            message append unknownClasses.size+" classes cannot be classified"

            BasicReport(message.toString)
        }
    }
}