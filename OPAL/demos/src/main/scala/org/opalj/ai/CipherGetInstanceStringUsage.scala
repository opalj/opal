/*
 * BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
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

package org.opalj.ai

import java.net.URL

import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicMethodInfo
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.LoadString

/**
 * The analysis demonstrates how to find values passed to Chipher.getInstance.
 *
 * @author Michael Reif
 */
object CipherGetInstanceStringUsage extends DefaultOneStepAnalysis {

    override def title: String =
        "Input value analysis for Chipher.getInstance calls"

    override def description: String = "Analyzes the input values of Chipher.getInstance calls."

    /*
    *static Chipher getInstance(String transformation)
    *static Cipher	getInstance(String transformation, Provider provider)
    *static Cipher	getInstance(String transformation, String provider)
  */

    // #################### CONSTANTS ####################

    val Cipher = ObjectType("javax/crypto/Cipher")

    val Key = ObjectType("java/security/Key")

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        var report = List.empty[String]

        val ai = new InterruptableAI[Domain]

        project.parForeachMethodWithBody() { m ⇒
            val BasicMethodInfo(classFile, method) = m

            val domain = new DefaultDomainWithCFGAndDefUse(project, classFile, method)
            val result = ai(classFile, method, domain)
            implicit val code = result.domain.code

            for {
                (pc, INVOKESTATIC(Cipher, false, "getInstance", _)) ← code
                vos ← domain.operandOrigin(pc, 0)
            } yield {

                // getInstance is static, algorithm is first param
                code.instructions(vos) match {
                    case LoadString(value) ⇒ report = value :: report
                    case invoke @ INVOKEINTERFACE(
                        Key,
                        "getAlgorithm",
                        MethodDescriptor.JustReturnsString) ⇒
                        report = invoke.toString :: report

                    case get @ GETFIELD(_, _, _) ⇒ println("unknwon inst: ")
                    case unknownInstr            ⇒ println("unknwon inst: "+unknownInstr)
                }

            }
        }

        BasicReport(report.mkString("\n"))
    }
}
