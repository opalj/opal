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
import org.opalj.br.MethodSignature
import org.opalj.br.ObjectType
import org.opalj.br.PCAndInstruction
import org.opalj.br.analyses.MethodIDKey
import org.opalj.br.analyses.MethodIDs
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.InvocationInstruction
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntTrieSet1
import org.opalj.log.OPALLogger
import org.opalj.log.Warn

import scala.annotation.switch
import scala.collection.Set
import scala.collection.Map
import scala.collection.JavaConverters._
import scala.collection.immutable.IntMap

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
        val callees:   Map[Method, IntMap[IntTrieSet]], //TODO
        val callers:   IntMap[Set[Long /*MethodId + PC*/ ]],
        val size:      Int,
        val methodIds: MethodIDs
) extends Property with OrderedProperty with CallGraphPropertyMetaInformation {
    def key: PropertyKey[CallGraph] = CallGraph.key

    override def toString: String = s"CallGraph(size = $size)"

    /**
     * Tests if this property is equal or better than the given one (better means that the
     * value is above the given value in the underlying lattice).
     */
    override def checkIsEqualOrBetterThan(e: Entity, other: CallGraph): Unit = {
        //TODO here compare real edges
        if (size > other.size)
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
    }

    def updated(method: Method, calleesOfM: IntMap[IntTrieSet]): CallGraph = {
        val methodId = methodIds(method)

        // add the new edges to the callee map of the call graph
        var newCallees = callees
        // todo refactor this into a readable stmt
        calleesOfM.foreach {
            case (pc, tgtsOfM) ⇒
                newCallees = newCallees.updated(
                    method,
                    newCallees.getOrElse(
                        method, IntMap.empty[IntTrieSet]
                    ).updated(
                        pc, newCallees.getOrElse(
                        method, IntMap.empty[IntTrieSet]
                    ).getOrElse(pc, IntTrieSet.empty) ++ tgtsOfM
                    )
                )
        }

        // add the new edges to the caller map of the call graph
        var newCallers = callers

        val oldCalleesOfMSize = callees.getOrElse(method, Map.empty).iterator.map(_._2.size).sum
        val calleesOfMSize = newCallees.getOrElse(method, Map.empty).iterator.map(_._2.size).sum
        assert(calleesOfMSize >= oldCalleesOfMSize)
        calleesOfM.foreach {
            case (pc, tgtsOfM) ⇒
                tgtsOfM.foreach { tgt ⇒
                    newCallers = newCallers.updated(tgt, newCallers.getOrElse(tgt, Set.empty) + CallGraph.toLong(methodId, pc))
                }
        }
        CallGraph(newCallees, newCallers, size + (calleesOfMSize - oldCalleesOfMSize), methodIds)
    }

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
            (_: PropertyStore, eps: EPS[SomeProject, CallGraph]) ⇒ eps.ub,
            (_: PropertyStore, _: SomeProject) ⇒ None
        )
    }

    def apply(
        callees:   Map[Method, IntMap[IntTrieSet]],
        callers:   IntMap[Set[Long]],
        size:      Int,
        methodIDs: MethodIDs
    ): CallGraph = new CallGraph(callees, callers, size, methodIDs)

    def unapply(
        cg: CallGraph
    ): Option[(Map[Method, IntMap[IntTrieSet]], IntMap[Set[Long]])] =
        Some(cg.callees → cg.callers)

    def toLong(methodId: Int, pc: Int): Long = {
        (methodId.toLong << 32) | (pc & 0xFFFFFFFFL)
    }
    def toMethodAndPc(methodAndPc: Long): (Int, Int) = {
        ((methodAndPc >> 32).toInt, methodAndPc.toInt)
    }

}

/**
 * A key to computes a CHA like call graph as fallback.
 */
object CHACallGraphKey extends ProjectInformationKey[CallGraph, Nothing] {

    override protected def requirements: ProjectInformationKeys = Nil

