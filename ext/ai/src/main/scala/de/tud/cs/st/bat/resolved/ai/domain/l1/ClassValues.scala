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

import de.tud.cs.st.util.No

/**
 * Enables the tracing of concrete Class values and can, e.g., be used to resolve
 * groovy's invokedynamic constructor calls.
 *
 * This class overrides invokestatic and, in certain cases, delegates to the superclass
 * implementation. Therefore, in a mixin hierarchy, ClassValues needs to come after a type
 * that implements invokestatic.
 *
 * @author Arne Lottmann
 */
trait ClassValues[+I] extends StringValues[I] {
    domain: Configuration with IntegerValuesComparison with ClassHierarchy ⇒

    type DomainClassValue <: AClassValue with DomainObjectValue

    class AClassValue(
        pc: Int,
        val value: Type)
            extends SObjectValue(pc, No, true, ObjectType.Class) {

        this: DomainObjectValue ⇒

        override def adapt[TDI >: I](target: Domain[TDI], pc: Int): target.DomainValue =
            target.ClassValue(pc, this.value)

        override def equals(other: Any): Boolean = other match {
            case cv: DomainClassValue ⇒ super.equals(other) && cv.value == this.value
            case _                    ⇒ false
        }

        override protected def canEqual(other: SObjectValue): Boolean = other.isInstanceOf[AClassValue]

        override def hashCode: Int = super.hashCode + 71 * value.hashCode

        override def toString(): String = "Class(pc="+pc+", value=\""+value.toJava+"\")"
    }

    // Needs to be implemented since the default implementation does not make sense here
    override def ClassValue(pc: Int, value: Type): DomainObjectValue

    abstract override def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {

        handleClassForNameCalls(pc, declaringClass, name, methodDescriptor, operands) match {
            case Some(result: MethodCallResult) ⇒ result
            case None                           ⇒ 
            	super.invokestatic(pc, declaringClass, name, methodDescriptor, operands)
        }
    }

    private def handleClassForNameCalls(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): Option[MethodCallResult] = {

        if (isJavaLangClassForName(declaringClass, name, methodDescriptor)) {
            val StringValue(head) = operands.head
            val classValue = ReferenceType(head.replace('.', '/'))
            Some(ComputedValue(Some(ClassValue(pc, classValue))))
        } else {
            None
        }
    }

    private def isJavaLangClassForName(
        declaringClass: Type,
        name: String,
        methodDescriptor: MethodDescriptor): Boolean = {

        (declaringClass eq ObjectType.Class) && (name == "forName") && (methodDescriptor.returnType eq ObjectType.Class)
    }
}