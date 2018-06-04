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

import java.util.concurrent.ConcurrentHashMap

import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.InvocationInstruction
import org.opalj.log.OPALLogger
import org.opalj.log.Warn

import scala.collection.Set
import scala.collection.Map
import scala.collection.JavaConverters._

sealed trait CallGraphPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = CallGraph
}

/**
 * Represents a call graph for a specific [[org.opalj.br.analyses.SomeProject]].
 *
 * @param callees For each call-site (method and program counter), the set of potential call targets
 * @param callers For each method m, the set of potential call-sites (method and program counter)
 *                that have m as target.
 *
 * @author Florian Kuebler
 */
final class CallGraph(
        val callees: Map[Method, Map[Int /*PC*/ , Set[Method]]],
        val callers: Map[Method, Set[(Method, Int /*pc*/ )]]
) extends Property with CallGraphPropertyMetaInformation {
    def key: PropertyKey[CallGraph] = CallGraph.key

    /**
     * TODO
     */
    def size: Int = {
        val calleesSize = callees.map { case (_, callSites) ⇒ callSites.flatMap(_._2).size }.sum
        val callersSize = callers.map(_._2.size).sum
        assert(callersSize == calleesSize)
        calleesSize
    }

    override def toString: String = s"CallGraph(size = $size)"
}

object CallGraph extends CallGraphPropertyMetaInformation {

    /**
     * Computes a CHA like call graph.
     */
    def fallbackCG(p: SomeProject): CallGraph = {
        p.get(CHACallGraphKey)
    }

    final val key: PropertyKey[CallGraph] = {
        PropertyKey.create(
            name = "CallGraph",
            (_: PropertyStore, p: SomeProject) ⇒ {
                fallbackCG(p)
            },
            (_: PropertyStore, eps: EPS[SomeProject, CallGraph]) ⇒ eps.toUBEP
        )
    }

    def apply(
        callees: Map[Method, Map[Int /*PC*/ , Set[Method]]],
        callers: Map[Method, Set[(Method, Int /*pc*/ )]]
    ): CallGraph = new CallGraph(callees, callers)

    def unapply(
        cg: CallGraph
    ): Option[(Map[Method, Map[Int /*PC*/ , Set[Method]]], Map[Method, Set[(Method, Int /*PC*/ )]])] =
        Some(cg.callees → cg.callers)
}

/**
 * A key to computes a CHA like call graph as fallback.
 */
object CHACallGraphKey extends ProjectInformationKey[CallGraph, Nothing] {

    override protected def requirements: ProjectInformationKeys = Nil

    override protected def compute(project: SomeProject): CallGraph = {
        val callers = new ConcurrentHashMap[Method, Set[(Method, Int /*PC*/ )]]()
        val callees = new ConcurrentHashMap[Method, Map[Int /*PC*/ , Set[Method]]]

        project.parForeachMethod() { m ⇒
            m.body match {
                case Some(code) ⇒
                    // TODO use code.collect???
                    val calleesOfM = code.instructions.iterator.zipWithIndex.collect {
                        case (instr: InvocationInstruction, pc) ⇒
                            val tgts = instr match {
                                case call: INVOKESTATIC ⇒
                                    project.staticCall(call).toSet
                                case call: INVOKESPECIAL ⇒
                                    project.specialCall(call).toSet
                                case call: INVOKEVIRTUAL ⇒
                                    project.virtualCall(m.classFile.thisType.packageName, call)
                                case call: INVOKEINTERFACE ⇒
                                    project.interfaceCall(call)
                                case _: INVOKEDYNAMIC ⇒
                                    OPALLogger.logOnce(
                                        Warn(
                                            "analysis",
                                            "unresolved invokedynamics are not handled. please use appropriate reading configuration"
                                        )
                                    )(project.logContext)
                                    Set.empty[Method]
                            }
                            tgts.foreach { tgt ⇒
                                callers.compute(tgt, (x: Method, prev: Set[(Method, Int)]) ⇒ {
                                    if (prev == null)
                                        Set(m → pc)
                                    else
                                        prev + (m → pc)
                                })
                            }

                            pc → tgts
                    }.toMap

                    callees.put(m, calleesOfM)
                case None ⇒
                    callees.put(m, Map.empty)
            }
        }
        new CallGraph(callees.asScala, callers.asScala.withDefaultValue(Set.empty))
    }

}
