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
package bugpicker
package core
package analysis

import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.domain.TheCode
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.ACONST_NULL
import org.opalj.br.instructions.ICONST_0
import org.opalj.br.instructions.DCONST_0
import org.opalj.br.instructions.LCONST_0
import org.opalj.br.instructions.FCONST_0
import org.opalj.br.instructions.StoreLocalVariableInstruction

/**
 * Identifies unused local variables
 *
 * @author Michael Eichberg
 */
object UnusedLocalVariables {

    def analyze(
        theProject: SomeProject,
        classFile:  ClassFile,
        method:     Method,
        result:     AIResult { val domain: Domain with TheCode with RecordDefUse }
    ): Seq[StandardIssue] = {

        if (method.isSynthetic)
            return Nil;

        val unused = result.domain.unused()
        if (unused.isEmpty)
            return Nil;
        val code = result.domain.code
        val instructions = code.instructions
        var issues = List.empty[StandardIssue]
        val implicitParameterOffset = if (!method.isStatic) 1 else 0
        unused.foreach { vo ⇒
            var issue: String = null
            var relevance: Relevance = Relevance.Undetermined
            if (vo < 0) {
                // we have to make sure that we do not create an issue report
                // for instance methods that can be/are inherited
                if (method.isStatic ||
                    method.isPrivate ||
                    // TODO check that the method parameter is never used... across all implementations of the method... only then report it...|| 
                    method.name == "<init>") {
                    relevance = Relevance.High
                    if (vo == -1) {
                        issue = "the self reference \"this\" is unused"
                    } else {
                        issue = "the paramter with index "+(-(vo + implicitParameterOffset))+" is unused"
                    }
                }
            } else {
                val instruction = instructions(vo)
                instruction.opcode match {
                    case INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode |
                        INVOKESTATIC.opcode | INVOKESPECIAL.opcode ⇒
                        val invoke = instruction.asInstanceOf[MethodInvocationInstruction]
                        issue = "the return value of the call of "+invoke.declaringClass.toJava+
                            "{ "+
                            invoke.methodDescriptor.toJava(invoke.name)+
                            " } is not used"
                        // TODO we need an assessment how important it is to ignore the return value...
                        relevance = Relevance.DefaultRelevance
                    case ACONST_NULL.opcode |
                        ICONST_0.opcode |
                        LCONST_0.opcode |
                        FCONST_0.opcode |
                        DCONST_0.opcode ⇒
                        val nextPC = code.pcOfNextInstruction(vo)
                        instructions(nextPC) match {
                            case StoreLocalVariableInstruction((_, index)) ⇒
                                val lvOption = code.localVariable(nextPC, index)
                                if (lvOption.isDefined && (
                                    lvOption.get.startPC < vo || lvOption.get.startPC > nextPC
                                )) {
                                    issue = s"the constant value ${instruction.toString(vo)} is not used"
                                    relevance = Relevance.Low
                                }
                            // else... we filter basically all issues unless we are sure that this is real...
                            case _ ⇒
                                issue = s"the constant value ${instruction.toString(vo)} is (most likely) used to initialize a local variable"
                                relevance = Relevance.TechnicalArtifact
                        }

                    case IINC.opcode ⇒
                        issue = "the incremented value is not used"
                        relevance = Relevance.DefaultRelevance

                    case _ ⇒
                        issue = "the value of the expression "+instruction.toString(vo)+" is not used"
                        relevance = Relevance.OfUtmostRelevance
                }

            }
            if (issue ne null) {
                issues ::= StandardIssue(
                    "UnusedLocalVariables",
                    theProject,
                    classFile,
                    Some(method),
                    if (vo >= 0) Some(vo) else None,
                    None,
                    None,
                    issue,
                    None,
                    Set(IssueCategory.Smell, IssueCategory.Performance),
                    Set(IssueKind.Useless),
                    Nil,
                    relevance
                )
            }
        }

        issues

    }

}
