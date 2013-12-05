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
package l1

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

import analyses.{ Project, ClassHierarchy }

/**
 *
 * @author Michael Eichberg
 */
trait TypeLevelInvokeInstructionsWithNullPointerHandling { this: SomeDomain ⇒

    import ObjectType._

    protected[this] def ReturnValue(pc: PC, someType: Type): Option[DomainValue] = {
        if (someType.isVoidType)
            None
        else
            Some(TypedValue(pc, someType))
    }

    protected def handleInstanceBasedInvoke(
        pc: PC,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult =
        isNull(operands.last) match {
            case Yes ⇒
                ThrowsException(Set(InitializedObject(pc, NullPointerException)))
            case No ⇒
                ComputedValue(ReturnValue(pc, methodDescriptor.returnType))
            case Unknown ⇒
                ComputedValueAndException(
                    ReturnValue(pc, methodDescriptor.returnType),
                    Set(InitializedObject(pc, NullPointerException)))
        }

    override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult =
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)

    override def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult =
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)

    override def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult =
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)

    override def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult =
        ComputedValue(ReturnValue(pc, methodDescriptor.returnType))

    override def invokedynamic(
        pc: PC,
        bootstrapMethod: BootstrapMethod,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): Computation[DomainValue, ExceptionValues] =
        ComputedValue(TypedValue(pc, ObjectType.Object))
}

