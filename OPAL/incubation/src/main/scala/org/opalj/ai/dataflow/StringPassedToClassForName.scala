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

import bi.AccessFlagsMatcher

import br._
import br.instructions._
import br.analyses._

import domain._
import domain.l0._

import scala.collection.{ Map, Set }

/**
 * Searches for strings that are passed to `Class.forName(_)` calls.
 *
 * @author Michael Eichberg and Ben Hermann
 */
class StringPassedToClassForName(val project: SomeProject) extends DataFlowProblemSpecification {

    sources(Methods(
        properties = { case Method(AccessFlagsMatcher.NOT_PRIVATE(), _, md) ⇒ md.parametersCount >= 1 },
        parameters = { case (_ /*ID*/ , ObjectType.String) ⇒ true }
    ))
    //
    //    origins {
    //        case method @ Method(AccessFlagsMatcher.NOT_PRIVATE(), _, md) ⇒
    //            md.selectParameter(_ == ObjectType.String).map(parameterToValueIndex(method, _))
    //    }

    sinks(Calls(
        { case (ObjectType.Class, "forName", SingleArgumentMethodDescriptor((ObjectType.String, ObjectType.Object))) ⇒ true }
    ))
}

object StringPassedToClassForName extends DataFlowProblemRunner(
    (project: SomeProject, parameters: Seq[String]) ⇒ new StringPassedToClassForName(project)
)