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
package de.tud.cs.st
package bat
package resolved
package ai
package project

import analyses.{ SomeProject, Project }
import domain._

import scala.collection.Set
import scala.collection.Map

/**
 * Basic representation of a (calculated) call graph.
 *
 * ==Thread Safety==
 * The call graph is immutable and can be accessed by multiple threads concurrently.
 * Calls will never block.
 * 
 * ==Call Graph Construction==
 * The call graph is constructed by the [[de.tud.cs.st.bat.resolved.ai.project.CallGraphFactory]].
 *
 * @author Michael Eichberg
 */
class CallGraph private[project] (
        val project: SomeProject,
        private[this] val calledByMap: Array[_ <: Map[Method, PCs]],
        private[this] val callsMap: Array[_ <: Map[PC, Iterable[Method]]]) {

    import de.tud.cs.st.util.ControlAbstractions.foreachNonNullValueOf

    /**
     * Returns the invoke instructions (by means of (`Method`,`PC`) pairs) that
     * call the given method. If this method is not called by any other method an
     * empty map is returned.
     */
    def calledBy(method: Method): Map[Method, PCs] = {
        val callers = calledByMap(method.id)
        if (callers ne null) callers else Map.empty
    }

    /**
     * Returns the methods that are potentially invoked by the invoke instruction
     * identified by the (`method`,`pc`) pair. 
     */
    def calls(method: Method, pc: PC): Iterable[Method] = {
        val callees = callsMap(method.id)
        if (callees ne null) callees.get(pc).get else Iterable.empty
    }

    /**
     * Returns the methods that are called by the invoke instructions of the given method.
     *
     * If this method does not call any methods an empty map is returned.
     */
    // In case of the CHA Call Graph this could also be easily calculated on-demand, 
    // since we do not use any information that is not readily available.
    // However, we collect/store that information for the time being to make the 
    // implementation more uniform.
    def calls(method: Method): Map[PC, Iterable[Method]] = {
        val callees = callsMap(method.id)
        if (callees ne null) callees else Map.empty
    }

    /**
     * Calls the function `f` for each method that calls some other method.
     */
    def foreachCallingMethod[U](f: (Method, Map[PC, Iterable[Method]]) ⇒ U): Unit = {
        foreachNonNullValueOf(callsMap) { (i, callees) ⇒
            f(project.method(i), callees)
        }
    }

    /**
     * Calls the function `f` for each method that is called by some other method.
     */
    def foreachCalledByMethod[U](f: (Method, Map[Method, PCs]) ⇒ U): Unit = {
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
     * (TSV format (tab-separated file) - can easily be read by most spreadsheet
     * applications).
     */
    def callsStatistics(maxNumberOfResults: Int = 65536): String = {
        var result: List[List[String]] = List.empty
        var resultCount = 0
        project forallMethods { (method: Method) ⇒
            val callSites = calls(method)
            callSites forall { callSite ⇒
                val (pc, targets) = callSite
                result = List(
                    project.classFile(method).fqn,
                    "\""+method.toJava+"\"",
                    pc.toString,
                    targets.size.toString
                ) :: result
                resultCount += 1
                resultCount < maxNumberOfResults
            }
            resultCount < maxNumberOfResults
        }
        // add(prepend) the line with the column titles
        result =
            List("\"Class\"",
                "\"Method\"",
                "\"Callsite (PC)\"",
                "\"Targets\"") :: result
        result.map(_.mkString("\t")).mkString("\n")
    }

    /**
     * Statistics about the number of methods that potentially call a specific method.
     * (TSV format (tab-separated file) - can easily be read by most spreadsheet
     * applications).
     */
    def calledByStatistics(maxNumberOfResults: Int = 65536): String = {
        assume(maxNumberOfResults > 0)

        var result: List[List[String]] = List.empty
        var resultCount = 0
        project forallMethods { (method: Method) ⇒
            val callingSites = calledBy(method)
            callingSites forall { callingSite ⇒
                val (callerMethod, callingInstructions) = callingSite
                result =
                    List(
                        project.classFile(method).fqn,
                        method.toJava,
                        project.classFile(callerMethod).fqn,
                        callerMethod.toJava,
                        callingInstructions.size.toString
                    ) :: result
                resultCount += 1
                resultCount < maxNumberOfResults
            }
            resultCount < maxNumberOfResults
        }
        // add(prepend) the line with the column titles
        result =
            List("\"Class\"",
                "\"Method\"",
                "\"Class of calling Method\"",
                "\"Calling Method\"",
                "\"Calling Sites\"") :: result
        result.map(_.mkString("\t")).mkString("\n")
    }
}
