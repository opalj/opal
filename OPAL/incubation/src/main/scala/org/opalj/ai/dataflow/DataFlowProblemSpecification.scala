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

import scala.collection.{ Map, Set }

import bi.AccessFlagsMatcher

import br._
import br.analyses._
import br.instructions._

import domain._
import domain.l0._

/**
 * Support methods to facilitate the definition of data-flow constraints.
 *
 * @author Michael Eichberg and Ben Hermann
 */
trait DataFlowProblemSpecification extends DataFlowProblem {

    def parameterToValueIndex(method: Method, parameterIndex: Int): Int = {
        val methodParameterShift = if (method.isStatic) -1 else -2
        -parameterIndex + methodParameterShift
    }

    //
    // Storing/Managing Sources
    //
    private[this] var sourceMatchers: List[AValueLocationMatcher] = Nil
    def sources(vlm: AValueLocationMatcher): Unit = sourceMatchers = vlm :: sourceMatchers

    private[this] var theSourceValues: Map[Method, Set[PC]] = _
    def sourceValues: Map[Method, Set[PC]] = theSourceValues

    def origins(f: PartialFunction[Method, Set[Int]]): Unit = {
        sourceMatchers = MethodsMatcher(f) :: sourceMatchers
    }

    //
    // Storing/Managing Sinks
    //
    private[this] var sinkMatchers: List[AValueLocationMatcher] = Nil
    def sinks(vlm: AValueLocationMatcher): Unit = sinkMatchers = vlm :: sinkMatchers

    private[this] var theSinkInstructions: Map[Method, Set[PC]] = _
    def sinkInstructions: Map[Method, Set[PC]] = theSinkInstructions

    //
    // Instantiating the problem
    //

    def initialize(): Unit = {
        import scala.collection.immutable.HashMap

        val sources = sourceMatchers map ((m: AValueLocationMatcher) ⇒ m(project))
        this.theSourceValues = sources.foldLeft(HashMap.empty[Method, Set[PC]])(_ ++ _)

        val sinks = sinkMatchers map ((m: AValueLocationMatcher) ⇒ m(project))
        this.theSinkInstructions = sinks.foldLeft(HashMap.empty[Method, Set[PC]])(_ ++ _)
    }

    override def solve() = {
        import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }
        time {
            initialize()
        } { t ⇒
            println(f"[info] Locating the sources and sinks took ${ns2sec(t)}%.4f seconds.")
        }

        super.solve()
    }
}

case class MethodsMatcher(
        matcher: PartialFunction[Method, Set[Int]]) extends AValueLocationMatcher {

    def apply(project: SomeProject): Map[Method, Set[Int]] = {
        val map = scala.collection.mutable.AnyRefMap.empty[Method, Set[Int]]
        for {
            classFile ← project.classFiles
            method @ MethodWithBody(_) ← classFile.methods
        } {
            if (matcher.isDefinedAt(method))
                map.update(method, matcher(method))
        }
        map
    }
}


