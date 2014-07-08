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
package tutorial
package base

import java.net.URL
import org.opalj.br.analyses.{ Analysis, AnalysisExecutor, BasicReport, Project }
import org.opalj.br.MethodWithBody
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.SingleArgumentMethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.VoidType
import org.opalj.br.Method
import org.opalj.br.ClassFile

/**
 * @author Michael Eichberg
 */
object IdentifyResourcesAnalysis extends AnalysisExecutor {

    val analysis = new Analysis[URL, BasicReport] {

        override def title: String = "Identifies Resources"

        override def description: String = "Identifies methods that call resources"

        override def analyze(theProject: Project[URL], parameters: Seq[String]) = {

            // Step 1
            // Find all methods that create "java.io.File(<String>)" objects.
            val callSites = (for {
                cf ← theProject.classFiles
                m @ MethodWithBody(body) ← cf.methods
            } yield {
                val pcs = for {
                    pc ← body.collectWithIndex {
                        case (pc, INVOKESPECIAL(
                            ObjectType("java/io/File"),
                            "<init>",
                            SingleArgumentMethodDescriptor((ObjectType.String, VoidType)))) ⇒ pc
                    }
                } yield pc
                (cf, m, pcs)
            }).filter(_._3.size > 0)

            // Step 2
            // Perform a simple abstract 

            def toString(callSite: (ClassFile, Method, Seq[PC])): String = {
                callSite._1.thisType.toJava+
                    "{ "+callSite._2.toJava+
                    "{"+callSite._3.mkString("PCs:", ",", "")+"} }"
            }
            BasicReport(callSites.map(toString(_)).mkString("Methods:\n", "\n", ""))
        }
    }
}



