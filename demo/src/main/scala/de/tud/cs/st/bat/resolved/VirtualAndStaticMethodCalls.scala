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
package de.tud.cs.st
package bat
package resolved

import instructions._
import analyses.{ Analysis, AnalysisExecutor, BasicReport, Project }

import java.net.URL

/**
 * Counts the number of static and virtual method calls.
 *
 * @author Michael Eichberg
 */
object VirtualAndStaticMethodCalls extends AnalysisExecutor {

    val analysis = new Analysis[URL, BasicReport] {

        def description: String = "Counts the number of static and virtual method calls."

        def analyze(project: Project[URL], parameters: Seq[String] = List.empty) = {
            var staticCalls = 0
            var virtualCalls = 0
            for {
                classFile ← project.classFiles
                MethodWithBody(code) ← classFile.methods
                invokeInstruction @ MethodInvocationInstruction(_, _, _) ← code.instructions
            } {
                if (invokeInstruction.isVirtualMethodCall)
                    virtualCalls += 1
                else
                    staticCalls += 1
            }

            BasicReport(
                "Number of invokestatic/invokespecial instructions: "+staticCalls+"\n"+
                    "Number of invokeinterface/invokevirtual instructions: "+virtualCalls
            )
        }
    }
}