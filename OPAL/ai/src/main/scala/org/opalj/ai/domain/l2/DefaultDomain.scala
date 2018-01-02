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
package domain
package l2

import org.opalj.collection.immutable.Chain
import org.opalj.br.Method
import org.opalj.br.analyses.Project

class DefaultDomain[Source](
        project:                            Project[Source],
        method:                             Method,
        val frequentEvaluationWarningLevel: Int,
        val maxCallChainLength:             Int
) extends SharedDefaultDomain[Source](project, method)
    with PerformInvocationsWithRecursionDetection
    with RecordCFG
    with TheMemoryLayout {
    callingDomain ⇒

    type CalledMethodDomain = ChildDefaultDomain[Source]

    def this(project: Project[Source], method: Method) {
        this(project, method, 256, 2)
    }

    final val coordinatingDomain = new CoordinatingValuesDomain(project)

    def calledMethodAI = BaseAI

    def calledMethodDomain(method: Method) = {
        new ChildDefaultDomain(project, method, callingDomain, maxCallChainLength - 1)
    }

    def shouldInvocationBePerformed(method: Method): Boolean = {
        maxCallChainLength > 0 && !method.returnType.isVoidType
    }

    // (HERE)
    // It has to be lazy, because we need the "MemoryLayout" which is set by the
    // abstract interpreter before the first evaluation of an instruction (and,
    // hence, before the first usage of the CalledMethodsStore by the AI)
    lazy val calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type } = {
        val operands =
            localsArray(0).foldLeft(Chain.empty[DomainValue])((l, n) ⇒
                if (n ne null) n :&: l else l)
        CalledMethodsStore(
            coordinatingDomain, callingDomain.frequentEvaluationWarningLevel
        )(
            method, mapOperands(operands, coordinatingDomain)
        )
    }

}

class ChildDefaultDomain[Source](
        project:                Project[Source],
        method:                 Method,
        val callerDomain:       PerformInvocationsWithRecursionDetection { type CalledMethodDomain = ChildDefaultDomain[Source] },
        val maxCallChainLength: Int
) extends SharedDefaultDomain[Source](project, method)
    with ChildPerformInvocationsWithRecursionDetection
    with DefaultRecordMethodCallResults { callingDomain ⇒

    type CalledMethodDomain = callerDomain.CalledMethodDomain

    final def calledMethodAI: AI[_ >: CalledMethodDomain] = callerDomain.calledMethodAI

    def shouldInvocationBePerformed(method: Method): Boolean = {
        maxCallChainLength > 0 && !method.returnType.isVoidType
    }

    def calledMethodDomain(method: Method) = {
        new ChildDefaultDomain(project, method, callingDomain, maxCallChainLength - 1)
    }

}
