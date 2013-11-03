/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai
package domain

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

import analyses.{ Project, ClassHierarchy }
import de.tud.cs.st.bat.resolved.ai.IsReferenceType

/**
 * Most basic handling of method invocations that determines the value that is
 * put onto the operand stack/returned by a method call based on the called method's
 * signature. This implementation completely ignores exceptions and/or errors (e.g.,
 * `NullPointerException`s or exceptions thrown by the method).
 *
 * (Linkage related exceptions are currently generally ignored.)
 *
 * @note By ignoring potentially thrown exceptions it may be the case that not all
 *      possible paths in a program are explored and the overall analysis may not be
 *      sound.
 *
 * @author Michael Eichberg
 */
trait TypeLevelInvokeInstructions { this: Domain[_] ⇒

    import ObjectType._

    protected def asTypedValue(pc: PC, someType: Type): Option[DomainValue] = {
        if (someType.isVoidType)
            None
        else
            Some(newTypedValue(pc, someType))
    }

    override def invokedynamic(
        pc: PC,
        bootstrapMethod: BootstrapMethod,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): Computation[DomainValue, Iterable[DomainValue]] =
        ComputedValue(newTypedValue(pc, ObjectType.Object))

    override def invokeinterface(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

    override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

    override def invokespecial(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

    override def invokestatic(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

}

