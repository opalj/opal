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
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.analysis.methods.Purity
import org.opalj.fpcf.analysis.methods.Pure
import scala.util.control.ControlThrowable
import org.opalj.log.OPALLogger
import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.br.LocalVariable
import org.opalj.br.instructions.ICONST_M1
import org.opalj.br.instructions.IINC
import org.opalj.br.instructions.BIPUSH
import org.opalj.br.instructions.SIPUSH
import org.opalj.br.instructions.LoadInt
import org.opalj.br.instructions.LDC
import org.opalj.br.instructions.ICONST_1
import org.opalj.br.instructions.ICONST_3
import org.opalj.br.instructions.ICONST_4
import org.opalj.br.instructions.ICONST_5
import org.opalj.br.instructions.LCONST_1
import org.opalj.br.instructions.DCONST_1
import org.opalj.br.instructions.FCONST_1
import org.opalj.br.instructions.LDC_W
import org.opalj.br.instructions.LDC2_W
import org.opalj.br.instructions.ICONST_2
import org.opalj.br.instructions.FCONST_2

/**
 * Identifies unused local variables
 *
 * @author Michael Eichberg
 */
object UnusedLocalVariables {

    def apply(
        theProject:    SomeProject,
        propertyStore: PropertyStore,
        callGraph:     CallGraph,
        classFile:     ClassFile,
        method:        Method,
        result:        AIResult { val domain: Domain with TheCode with RecordDefUse }
    ): Seq[StandardIssue] = {

    		if (method.isSynthetic)
    			return Nil;
    		
        //
        //
        // IDENTIFYING RAW ISSUES
        //
        //
        

        val operandsArray = result.operandsArray
        val allUnused = result.domain.unused()
        val unused = allUnused.filter { vo ⇒
            // filter unused local variables related to dead code...
            // (we have another analysis for that)
            vo < 0 || (operandsArray(vo) ne null)
        }

        if (unused.isEmpty)
            return Nil;

        //
        //
        // POST PROCESSING ISSUES
        //
        //

        val code = result.domain.code
        val instructions = code.instructions
        var issues = List.empty[StandardIssue]
        val implicitParameterOffset = if (!method.isStatic) 1 else 0
        
        lazy val constantValues : Set[Any] = {
            code.collectWithIndex{
                case (pc, LoadConstantInstruction(value)) => value
            }
        }
        
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
                    if (vo == -1 && !method.isStatic) {
                        issue = "the self reference \"this\" is unused (the method could be static)"
                    } else {
                        val index = (-(vo + implicitParameterOffset))
                        code.localVariable(0, index - 1) match {
                            case Some(lv) ⇒ issue = s"the parameter ${lv.name} is unused"
                            case None     ⇒ issue = s"the $index. parameter is unused"
                        }
                    }
                }
            } else {
                val instruction = instructions(vo)
                instruction.opcode match {

                    case INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode |
                        INVOKESTATIC.opcode | INVOKESPECIAL.opcode ⇒
                        val invoke = instruction.asInstanceOf[MethodInvocationInstruction]
                        try {
                            val resolvedMethod: Iterable[Method] = callGraph.calls(method, vo)
                            // TODO Use a more precise method to determine if a method has a side effect "pureness" is actually too strong
                            if (resolvedMethod.exists(m ⇒ propertyStore(m, Purity.key) == Pure)) {
                                issue = "the return value of the call of "+invoke.declaringClass.toJava+
                                    "{ "+
                                    invoke.methodDescriptor.toJava(invoke.name)+
                                    " } is not used"
                                relevance = Relevance.OfUtmostRelevance
                            }
                        } catch {
                            case ct: ControlThrowable ⇒ throw ct
                            case t: Throwable ⇒
                                OPALLogger.error(
                                    "error",
                                    "assessing analysis result failed; ignoring issue",
                                    t
                                )(theProject.logContext)
                        }

                    case ACONST_NULL.opcode |
                        ICONST_0.opcode |
                        ICONST_M1.opcode |
                        LCONST_0.opcode |
                        FCONST_0.opcode |
                        DCONST_0.opcode ⇒
                        val nextPC = code.pcOfNextInstruction(vo)
                        instructions(nextPC) match {
                            case StoreLocalVariableInstruction((_, index)) ⇒
                                // The typical pattern generated by a compiler if the
                                // value is used to set "some initial value" is that
                                // after pushing the constant value on the stack, the
                                // value is immediately stored in a register...
                                //
                                // final int i = 0
                                // if (x == ...) i = j*1; else i = abc();
                                //
                                
                                val lvOption = code.localVariable(nextPC, index)
                                if (lvOption.isDefined && (
                                    lvOption.get.startPC < vo || lvOption.get.startPC > nextPC
                                )) {
                                    issue = s"the constant value ${instruction.toString(vo)} is not used"
                                    relevance = Relevance.Low
                                }
                            // else... we filter basically all issues unless we are sure that this is real; i.e.,
                            //  - it is not a default value
                            //  - it it not a final local variable 
                           
                            case _ ⇒
                                issue = "the constant value "+
                                    instruction.toString(vo)+
                                    "is (most likely) used to initialize a local variable"
                                relevance = Relevance.TechnicalArtifact
                        }
                    
                    case BIPUSH.opcode | SIPUSH.opcode | 
                    ICONST_1.opcode | ICONST_2.opcode | ICONST_3.opcode | ICONST_4.opcode | ICONST_5.opcode | 
                    LCONST_1.opcode | 
                    DCONST_1.opcode |
                    FCONST_1.opcode | FCONST_2.opcode |
                    LDC.opcode | LDC_W.opcode | LDC2_W.opcode =>
                        
                    

                    case IINC.opcode ⇒
                        issue = "the incremented value is not used"
                        relevance = Relevance.DefaultRelevance

                    case _ ⇒
                        issue = "the value of the expression "+
                            instruction.toString(vo)+
                            " is not used"
                        relevance = Relevance.VeryHigh
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
