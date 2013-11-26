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

import bat.resolved.analyses.{ SomeProject, Project }
import collection.Set
import collection.Map
import collection.mutable.HashMap
import bat.resolved.ai.domain._
import scala.annotation.tailrec

/**
 * Contains all information required during the computation of a call graph.
 *
 * Basic a helper data structure that encapsulates all data structures that
 * are mutated during the computation of the call graph.
 *
 * @author Michael Eichberg
 */
class CallGraphConstructionContext[Source](
        val project: Project[Source],
        val analyzeMethod: (Method) ⇒ Unit,
        val handleUnresolvedMethodCall: ( /*callerClass: */ ReferenceType, /*caller:*/ Method, /*pc:*/ PC, /*calleeClass:*/ ReferenceType, /*calleeName:*/ String, /*calleeDescriptor: */ MethodDescriptor) ⇒ _) {

    // the index is the id of the ReferenceType of the receiver
    val resolvedTargetsCache: Array[HashMap[MethodSignature, Iterable[Method]]] =
        new Array[HashMap[MethodSignature, Iterable[Method]]](project.objectTypesCount)

    /* THE CALL GRAPH */
    // the index is the id of the method that is "called by" other methods
    private[this] val calledByMap: Array[HashMap[Method, Set[PC]]] = new Array(project.methodsCount)
    // the index is the id of the method that calls other methods
    private[this] val callsMap: Array[HashMap[PC, Iterable[Method]]] = new Array(project.methodsCount)

    @inline final def addCallEdge(
        caller: Method,
        pc: PC,
        callees: Iterable[Method]): Unit = {
        import UID.getOrElseUpdate

        // calledBy: Map[Method, Map[Method, Set[PC]]]
        for (callee ← callees) {
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
                    val newPCs = collection.immutable.Set.empty + pc
                    callers.update(caller, newPCs)
            }
            if (!callee.isNative) {
                analyzeMethod(callee)
            }
        }

        // calls : Map[Method, Map[PC, Iterable[Method]]]          
        val callSites =
            getOrElseUpdate(
                callsMap,
                caller,
                HashMap.empty[PC, Iterable[Method]])
        callSites.update(pc, callees)
    }

    def buildCallGraph(): CallGraph[Source] =
        new CallGraph(project, calledByMap, callsMap)
}
