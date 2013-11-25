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

// internally, we use mutable data structures (Arrays, mutable Maps) due to their 
// better performance characteristics, but they are not exposed to the outside
import collection.mutable.HashMap

import bat.resolved.ai.domain._

/**
 * Basic representation of a call graph.
 *
 * @author Michael Eichberg
 */
class CHACallGraph[Source] private[project] (
        val project: Project[Source],
        private[this] val calledByMap: Array[_ <: Map[Method, Set[PC]]],
        private[this] val callsMap: Array[_ <: Map[PC, Iterable[Method]]]) {

    import de.tud.cs.st.util.ControlAbstractions.foreachNonNullValueOf

    /**
     * Returns the invoke instructions (by means of a `Method`/`PC` pairs) that
     * call the given method.
     */
    def calledBy(method: Method): Option[Map[Method, Set[PC]]] = {
        Option(calledByMap(method.id))
    }

    /**
     * Returns the methods that are called by an invoke instruction of the given method.
     */
    // In case of the CHA Call Graph this could also be easily calculated on-demand, 
    // since we do not use any information that is not readily available.
    def calls(method: Method): Option[Map[PC, Iterable[Method]]] = {
        Option(callsMap(method.id))
    }

    def foreachCallingMethod[U](f: (Method, Map[PC, Iterable[Method]]) ⇒ U): Unit = {
        foreachNonNullValueOf(callsMap) { (i, callees) ⇒
            f(project.method(i), callees)
        }
    }

    def foreachCalledByMethod[U](f: (Method, Map[Method, Set[PC]]) ⇒ U): Unit = {
        foreachNonNullValueOf(calledByMap) { (i, callers) ⇒
            f(project.method(i), callers)
        }
    }

    /** Number of methods that call at least one other method. */
    def callsCount: Int = {
        var callsCount = 0
        foreachNonNullValueOf(callsMap) { (e, i) ⇒ callsCount += 1 }
        callsCount
    }

    /** Number of methods that are called by at least one other method. */
    def calledByCount: Int = {
        var calledByCount = 0
        foreachNonNullValueOf(calledByMap) { (e, i) ⇒ calledByCount += 1 }
        calledByCount
    }

    /**
     * Statistics about the number of potential targets per call site.
     * (TSV format (tab-separated file) - can easily read by most spreadsheet
     * applications).
     *
     */
    def callsStatistics(maxNumberOfResults: Int = 65536): String = {
        var result: List[List[String]] = List.empty
        var resultCount = 0
        project foreachMethod { (method: Method) ⇒
            if (resultCount < maxNumberOfResults)
                calls(method) foreach { callSites ⇒
                    if (resultCount < maxNumberOfResults)
                        callSites foreach { callSite ⇒
                            val (pc, targets) = callSite
                            result = List(
                                method.id.toString,
                                "\""+method.toJava+"\"",
                                pc.toString,
                                targets.size.toString
                            ) :: result
                            resultCount += 1
                        }
                }
        }
        result = List("\"Method ID\"", "\"Method Signature\"", "\"Callsite (PC)\"", "\"Targets\"") :: result
        result.map(_.mkString("\t")).mkString("\n")
    }

    /**
     * Statistics about the number of methods that potentially call a specific method.
     * (TSV format (tab-separated file) - can easily read by most spreadsheet
     * applications).
     */
    def calledByStatistics(maxNumberOfResults: Int = 65536): String = {
        var result: List[List[String]] = List.empty
        var resultCount = 0
        project foreachMethod { (method: Method) ⇒
            if (resultCount < maxNumberOfResults)
                calledBy(method) foreach { callingSites ⇒
                    if (resultCount < maxNumberOfResults)
                        callingSites foreach { callingSite ⇒
                            val (callerMethod, callingInstructions) = callingSite
                            result =
                                List(
                                    method.id.toString,
                                    method.toJava,
                                    callerMethod.id.toString,
                                    callerMethod.toJava,
                                    callingInstructions.size.toString
                                ) :: result
                            resultCount += 1
                        }
                }
        }
        result = List("Method ID", "Method Signature", "Calling Method ID", "Calling Method", "Calling Sites") :: result
        result.map(_.mkString("\t")).mkString("\n")
    }
}