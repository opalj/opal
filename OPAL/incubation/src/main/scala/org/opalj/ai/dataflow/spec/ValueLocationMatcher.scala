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

trait ValueLocationMatcher extends AValueLocationMatcher {

    def apply(project: SomeProject): Map[Method, Set[PC]] // the PCs of the values that we want to track

}

case class Methods(
        properties: PartialFunction[Method, Boolean] = { case m: Method ⇒ true },
        parameters: PartialFunction[(Int, FieldType), Boolean]) extends ValueLocationMatcher {

    def apply(project: SomeProject): Map[Method, Set[PC]] = {
        import scala.collection.mutable.{ HashMap, HashSet }

        var result = HashMap.empty[Method, HashSet[PC]]
        for {
            classFile ← project.classFiles
            method @ MethodWithBody(body) ← classFile.methods
            true ← properties.lift(method)
            (parameterType, index) ← method.descriptor.parameterTypes.zipWithIndex
        } {
            val methodParameterShift = if (method.isStatic) -1 else -2
            val parameter = (index, parameterType)
            if (parameters.isDefinedAt(parameter) && parameters(parameter)) {
                result.getOrElseUpdate(method, HashSet.empty) += (-index + methodParameterShift)
            }
        }
        result
    }
}

case class Calls(
        properties: PartialFunction[(ReferenceType, String, MethodDescriptor), Boolean]) extends ValueLocationMatcher {

    def apply(project: SomeProject): Map[Method, Set[PC]] = {
        import scala.collection.mutable.{ HashMap, HashSet }

        var result = HashMap.empty[Method, HashSet[PC]]
        for {
            classFile ← project.classFiles
            method @ MethodWithBody(body) ← classFile.methods
            pc ← body collectWithIndex {
                case (pc, MethodInvocationInstruction(receiver, name, descriptor)) if properties.isDefinedAt((receiver, name, descriptor)) &&
                    properties((receiver, name, descriptor)) ⇒ pc
            }
        } {
            result.getOrElseUpdate(method, HashSet.empty) += pc
        }
        result
    }
}

