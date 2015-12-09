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
package domain
package l1

import scala.language.existentials
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._
import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.graphs.DefaultMutableNode
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import scala.collection.immutable.ListSet
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.MethodInvocationInstruction

/**
 * Simple analysis that takes the "unused"-Node from the def-use graph
 * and returns all its children,
 * i.e. definitions and assignments that are not used again locally.
 *
 * @author Stephan Neumann
 */
object SimpleDefUseAnalysis extends DefaultOneStepAnalysis {

    override def title: String =
        "Identifies unused variables and unnecessary calculations"

    override def description: String =
        "Identifies variable declarations or assignments that are not used again locally"

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {

        var analysisTime: Seconds = Seconds.None
        val unusedDefUseNodes = time {

            val results = new ConcurrentLinkedQueue[String]
            val ai = new InterruptableAI[Domain]

            theProject.parForeachMethodWithBody() { m ⇒
                val (_, classFile, method) = m
                if (!method.isSynthetic) {

                    val domain = new DefaultDomainWithCFGAndDefUse(theProject, classFile, method)
                    val result = ai(classFile, method, domain)
                    val instructions = result.domain.code.instructions
                    val unused = result.domain.unused()
                    if (unused.nonEmpty) {
                        var values = ListSet.empty[String]
                        val implicitParameterOffset = if (!method.isStatic) 1 else 0
                        unused.foreach { vo ⇒
                            if (vo < 0) {
                                // we have to make sure that we do not create an issue report
                                // for instance methods that can be/are inherited
                                if (method.isStatic ||
                                    method.isPrivate ||
                                    // TODO check that the method parameter is never used... across all implementations of the method... only then report it...|| 
                                    method.name == "<init>") {
                                    if (vo == -1) {
                                        values += "this"
                                    } else {
                                        values += "param:"+(-(vo + implicitParameterOffset))
                                    }
                                }
                            } else {
                                val instruction = instructions(vo)
                                instruction.opcode match {
                                    case INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode |
                                        INVOKESTATIC.opcode | INVOKESPECIAL.opcode ⇒
                                        val invoke = instruction.asInstanceOf[MethodInvocationInstruction]
                                        values +=
                                            vo.toString+": invoke "+invoke.declaringClass.toJava+
                                            "{ "+
                                            invoke.methodDescriptor.toJava(invoke.name)+
                                            " }"
                                    case _ ⇒
                                        values += vo.toString+": "+instruction.toString(vo)
                                }

                            }

                        }
                        if (values.nonEmpty)
                            results.add(method.toJava(classFile) + values.mkString("{", ",", "}"))
                    }
                }
            }
            results.asScala

        } { t ⇒ analysisTime = t.toSeconds }

        BasicReport(
            unusedDefUseNodes.mkString("Methods with unused values:\n", "\n", "\n")+
                "The analysis took "+analysisTime+" and found "+unusedDefUseNodes.size+" issues"
        )
    }

}