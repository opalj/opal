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

import org.opalj.br._
import org.opalj.ai.analyses.cg.Callees

/**
 * Mix in this trait if methods that are called by `invokeXYZ` instructions should
 * actually be interpreted using a custom domain.
 *
 * @author Michael Eichberg
 */
trait PerformInvocationsWithBasicVirtualMethodCallResolution
        extends PerformInvocations
        with Callees {
    callingDomain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheCode ⇒

    /**
     * The default implementation only supports the case where we can precisely
     * resolve the target.
     */
    override def doInvokeVirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {

        def handleVirtualInvokeFallback() = {
            val receiver = operands(descriptor.parametersCount)

            typeOfValue(receiver) match {
                case refValue: IsAReferenceValue if (
                    refValue.isNull.isNo && // TODO handle the case if the value maybe null
                    refValue.upperTypeBound.hasOneElement &&
                    refValue.upperTypeBound.head.isObjectType) ⇒
                    val methods =
                        callees(refValue.upperTypeBound.head.asObjectType, name, descriptor)
                    if (methods.size == 1) {
                        val method = methods.head
                        testAndDoInvoke(
                            pc,
                            project.classFile(method), method,
                            operands,
                            fallback)
                    } else
                        fallback();

                case _ ⇒
                    fallback();
            }
        }

        super.doInvokeVirtual(
            pc,
            declaringClass, name, descriptor,
            operands,
            handleVirtualInvokeFallback)

    }

}

