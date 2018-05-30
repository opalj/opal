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
package fpcf
package properties

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.log.OPALLogger
import org.opalj.log.Warn

import scala.collection.Set

sealed trait CallGraphPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = CallGraph
}

class CallGraph(
        val callees: Map[Method, Map[Int /*PC*/ , Set[Method]]],
        val callers: Map[Method, Set[(Method, Int /*pc*/ )]]
) extends Property with CallGraphPropertyMetaInformation {
    final def key: PropertyKey[CallGraph] = CallGraph.key
}

object CallGraph extends CallGraphPropertyMetaInformation {

    /**
     * Computes a CHA like call graph
     */
    def fallbackCG(p: SomeProject): CallGraph = {
        //TODO parallelize. The current prototype impl. should be incredibly slow!
        var callers = Map.empty[Method, Set[(Method, Int /*PC*/ )]].withDefaultValue(Set.empty)
        val callees = p.allMethods.map { m ⇒
            m.body match {
                case Some(code) ⇒
                    val callees = code.instructions.zipWithIndex.collect {
                        case (instr, pc) ⇒
                            val tgts = instr match {
                                case call: INVOKESTATIC ⇒
                                    p.staticCall(call).toSet
                                case call: INVOKESPECIAL ⇒
                                    p.specialCall(call).toSet
                                case call: INVOKEVIRTUAL ⇒
                                    p.virtualCall(m.classFile.thisType.packageName, call)
                                case call: INVOKEINTERFACE ⇒
                                    p.interfaceCall(call)
                                case _: INVOKEDYNAMIC ⇒
                                    OPALLogger.logOnce(
                                        Warn(
                                            "analysis",
                                            "unresolved invokedynamics are not handled. please use appropriate reading configuration"
                                        )
                                    )(p.logContext)
                                    Set.empty[Method]
                            }
                            for (tgt ← tgts) {
                                // TODO make updates nicer
                                callers = callers.updated(tgt, callers(tgt) + (m → pc))
                            }
                            pc → tgts
                    }.toMap
                    m → callees
                case None ⇒
                    m → Map.empty[Int /*PC*/ , Set[Method]]
            }
        }.toMap
        new CallGraph(callees, callers)
    }

    final val key: PropertyKey[CallGraph] = {
        PropertyKey.create[SomeProject, CallGraph](
            name = "Callees",
            (ps: PropertyStore, p: SomeProject) ⇒ {
                fallbackCG(p)
            },
            (_, eps: EPS[SomeProject, CallGraph]) ⇒ eps.toUBEP
        )
    }
}
