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

import analyses.{ OneStepAnalysis, AnalysisExecutor, BasicReport, Project }
import java.net.URL
import org.opalj.br.analyses.ClassHierarchy

/**
 * Counts the number of private methods that have at least one parameter with
 * a reference type.
 *
 * @author Michael Eichberg
 */
object PrivateMethodsWithObjectTypeParameterCounter extends AnalysisExecutor {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def description: String = "Counts the number of private methods with at least one parameter that is an object type."

        def doAnalyze(
            project: Project[URL],
            parameters: Seq[String],
            isInterrupted: () ⇒ Boolean) = {
            val methods =
                for {
                    classFile ← project.classFiles;
                    method ← classFile.methods
                    if method.isPrivate
                    if method.descriptor.parameterTypes.exists { pt ⇒
                        pt.isObjectType && {
                            val objectType = pt.asObjectType
                            project.classHierarchy.hasSubtypes(objectType).isYes && (
                                project.classFile(objectType) match {
                                    case Some(cf) ⇒ !cf.isFinal
                                    case _        ⇒ false
                                }
                            )
                        }
                    }
                } yield classFile.thisType.toJava+"{ "+method.toJava+" }"

            BasicReport(
                methods.mkString(s"${methods.size} methods found:\n\t", "\n\t", "\n")
            )
        }
    }
}