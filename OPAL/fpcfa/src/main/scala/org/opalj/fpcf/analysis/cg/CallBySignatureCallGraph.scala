/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package fpcf
package analysis
package cg

import org.opalj.br._
import org.opalj.br.analyses.SomeProject

import scala.collection.Map

/**
 * Basic representation of a (calculated) call graph.
 *
 * ==Terminology==
 * A method that calls another method is referred to as the `caller`. The method
 * that is called is called the `callee`. Hence, a caller calls a callee.
 *
 * ==Thread Safety==
 * The call graph is effectively immutable and can be accessed by multiple
 * threads concurrently.
 * Calls will never block.
 *
 * ==Call Graph Construction==
 * The call graph is constructed by the [[CallGraphFactory]].
 *
 * @param calledByMap The map of all methods that are called by at least one method.
 *      I.e., the value is never the empty map.
 * @param callsMap The map of all methods that call at least one method.
 *      I.e., the value is never an empty map.
 * @author Michael Eichberg
 */
class CallBySignatureCallGraph private[cg] (
        project:                   SomeProject,
        calledByMap: Map[Method, Map[Method, PCs]],
        callsMap:    Map[Method, Map[PC, Iterable[Method]]],
        cbsCount:    Int
) extends org.opalj.ai.analyses.cg.CallGraph(project, calledByMap, callsMap){

    def callBySignatureCount: Int = cbsCount
}
