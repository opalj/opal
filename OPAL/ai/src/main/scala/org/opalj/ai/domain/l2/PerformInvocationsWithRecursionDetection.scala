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

import org.opalj.br.Method

/**
 * Enables to perform invocations.
 *
 * ==Example==
 * (PerformInvocationsWithRecursionDetection is in particular used by BugPicker's domain.)
 *
 * @author Michael Eichberg
 */
trait PerformInvocationsWithRecursionDetection extends PerformInvocations with TheMemoryLayout {
    callingDomain: ValuesFactory with ReferenceValuesDomain with TheProject with TheMethod with Configuration ⇒

    override type CalledMethodDomain <: TargetDomain with ChildPerformInvocationsWithRecursionDetection with MethodCallResults

    val coordinatingDomain: CalledMethodsStore.BaseDomain

    def frequentEvaluationWarningLevel: Int

    def calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type }

    // The childCalledMethodsStore is valid for one invocation only and is set by
    // doInvoke...
    private[l2] var childCalledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type } = null

    override protected[this] def doInvoke(
        pc:       Int,
        method:   Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult
    ): MethodCallResult = {

        callingDomain.calledMethodsStore.testOrElseUpdated(method, operands) match {
            case Some(newCalledMethodsStore) ⇒
                childCalledMethodsStore = newCalledMethodsStore
                super.doInvoke(pc, method, operands, fallback)
            case None ⇒
                fallback();
        }
    }
}

trait ChildPerformInvocationsWithRecursionDetection extends PerformInvocationsWithRecursionDetection {
    callingDomain: ValuesFactory with ReferenceValuesDomain with TheClassHierarchy with Configuration with TheProject with TheMethod ⇒

    val callerDomain: PerformInvocationsWithRecursionDetection

    final override val coordinatingDomain: callerDomain.coordinatingDomain.type = {
        callerDomain.coordinatingDomain
    }

    def frequentEvaluationWarningLevel: Int = callerDomain.frequentEvaluationWarningLevel

    final def calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type } = {
        callerDomain.childCalledMethodsStore
    }

}