    override protected def compute(project: SomeProject): CallGraph = {
        val methodIds = project.get(MethodIDKey)

        val callees = new ConcurrentHashMap[Method, IntMap[IntTrieSet]]
        val callers = new ConcurrentHashMap[Int, Set[Long]]()

        // we need the kind as a method can be called via super and via invokeinterface with same signature and package
        val staticCallCache = new CallGraphCache[MethodSignature, IntTrieSet](project)
        val specialCallCache = new CallGraphCache[MethodSignature, IntTrieSet](project)
        val interfaceCallCache = new CallGraphCache[MethodSignature, IntTrieSet](project)
        val virtualCallCache = new CallGraphCache[(String, MethodSignature), IntTrieSet](project)

        project.parForeachMethod() { m ⇒
            val methodId = methodIds(m)
            m.body match {
                case Some(code) ⇒
                    var calleesOfM = IntMap.empty[IntTrieSet]
                    code.collectWithIndex {
                        case PCAndInstruction(pc, instr: InvocationInstruction) ⇒
                            val tgts = (instr.opcode: @switch) match {

                                case INVOKESTATIC.opcode ⇒
                                    val call = instr.asInstanceOf[INVOKESTATIC]
                                    val methodSignature = MethodSignature(call.name, call.methodDescriptor)
                                    staticCallCache.getOrElseUpdate(
                                        call.declaringClass, methodSignature
                                    ) {
                                        project.staticCall(call) match {
                                            case Success(tgt) ⇒ IntTrieSet1(methodIds(tgt))
                                            case _            ⇒ IntTrieSet.empty
                                        }
                                    }

                                case INVOKESPECIAL.opcode ⇒
                                    val call = instr.asInstanceOf[INVOKESPECIAL]
                                    val methodSignature = MethodSignature(call.name, call.methodDescriptor)
                                    specialCallCache.getOrElseUpdate(
                                        call.declaringClass, methodSignature
                                    ) {
                                        project.specialCall(call) match {
                                            case Success(tgt) ⇒ IntTrieSet1(methodIds(tgt))
                                            case _            ⇒ IntTrieSet.empty
                                        }
                                    }

                                case INVOKEINTERFACE.opcode ⇒
                                    val call = instr.asInstanceOf[INVOKEINTERFACE]
                                    val methodSignature = MethodSignature(call.name, call.methodDescriptor)
                                    interfaceCallCache.getOrElseUpdate(
                                        call.declaringClass, methodSignature
                                    ) {
                                        project.interfaceCall(
                                            call
                                        ).foldLeft(IntTrieSet.empty) { (r, tgt) ⇒
                                            r + methodIds(tgt)
                                        }
                                    }

                                case INVOKEVIRTUAL.opcode ⇒
                                    val call = instr.asInstanceOf[INVOKEVIRTUAL]
                                    val methodSignature = MethodSignature(call.name, call.methodDescriptor)
                                    /*val samePackage = call.declaringClass.isObjectType &&
                                        m.classFile.thisType.packageName ==
                                        call.declaringClass.asObjectType.packageName*/

                                    val objType =
                                        if (call.declaringClass.isArrayType)
                                            ObjectType.Object
                                        else
                                            call.declaringClass.asObjectType

                                    virtualCallCache.getOrElseUpdate(
                                        objType, (m.classFile.thisType.packageName, methodSignature)
                                    ) {
                                        project.virtualCall(
                                            m.classFile.thisType.packageName, call
                                        ).foldLeft(IntTrieSet.empty) { (r, tgt) ⇒
                                            r + methodIds(tgt)
                                        }
                                    }

                                case INVOKEDYNAMIC.opcode ⇒
                                    OPALLogger.logOnce(
                                        Warn(
                                            "analysis",
                                            "unresolved invokedynamics are not handled. please use appropriate reading configuration"
                                        )
                                    )(project.logContext)
                                    IntTrieSet.empty
                            }

                            tgts.foreach { tgt ⇒
                                callers.compute(tgt, (_: Int, prev: Set[Long]) ⇒ {
                                    val methodAndPC: Long = CallGraph.toLong(methodId, pc)
                                    if (prev == null)
                                        Set(methodAndPC)
                                    else
                                        prev + methodAndPC
                                })
                            }

                            calleesOfM += (pc → tgts)
                    }

                    callees.put(m, calleesOfM)
                case None ⇒
                    callees.put(m, IntMap.empty)
            }
        }

        println("computed fallback cg")

        val size = callers.values().iterator().asScala.map(_.size).sum

        new CallGraph(
            callees.asScala,
            IntMap[Set[Long]](callers.asScala.toSeq: _*),
            size,
            methodIds
        )
    }

}
