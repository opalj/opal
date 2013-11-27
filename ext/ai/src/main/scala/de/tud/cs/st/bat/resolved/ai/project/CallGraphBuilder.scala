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

import bat.resolved.analyses.Project

/**
 * Builds a call graph by first collecting all call edges before the final
 * `CallGraph` is created.
 *
 * ==Thread Safety==
 * This class is not '''thread-safe'''.
 *
 * @author Michael Eichberg
 */
class CallGraphBuilder[Source](val project: Project[Source]) {

    import collection.immutable.Set
    import collection.mutable.HashMap
    import java.util.concurrent.ConcurrentLinkedQueue

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

        // the index is the id of the method that is "called by" other methods
        val calledByMapFuture: Future[Array[HashMap[Method, Set[PC]]]] = future {
            val calledByMap: Array[HashMap[Method, Set[PC]]] = new Array(project.methodsCount)
            for {
                edges ← allCallEdges //.iterator()
                (caller, pc, callees) ← edges
                callee ← callees
            } {
                val callers =
                    getOrElseUpdate(
                        calledByMap,
                        callee,
                        HashMap.empty[Method, Set[PC]])
                callers.get(caller) match {
                    case Some(pcs) ⇒
                        val newPCs = pcs + pc
                        if (pcs ne newPCs)
                            callers.update(caller, newPCs)
                    case None ⇒
                        val newPCs = Set.empty + pc
                        callers.update(caller, newPCs)
                }
            }
            calledByMap
        }

        // the index is the id of the method that calls other methods
        val callsMap: Array[HashMap[PC, Iterable[Method]]] = new Array(project.methodsCount)
        for {
            edges ← allCallEdges
            (caller, pc, callees) ← edges
            callee ← callees
        } {
            val callSites =
                getOrElseUpdate(
                    callsMap,
                    caller,
                    HashMap.empty[PC, Iterable[Method]])
            callSites.update(pc, callees)
        }

        new CallGraph(
            project,
            Await.result(calledByMapFuture, Duration.Inf),
            callsMap)
    }
}
