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
package la

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.ai.ValuesFactory
import org.opalj.ai.IsAReferenceValue
import org.opalj.ai.analyses.MethodReturnValueInformation
import org.opalj.ai.analyses.cg.MethodCallsDomainWithMethodLockup

/**
 *
 * @author Michael Eichberg
 */
trait RefinedTypeLevelInvokeInstructions extends MethodCallsDomainWithMethodLockup {
    callingDomain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheCode ⇒

    val methodReturnValueInformation: MethodReturnValueInformation

    protected[this] def doInvoke(
        pc: PC,
        method: Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {

        val returnValue =
            methodReturnValueInformation.getOrElse(method, {
                return fallback();
            })

        if (returnValue.isDefined) {
            val adaptedReturnValue = returnValue.get.adapt(this, pc)
            // println(s"${method.toJava()} returning refined value $adaptedReturnValue (returntye: ${method.returnType}; original value ${returnValue.get})")
            MethodCallResult(adaptedReturnValue, getPotentialExceptions(pc))
        } else {
            // the method always throws an exception... but we don't know which one
            val potentialExceptions = getPotentialExceptions(pc)
            ThrowsException(potentialExceptions)
        }
    }

}

