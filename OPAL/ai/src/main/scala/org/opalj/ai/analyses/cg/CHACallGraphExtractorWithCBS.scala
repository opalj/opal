/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package analyses
package cg

import scala.collection.Set

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodSignature
import org.opalj.br.ObjectType
import org.opalj.br.analyses.IntStatisticsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.analyses.CallBySignatureResolutionKey

/**
 * Domain object that can be used to calculate a call graph using CHA. This domain
 * basically collects – for all invoke instructions of a method – the potential target
 * methods that may be invoked at runtime.
 *
 * Virtual calls on Arrays (clone(), toString(),...) are replaced by calls to the
 * respective methods of `java.lang.Object`.
 *
 * Signature polymorphic methods are correctly resolved (done by the method
 * `lookupImplementingMethod` defined in `ClassHierarchy`.)
 *
 * @author Michael Eichberg
 * @author Michael Reif
 */
class CHACallGraphExtractorWithCBS(
        cache: CallGraphCache[MethodSignature, Set[Method]]
) extends CHACallGraphExtractor(cache) {

    protected[this] class AnalysisContext(method: Method)(implicit project: SomeProject)
        extends super.AnalysisContext(method) {

        final val cbsIndex = project.get(CallBySignatureResolutionKey)

        private[AnalysisContext] def callBySignature(
            declaringClassType: ObjectType,
            name:               String,
            descriptor:         MethodDescriptor
        ): Set[Method] = {
            cbsIndex.findMethods(name, descriptor, declaringClassType)
        }

        val statistics = project.get(IntStatisticsKey)

        override def interfaceCall(
            caller:             Method,
            pc:                 PC,
            declaringClassType: ObjectType,
            name:               String,
            descriptor:         MethodDescriptor
        ): Unit = {

            addCallToNullPointerExceptionConstructor(method, pc)

            val cbsCallees = callBySignature(declaringClassType, name, descriptor)
            val callees = this.callees(caller, declaringClassType, isInterface = true, name, descriptor)

            assert(
                (callees & cbsCallees).isEmpty,
                s"CHACallGraphExtractor: call by signature calls for $name on ${declaringClassType.toJava} \n\n"+
                    s"${cbsCallees.map { _.classFile.thisType.toJava }.mkString(", ")}}\n\n"+
                    s"are not disjunct with normal callees: ${callees.map { _.classFile.thisType.toJava }.mkString(", ")}}\n\n common:"+
                    (callees & cbsCallees).map { m ⇒ m.toJava }.mkString("\n")
            )

            val allCallees = cbsCallees ++ callees
            if (callees.isEmpty) {
                addUnresolvedMethodCall(method, pc, declaringClassType, name, descriptor)
            } else {
                addCallEdge(pc, allCallees)
            }
        }
    }

}
