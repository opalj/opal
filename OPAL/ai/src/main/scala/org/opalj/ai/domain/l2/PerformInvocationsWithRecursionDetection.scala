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
package domain
package l2

import org.opalj.br.Method
import org.opalj.br.ClassFile

/**
 * Enables to perform invocations.
 *
 * ==Example==
 * (PerformInvocationsWithRecursionDetection is in particular used by BugPicker's domain.)
 *
 * @author Michael Eichberg
 */
trait PerformInvocationsWithRecursionDetection extends PerformInvocations {
    callingDomain: ValuesFactory with ReferenceValuesDomain with ClassHierarchy with Configuration with TheProject with TheCode ⇒

    /**
     * The `CalledMethodsStore`, stores (method,concrete parameters) pairs to
     * make it possible to detect (and prevent) recursive calls.
     *
     * The `CalledMethodsStore` does not store the results.
     */
    val calledMethodsStore: CalledMethodsStore

    /**
     * @inheritdoc
     *
     * @return The result of calling [[CalledMethodsStore.isRecursive]].
     */
    def isRecursive(classFile: ClassFile, method: Method, operands: Operands): Boolean =
        calledMethodsStore.isRecursive(classFile, method, operands)

    trait InvokeExecutionHandler extends super.InvokeExecutionHandler {

        override val domain: Domain with MethodCallResults with PerformInvocationsWithRecursionDetection {

            /*
             * A reference to the calling domain's called method store.
             */
            // We are using the type system to ensure that all instances use the
            // _same_ CalledMethodsStore
            val calledMethodsStore: callingDomain.calledMethodsStore.type
        }

    }
}

