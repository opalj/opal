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
import scala.util.control.ControlThrowable
import org.opalj.log.OPALLogger
import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.br.LocalVariable
import org.opalj.br.instructions.ICONST_M1
import org.opalj.br.instructions.IINC
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
import org.opalj.br.instructions.NEW

/**
 * Identifies unused local variables
 *
 * @author Michael Eichberg
 */
object CollectionsUsage {

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
        var issues = List.empty[StandardIssue]
        val domain = result.domain
        val code = domain.code
        val instructions = code.instructions
        code.foreach { (pc, instruction) ⇒
            instruction match {

                case INVOKESTATIC(Collections, "unmodifiableCollection", unmodifiableCollectionMethodDescriptor) ⇒
                    val origins = domain.operandOrigin(pc, 0)
                    if (origins.size == 1 && instructions(origins.head).opcode == NEW.opcode) {
                        // there is just one path on which the value is initialized
                        val usages = domain.usedBy(origins.head)
                        if (usages.size == 2) {
                            // one for the call of the initializer and for the call to Coll...
                            instructions(usages.filter { _ != pc }.head) match {

                                // TODO Support the matching of other constructors... (e.g., which take a size hint)
                                case INVOKESPECIAL(_, _, MethodDescriptor.NoArgsAndReturnVoid) ⇒
                                    issues ::= StandardIssue(
                                        "CollectionsUsage",
                                        theProject, classFile, Some(method), Some(pc),
                                        None,
                                        None,
                                        "directly use Collections.emptyList/Collections.emptySet",
                                        None,
                                        Set(IssueCategory.Comprehensibility, IssueCategory.Performance),
                                        Set(IssueKind.JavaCollectionAPIUsage),
                                        Seq((origins.head, "useless creation of collection")),
                                        Relevance.DefaultRelevance
                                    )

                                case _ ⇒ // other constructors are ignored
                            }

                        } else if (usages.size == 3) {
                            var foundConstructorCall = false
                            var foundAddCall = false
                            val previousUsages = usages.filter { _ != pc }
                            previousUsages.foreach { pc ⇒
                                instructions(pc) match {

                                    // TODO Support the matching of other constructors... (e.g., which take a size hint)
                                    case INVOKESPECIAL(_, _, MethodDescriptor.NoArgsAndReturnVoid) ⇒
                                        foundConstructorCall = true

                                    case INVOKEVIRTUAL(_, "add", MethodDescriptor(IndexedSeq(ObjectType.Object), _)) |
                                        INVOKEINTERFACE(_, "add", MethodDescriptor(IndexedSeq(ObjectType.Object), _)) ⇒
                                        // is it the receiver or the parameter (in relation to a different collection?
                                        if (domain.operandOrigin(pc, 1) == origins) {
                                            foundAddCall = true
                                        }

                                    case i ⇒ // other calls are ignored
                                        println("let's see"+i)
                                }
                            }
                            if (foundAddCall && foundConstructorCall) {
                                issues ::= StandardIssue(
                                    "CollectionsUsage",
                                    theProject, classFile, Some(method), Some(pc),
                                    None,
                                    None,
                                    "directly use Collections.singletonList/Collections.singletonSet",
                                    None,
                                    Set(IssueCategory.Comprehensibility, IssueCategory.Performance),
                                    Set(IssueKind.JavaCollectionAPIUsage),
                                    Seq((origins.head, "useless creation of collection")),
                                    Relevance.DefaultRelevance
                                )
                            }
                        }
                    }
                case _ ⇒ // don't care
            }
        }

        issues
    }

    final val Collection = ObjectType("java/util/Collection")
    final val unmodifiableCollectionMethodDescriptor = MethodDescriptor(Collection, Collection)
    final val Collections = ObjectType("java/util/Collections")
}
