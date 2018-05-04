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
package br

import java.net.URL
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.LoadMethodHandle
import org.opalj.br.instructions.LoadMethodHandle_W
import org.opalj.br.instructions.LoadMethodType
import org.opalj.br.instructions.LoadMethodType_W

/**
 * @author Michael Eichberg
 */
object LoadMethodHandleOrMethodType extends DefaultOneStepAnalysis {

    override def description: String = "prints information about loads of method handles and types"

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val loads =
            for {
                classFile ← project.allProjectClassFiles.par
                (method, code) ← classFile.methodsWithBody
                pcAndInstruction ← code collect {
                    case LoadMethodHandle(mh)   ⇒ mh
                    case LoadMethodHandle_W(mh) ⇒ mh
                    case LoadMethodType(md)     ⇒ md
                    case LoadMethodType_W(md)   ⇒ md
                }
            } yield {
                val pc = pcAndInstruction.pc
                val instruction = pcAndInstruction.value
                method.toJava(s"pc=$pc;load constant=${instruction.valueToString}") +
                    s"<${project.source(classFile.thisType).map(_.toString()).getOrElse("N/A")}>"
            }

        BasicReport(loads.seq.mkString("Instances of LoadMethod(Type|Handle):\n\t", "\n\t", "\n"))
    }
}
