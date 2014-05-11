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
package l1

import org.opalj.util.No

import br._

/**
 * Enables the tracing of concrete Class values and can, e.g., be used to resolve
 * groovy's invokedynamic constructor calls.
 *
 * This class overrides `invokestatic` and only delegates to the default implementation
 * if it cannot successfully handle the call. Hence, this trait needs to be mixed in after
 * the trait that handles the default case but before all other traits that "just"
 * analyze invokestatic calls.
 * {{{
 * class MyDomain
 *  extends DefaultTypeLevelInvokeInstructions
 *  with ClassValues
 *  with <DOES ANAYLZE INVOKE CALLS>
 * }}}
 *
 * @author Michael Eichberg (fixes for multi-parameter Class.forName(...) calls)
 * @author Arne Lottmann
 */
trait ClassValues extends StringValues {
    domain: Configuration with IntegerValuesComparison with ClassHierarchy ⇒

    type DomainClassValue <: ClassValue with DomainObjectValue

    protected class ClassValue(
        pc: Int,
        val value: Type)
            extends SObjectValue(pc, No, true, ObjectType.Class) {
        this: DomainClassValue ⇒

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {

            other match {
                case that: ClassValue if (this.value eq that.value) ⇒
                    NoUpdate
                case _ ⇒
                    val answer = super.doJoinWithNonNullValueWithSameOrigin(joinPC, other)
                    if (answer == NoUpdate) {
                        // => This class value and the other value have a corresponding
                        //    abstract representation (w.r.t. the next abstraction level!)
                        //    but we still need to drop the concrete information...
                        StructuralUpdate(ObjectValue(pc, No, true, ObjectType.Class))
                    } else {
                        answer
                    }
            }
        }

        override def adapt(target: Domain, pc: Int): target.DomainValue =
            target.ClassValue(pc, this.value)

        override def equals(other: Any): Boolean =
            other match {
                case cv: ClassValue ⇒ super.equals(other) && cv.value == this.value
                case _              ⇒ false
            }

        override protected def canEqual(other: SObjectValue): Boolean =
            other.isInstanceOf[ClassValue]

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

        import ClassValues._

        def classForName(className: String): MethodCallResult = {
            val classValue = ReferenceType(className.replace('.', '/'))
            ComputedValue(Some(ClassValue(pc, classValue)))
        }

        if ((declaringClass eq ObjectType.Class) &&
            (name == "forName") &&
            operands.nonEmpty) {

            operands.last match {
                case StringValue(value) ⇒
                    methodDescriptor match {
                        case `forName_String`                     ⇒ classForName(value)
                        case `forName_String_boolean_ClassLoader` ⇒ classForName(value)
                        case _ ⇒
                            throw new DomainException(
                                "unsupported Class { "+
                                    methodDescriptor.toJava("forName")+
                                    "}")
                    }

                case _ ⇒
                    // call default handler (the first arguement is not a string)
                    super.invokestatic(pc, declaringClass, name, methodDescriptor, operands)

            }
        } else {
            // call default handler
            super.invokestatic(pc, declaringClass, name, methodDescriptor, operands)
        }
    }
}

private object ClassValues {

    val forName_String =
        MethodDescriptor(ObjectType.String, ObjectType.Class)

    val forName_String_boolean_ClassLoader =
        MethodDescriptor(
            IndexedSeq(ObjectType.String, BooleanType, ObjectType("java/lang/ClassLoader")),
            ObjectType.Class)
}