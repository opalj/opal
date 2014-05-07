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
package dataflow

import de.tud.cs.st.bat.resolved._
import de.tud.cs.st.bat.resolved.analyses._
import de.tud.cs.st.bat.resolved.ai._
import de.tud.cs.st.bat.resolved.ai.domain._
import de.tud.cs.st.bat.resolved.ai.domain.l0._
import de.tud.cs.st.bat.resolved.instructions._
import de.tud.cs.st.bat.AccessFlagsMatcher

import scala.collection.{ Map, Set }

/**
 * Solve a data-flow problem. I.e., tries to find paths from the identified sources
 * to the identified sinks.
 *
 * ==Usage==
 *
 *  1. Set the project
 *  1. Initialize [[sourceValues ]] and [[sinkInstructions]] (These methods needs to be
 *     overridden by your subclass.)
 *  1. Call [[solve]]. After you have called [[solve]] you are no longer allowed
 *      to change the project or the sources and sinks.
 *
 *
 * @author Michael Eichberg and Ben Hermann
 */
trait DataFlowProblem {

    /**
     * The project that we are analyzing.
     */
    def project: SomeProject

    /**
     * Identifies the values that we want to track (by means of the PC) per
     * relevant method.
     *
     * **The returned map must not change, after solve was called!**
     *
     * @note The methods have to belong to the [[project]].
     *
     * @see [[DataFlowConstraintSpecificationSupprt]] for the easy creation
     *      of the `sourcesValues` map.
     */
    def sourceValues: Map[Method, Set[PC]]

    /**
     * Identifies the program counters (PCs) of those instructions
     * that are sinks.
     *
     * **The returned map must not change, after solve was called!**
     *
     * @note The methods have to belong to the [[project]].
     *
     * @see [[DataFlowConstraintSpecificationSupprt]] for the easy creation
     *      of the `sinkInstructions` map.
     */
    def sinkInstructions: Map[Method, Set[PC]]

    protected[this] def analyzeFeasability() {
        val sourceValuesCount = sourceValues.values.map(pcs ⇒ pcs.size).sum
        if (project.methodsCount / 10 < sourceValuesCount) {
            Console.out.println(
                "[info] The analysis will take long; the number of source values to analyze is: "+
                    sourceValuesCount+
                    ".")
        }
    }

    /**
     * Tries to find paths from the sources to the sinks.
     */
    def solve() : String = {
        analyzeFeasability()

        "Solved :-)"
    }
}


