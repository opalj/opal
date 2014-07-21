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
package l0

import org.opalj.util.{ Answer, Yes, No, Unknown }

import org.opalj.br.{ Type, ObjectType, ReferenceType }
import org.opalj.br.MethodDescriptor
import org.opalj.br.BootstrapMethod
import org.opalj.br.analyses.{ Project, ClassHierarchy }

/**
 * Most basic handling of method invocations that determines the value that is
 * put onto the operand stack/returned by a method call based on the called method's
 * signature.
 *
 * (Linkage related exceptions are currently generally ignored.)
 *
 * @author Michael Eichberg
 */
trait TypeLevelInvokeInstructions extends MethodCallsDomain {
    domain: CoreDomain with ReferenceValuesDomain with TypedValuesFactory with Configuration with TheCode ⇒

    protected[this] def getExceptions(pc: PC): Set[ExceptionValue] = {
        var exceptionTypes: Set[ExceptionValue] = Set.empty
        code.handlersFor(pc) foreach { h ⇒
            exceptionTypes +=
                (h.catchType match {
                    // We don't know the true type of the exception, we just
                    // know the upper bound!
                    case None     ⇒ NonNullObjectValue(pc, ObjectType.Throwable)
                    case Some(ex) ⇒ NonNullObjectValue(pc, ex)
                })
        }
        exceptionTypes
    }

    protected[this] def handleInstanceBasedInvoke(
        pc: PC,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {
        val exceptions = refIsNull(operands.last) match {
            case Yes ⇒
                return justThrows(NullPointerException(pc))
            case Unknown if throwNullPointerExceptionOnMethodCall ⇒
                getExceptions(pc) + NullPointerException(pc)
            case /*No or Unknown & DoNotThrowNullPointerException*/ _ ⇒
                getExceptions(pc)
        }
        val returnType = methodDescriptor.returnType
        handleInvoke(pc, returnType, exceptions)
    }

    protected[this] def handleInvoke(
        pc: PC,
        returnType: Type,
        exceptions: Set[ExceptionValue]): MethodCallResult = {
        if (returnType.isVoidType) {
            if (exceptions.isEmpty)
                ComputationWithSideEffectOnly
            else
                ComputationWithSideEffectOrException(exceptions)
        } else {
            if (exceptions.isEmpty)
                ComputedValue(TypedValue(pc, returnType))
            else
                ComputedValueOrException(TypedValue(pc, returnType), exceptions)
        }
    }

    /*override*/ def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult =
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)

    /*override*/ def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult =
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)

    /*override*/ def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult =
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)

    /*override*/ def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {
        val returnType = methodDescriptor.returnType
        handleInvoke(pc, returnType, getExceptions(pc))
    }

    /*override*/ def invokedynamic(
        pc: PC,
        bootstrapMethod: BootstrapMethod,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): Computation[DomainValue, ExceptionValues] = {
        val returnType = methodDescriptor.returnType
        handleInvoke(pc, returnType, getExceptions(pc))
    }
}

