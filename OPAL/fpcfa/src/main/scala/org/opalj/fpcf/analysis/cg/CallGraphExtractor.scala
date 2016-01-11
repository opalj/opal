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

import scala.collection.Set
import scala.collection.mutable.OpenHashMap
import scala.collection.mutable.HashSet
import scala.collection.Map
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassHierarchy
import org.opalj.br.ReferenceType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.MethodSignature
import org.opalj.ai.analyses.cg.UnresolvedMethodCall

/**
 * @author Michael Eichberg
 */
trait CallGraphExtractor { extractor ⇒

    import CallGraphExtractor.LocalCallGraphInformation

    def extract(
        project:   SomeProject,
        classFile: ClassFile,
        method:    Method
    ): LocalCallGraphInformation

    def cache: CallGraphCache[MethodSignature, Set[Method]]

    abstract protected[this] class AnalysisContext extends Callees {

        def project: SomeProject
        def classFile: ClassFile
        def method: Method

        @inline final def cache = extractor.cache

        //
        //
        // Managing/Storing Call Edges
        //
        //

        private[this] var callBySignatureCount = 0

        def callBySignatureCallEdges = callBySignatureCount

        def addCallBySignatureNumber(count: Int) = callBySignatureCount += count

        var unresolvableMethodCalls = List.empty[UnresolvedMethodCall]

        @inline def addUnresolvedMethodCall(
            callerClass: ReferenceType, caller: Method, pc: PC,
            calleeClass: ReferenceType, calleeName: String, calleeDescriptor: MethodDescriptor
        ): Unit = {
            unresolvableMethodCalls =
                new UnresolvedMethodCall(
                    callerClass, caller, pc,
                    calleeClass, calleeName, calleeDescriptor
                ) :: unresolvableMethodCalls
        }

        def allUnresolvableMethodCalls: List[UnresolvedMethodCall] = unresolvableMethodCalls

        private[this] val callEdgesMap = OpenHashMap.empty[PC, Set[Method]]

        @inline final def addCallEdge(
            pc:      PC,
            callees: Set[Method]
        ): Unit = {

            if (callEdgesMap.contains(pc)) {
                callEdgesMap(pc) ++= callees
            } else {
                callEdgesMap.put(pc, callees)
            }
        }

        def allCallEdges: (Method, Map[PC, Set[Method]]) = (method, callEdgesMap)

        def addCallToNullPointerExceptionConstructor(
            callerType: ObjectType, callerMethod: Method, pc: PC
        ): Unit = {

            cache.NullPointerExceptionDefaultConstructor match {
                case Some(defaultConstructor) ⇒
                    addCallEdge(pc, HashSet(defaultConstructor))
                case _ ⇒
                    val defaultConstructorDescriptor = MethodDescriptor.NoArgsAndReturnVoid
                    val NullPointerException = ObjectType.NullPointerException
                    addUnresolvedMethodCall(
                        callerType, callerMethod, pc,
                        NullPointerException, "<init>", defaultConstructorDescriptor
                    )
            }
        }
    }
}
object CallGraphExtractor {

    type LocalCallGraphInformation = (( /*Caller*/ Method, Map[PC, /*Callees*/ Set[Method]]), List[UnresolvedMethodCall], Int)

}

