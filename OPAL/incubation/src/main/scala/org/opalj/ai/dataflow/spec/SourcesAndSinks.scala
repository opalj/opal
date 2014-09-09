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
package spec

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
trait SourcesAndSinks {

    //
    // Storing/Managing Sources
    //
    private[this] var sourceMatchers: List[AValueLocationMatcher] = Nil
    def sources(vlm: AValueLocationMatcher): Unit = sourceMatchers = vlm :: sourceMatchers

    private[this] var theSourceValues: Map[Method, Set[ValueOrigin]] = _
    def sourceValues: Map[Method, Set[ValueOrigin]] = theSourceValues

    def sources(
        filter: Function[ClassFile, Boolean],
        matcher: PartialFunction[Method, Set[ValueOrigin]]): Unit = {

        sourceMatchers =
            new AValueLocationMatcher {

                def apply(project: SomeProject) = {
                    var map = scala.collection.mutable.AnyRefMap.empty[Method, Set[ValueOrigin]]
                    for {
                        classFile ← project.classFiles
                        if filter(classFile)
                        method @ MethodWithBody(_) ← classFile.methods
                    } {
                        if (matcher.isDefinedAt(method)) {
                            val matchedValues = matcher(method)
                            if (matchedValues.nonEmpty)
                                map.update(method, matchedValues)
                        }
                    }
                    map.repack
                    map
                }
            } :: sourceMatchers
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

    protected[this] def initializeSourcesAndSinks(project: SomeProject): Unit = {
        import scala.collection.immutable.HashMap

        val sources = sourceMatchers map ((m: AValueLocationMatcher) ⇒ m(project))
        this.theSourceValues = sources.foldLeft(HashMap.empty[Method, Set[ValueOrigin]])(_ ++ _)

        val sinks = sinkMatchers map ((m: AValueLocationMatcher) ⇒ m(project))
        this.theSinkInstructions = sinks.foldLeft(HashMap.empty[Method, Set[PC]])(_ ++ _)
    }
}

