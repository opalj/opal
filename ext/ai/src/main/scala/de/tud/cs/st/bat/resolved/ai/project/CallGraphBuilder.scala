/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai
package project

import de.tud.cs.st.collection.mutable.UShortSet

import analyses.Project

/**
 * Builds a call graph by first collecting all call graph edges before the final
 * `CallGraph` is created.
 *
 * ==Thread Safety==
 * This class is '''not thread-safe'''.
 *
 * ==Usage==
 * This class is used internally by the methods of the `CallGraphFactory` to build
 * the call graph.
 *
 * @author Michael Eichberg
 */
class CallGraphBuilder[Source](val project: Project[Source]) {

    type PCs = collection.mutable.UShortSet

    private[this] var allCallEdges = List.empty[List[(Method, PC, Iterable[Method])]]

    /**
     * Adds the given `callEdges` to the call graph.
     */
    def addCallEdges(callEdges: List[(Method, PC, Iterable[Method])]): Unit = {
        if (callEdges.nonEmpty) {
            allCallEdges = callEdges :: allCallEdges
        }
    }

    /**
     * Builds the final call graph.
     */
    def buildCallGraph(): CallGraph[Source] = {
        import UID.getOrElseUpdate

        import concurrent._
        import concurrent.duration._
        import ExecutionContext.Implicits.global

        import scala.collection.immutable.Map
        import scala.collection.mutable.HashMap

        // the index is the id of the method that is "called by" other methods
        val calledByMapFuture: Future[Array[HashMap[Method, PCs]]] = future {
            val calledByMap: Array[HashMap[Method, PCs]] = new Array(project.methodsCount)
            for {
                edges ← allCallEdges
                (caller, pc, callees) ← edges
                callee ← callees
            } {
                val callers =
                    getOrElseUpdate(
                        calledByMap,
                        callee,
                        HashMap.empty[Method, PCs])
                callers.get(caller) match {
                    case Some(pcs) ⇒
                        val newPCs = pcs + pc
                        if (pcs ne newPCs)
                            callers.update(caller, newPCs)
                    case None ⇒
                        val newPCs = UShortSet(pc)
                        callers.update(caller, newPCs)
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

        // the index is the id of the method that calls other methods
        //val callsMap: Array[Map[PC, Iterable[Method]]] = new Array(project.methodsCount)
        val callsMap: Array[Map[PC, Iterable[Method]]] = new Array(project.methodsCount)
        for {
            edges ← allCallEdges
            (caller, pc, callees) ← edges
        } {
            var callSites = callsMap(caller.id)
            if (callSites eq null) {
                callsMap(caller.id) = new Map.Map1(pc, callees)
            } else {
                callsMap(caller.id) = callSites.updated(pc, callees)
            }
        }

        new CallGraph(
            project,
            Await.result(calledByMapFuture, Duration.Inf),
            callsMap)
    }
}
