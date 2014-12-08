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
package project

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.ai.ValuesFactory
import org.opalj.ai.IsAReferenceValue
import org.opalj.ai.domain.MethodCallsHandling
import org.opalj.ai.domain.Configuration
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.TheCode

/**
 *
 * @author Michael Eichberg
 */
trait MethodCallsDomainWithMethodLockup extends MethodCallsHandling with Callees {
    callingDomain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheCode ⇒

    protected[this] def doInvoke(
        pc: PC,
        method: Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult

    /**
     * Currently, if we have multiple targets, `fallback` is called and that result is
     * returned.
     */
    protected[this] def doVirtualInvoke(
        pc: PC,
        declaringClassType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {

        val receiver = operands.last.asInstanceOf[IsAReferenceValue]
        if (receiver.isPrecise &&
            receiver.upperTypeBound.consistsOfOneElement &&
            receiver.upperTypeBound.first.isObjectType) {
            doNonVirtualInvoke(
                pc,
                receiver.upperTypeBound.first.asObjectType,
                methodName, methodDescriptor, operands, fallback)
        } else
            fallback()

    }

    protected[this] def doNonVirtualInvoke(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {

        classHierarchy.resolveMethodReference(
            // the cast is safe since arrays do not have any static/special methods
            declaringClass.asObjectType,
            methodName,
            methodDescriptor,
            project) match {
                case Some(method) ⇒
                    if (method.body.isDefined)
                        doInvoke(pc, method, operands, fallback)
                    else
                        fallback()
                case _ ⇒
                    println(
                        Console.YELLOW+"[warn] method reference cannot be resolved: "+
                            declaringClass.toJava+
                            "{ static "+methodDescriptor.toJava(methodName)+"}"+Console.RESET)
                    fallback()
            }
    }

    // -----------------------------------------------------------------------------------
    //
    // Implementation of the invoke instructions
    //
    // -----------------------------------------------------------------------------------

    abstract override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            baseInvokevirtual(pc, declaringClass, methodName, methodDescriptor, operands)

        if (declaringClass.isArrayType)
            fallback()
        else
            doVirtualInvoke(
                pc, declaringClass.asObjectType, methodName, methodDescriptor, operands, fallback
            )
    }

    def baseInvokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult =
        super.invokevirtual(pc, declaringClass, name, methodDescriptor, operands)

    abstract override def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            baseInvokeinterface(pc, declaringClass, methodName, methodDescriptor, operands)

        doVirtualInvoke(
            pc, declaringClass, methodName, methodDescriptor, operands, fallback
        )
    }

    def baseInvokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult =
        super.invokeinterface(pc, declaringClass, name, methodDescriptor, operands)

    abstract override def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            baseInvokespecial(pc, declaringClass, methodName, methodDescriptor, operands)

        doNonVirtualInvoke(
            pc, declaringClass, methodName, methodDescriptor, operands, fallback
        )
    }

    def baseInvokespecial(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {
        super.invokespecial(pc, declaringClass, name, methodDescriptor, operands)
    }

    abstract override def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            baseInvokestatic(pc, declaringClass, methodName, methodDescriptor, operands)

        doNonVirtualInvoke(
            pc, declaringClass, methodName, methodDescriptor, operands, fallback
        )
    }

    /**
     * Handle those `invokestatic` calls for which we have no concrete method (e.g.,
     * the respective class file was never loaded or the method is native) or
     * if have a recursive invocation.
     */
    protected[this] def baseInvokestatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {
        super.invokestatic(pc, declaringClass, name, methodDescriptor, operands)
    }

}

