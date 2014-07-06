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
package project

import scala.collection.{ Set, Map }

import org.opalj.collection.mutable.UShortSet
import org.opalj.collection.UID

import br._
import br.analyses.SomeProject

/**
 * Builds a call graph by first collecting all call graph edges before the final
 * `CallGraph` is created.
 *
 * ==Thread Safety==
 * This class is '''not thread-safe'''.
 *
 * ==Usage==
 * This class is used internally by the methods of the `CallGraphFactory` to build
 * the call graph. That class/those methods takes care of all thread-safety issues.
 *
 * @author Michael Eichberg
 */
class CallGraphBuilder(val project: SomeProject) {

    type PCs = collection.mutable.UShortSet

    private[this] var allCallEdges = List.empty[(Method, Map[PC, Set[Method]])]

    /**
     * Adds the given `callEdges` to the call graph.
     *
     * If `callEdges` contains another edge for a previously added `(Method,PC)` pair
     * then this edge will be added to the potential targets for the respective
     * invoke instruction (referred to by the `(Method,PC)` pair).
     */
    def addCallEdges(callEdges: (Method, Map[PC, Set[Method]])): Unit = {
        if (callEdges._2.nonEmpty) {
            allCallEdges = callEdges :: allCallEdges
        }
    }

    /**
     * Builds the final call graph.
     */
    def buildCallGraph(): CallGraph = {

        import concurrent._
        import concurrent.duration._
        import ExecutionContext.Implicits.global

        import scala.collection.mutable.{ OpenHashMap, AnyRefMap, WrappedArray }

        val calledByMapFuture: Future[AnyRefMap[Method, AnyRefMap[Method, PCs]]] = Future {
            val calledByMap: AnyRefMap[Method, AnyRefMap[Method, PCs]] =
                new AnyRefMap[Method, AnyRefMap[Method, PCs]](project.methodsCount)
            for {
                (caller, edges) ← allCallEdges
                (pc, callees) ← edges
                callee ← callees
            } {
                val callers =
                    calledByMap.getOrElseUpdate(
                        callee,
                        new AnyRefMap[Method, PCs](8)
                    )
                callers.get(caller) match {
                    case Some(pcs) ⇒
                        val newPCs = pc +≈: pcs
                        if (pcs ne newPCs)
                            callers.update(caller, newPCs)
                    case None ⇒
                        val newPCs = UShortSet(pc)
                        callers.put(caller, newPCs)
                }
                // USING AN IMMUTABLE MAP - ROUGHLY 5% SLOWER AND 10% MEMORY OVERHEAD
                // val callers = calledByMap(callee.id)
                // if (callers eq null) {
                //  calledByMap(callee.id) = new Map.Map1(caller, UShortSet(pc))
                // } else {
                //  callers.get(caller) match {
                //      case Some(pcs) ⇒
                //          val newPCs = pcs + pc
                //          if (pcs ne newPCs)
                //              calledByMap(callee.id) = callers.updated(caller, newPCs)
                //      case None ⇒
                //          val newPCs = UShortSet(pc)
                //          calledByMap(callee.id) = callers.updated(caller, newPCs)
                //      }
                // }
            }
            calledByMap
        }

        val callsMap: AnyRefMap[Method, OpenHashMap[PC, Iterable[Method]]] =
            new AnyRefMap[Method, OpenHashMap[PC, Iterable[Method]]](project.methodsCount)

        for {
            (caller, edges) ← allCallEdges
            (pc, callees) ← edges
            if callees.nonEmpty
        } {
            val callSite =
                callsMap.getOrElseUpdate(
                    caller,
                    new OpenHashMap[PC, Iterable[Method]](8)
                )
            if (callSite.contains(pc)) {
                callSite.update(
                    pc,
                    new WrappedArray.ofRef((callees ++ callSite(pc)).toArray))
            } else
                callSite.put(
                    pc,
                    new WrappedArray.ofRef(callees.toArray))
        }

        new CallGraph(
            project,
            Await.result(calledByMapFuture, Duration.Inf),
            callsMap)
    }
}
