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
 * Counts the number of `Class.forName` calls.
 *
 * @author Michael Eichberg
 */
// Demonstrates how to do pattern matching of instructions and how to use the `AnalysisExecutor`.
object CountClassForNameCalls extends AnalysisExecutor {

    val analysis = new Analysis[URL, BasicReport] {

        def description: String = "Counts the number of times Class.forName is called."

        def analyze(project: Project[URL], parameters: Seq[String]) = {
            var classForNameCount = 0

            import ObjectType.{ String, Class }
            // Next, we create a descriptor of a method that takes a single parameter of 
            // type "String" and that returns a value of type Class.
            val descriptor = MethodDescriptor(String, Class)
            val invokes = 
                // The following collects all calls of the method "forName" on
                // an object of type "java.lang.Class".
                for {
                    // Let's traverse all methods of all class files that have a 
                    // concrete (non-native) implementation. 
                    classFile ← project.classFiles
                    method @ MethodWithBody(code) ← classFile.methods
                    // Associate each instruction with its index to make it possible
                    // to distinguish multiple invocations of "Class.forName" within
                    // the same method.
                    instructions = code.associateWithIndex
                    // Match all invocations of the method:
                    // Class.forName(String) : Class<?>
                    (pc, INVOKESTATIC(Class, "forName", `descriptor`)) ← instructions
                } yield {
                    classForNameCount += 1
                    classFile.fqn+" { "+method.toJava+"{ pc="+pc+" } }"
                }
            val uniqueInvokes = invokes.toSet

            BasicReport(
                "Class.forName(String) was called: "+classForNameCount+" times.\n\t"+
                    uniqueInvokes.mkString("\n\t")
            )
        }
    }
}