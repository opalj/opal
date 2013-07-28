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

import reflect.ClassTag

/**
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait AbstractDefaultDomain extends DomainWithValues {

    //
    // CREATE ARRAY
    //
    def newarray(count: DomainValue, componentType: FieldType): DomainValue =
        TypedValue(ArrayType(componentType))

    def multianewarray(counts: List[DomainValue], arrayType: FieldType) =
        TypedValue(arrayType)

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(index: DomainValue, arrayref: DomainValue): DomainValue = {
        arrayref match {
            case TypedValue(ArrayType(componentType)) ⇒
                TypedValue(componentType)
            case _ ⇒
                AIImplementationError("aaload - tracking of array type failed; array contains reference values of unknown type")
        }
    }
    def aastore(value: DomainValue, index: DomainValue, arrayref: DomainValue) { /* Nothing to do. */ }
    def baload(index: DomainValue, arrayref: DomainValue): DomainValue = { // byte or boolean load...
        arrayref match {
            case TypedValue(ArrayType(componentType)) ⇒ TypedValue(componentType)
            case _ ⇒
                AIImplementationError("baload - tracking of array type failed; array may contain booleans or byte values")
        }
    }
    def bastore(value: DomainValue, index: DomainValue, arrayref: DomainValue) { /* Nothing to do. */ }
    def caload(index: DomainValue, arrayref: DomainValue): DomainValue = SomeCharValue
    def castore(value: DomainValue, index: DomainValue, arrayref: DomainValue) { /* Nothing to do. */ }
    def daload(index: DomainValue, arrayref: DomainValue): DomainValue = SomeDoubleValue
    def dastore(value: DomainValue, index: DomainValue, arrayref: DomainValue) { /* Nothing to do. */ }
    def faload(index: DomainValue, arrayref: DomainValue): DomainValue = SomeFloatValue
    def fastore(value: DomainValue, index: DomainValue, arrayref: DomainValue) { /* Nothing to do. */ }
    def iaload(index: DomainValue, arrayref: DomainValue): DomainValue = SomeIntegerValue
    def iastore(value: DomainValue, index: DomainValue, arrayref: DomainValue) { /* Nothing to do. */ }
    def laload(index: DomainValue, arrayref: DomainValue): DomainValue = SomeLongValue
    def lastore(value: DomainValue, index: DomainValue, arrayref: DomainValue) { /* Nothing to do. */ }
    def saload(index: DomainValue, arrayref: DomainValue): DomainValue = SomeShortValue
    def sastore(value: DomainValue, index: DomainValue, arrayref: DomainValue) { /* Nothing to do. */ }

    //
    // LENGTH OF AN ARRAY
    //
    def arraylength(value: DomainValue): DomainValue = SomeIntegerValue

    //
    // PUSH CONSTANT VALUE
    //
    def byteValue(value: Int) = SomeByteValue
    def shortValue(value: Int) = SomeShortValue
    def intValue(value: Int) = SomeIntegerValue
    def longValue(vlaue: Long) = SomeLongValue
    def floatValue(value: Float) = SomeFloatValue
    def doubleValue(value: Double) = SomeDoubleValue
    def stringValue(value: String) = AString
    def classValue(t: ReferenceType) = AClass

    //
    // TYPE CHECKS AND CONVERSION
    //

    def checkcast(value: DomainValue, resolvedType: ReferenceType) = TypedValue(resolvedType)
    def instanceof(value: DomainValue, resolvedType: ReferenceType) = SomeBooleanValue

    def d2f(value: DomainValue): DomainValue = SomeFloatValue
    def d2i(value: DomainValue): DomainValue = SomeIntegerValue
    def d2l(value: DomainValue): DomainValue = SomeLongValue

    def f2d(value: DomainValue): DomainValue = SomeDoubleValue
    def f2i(value: DomainValue): DomainValue = SomeIntegerValue
    def f2l(value: DomainValue): DomainValue = SomeLongValue

    def i2b(value: DomainValue): DomainValue = SomeByteValue
    def i2c(value: DomainValue): DomainValue = SomeCharValue
    def i2d(value: DomainValue): DomainValue = SomeDoubleValue
    def i2f(value: DomainValue): DomainValue = SomeFloatValue
    def i2l(value: DomainValue): DomainValue = SomeLongValue
    def i2s(value: DomainValue): DomainValue = SomeShortValue

    def l2d(value: DomainValue): DomainValue = SomeDoubleValue
    def l2f(value: DomainValue): DomainValue = SomeFloatValue
    def l2i(value: DomainValue): DomainValue = SomeIntegerValue

    //
    // RETURN FROM METHOD 
    // The domain is called to inform it about the values that are returned
    //
    def areturn(value: DomainValue) { /* Nothing to do. */ }
    def dreturn(value: DomainValue) { /* Nothing to do. */ }
    def freturn(value: DomainValue) { /* Nothing to do. */ }
    def ireturn(value: DomainValue) { /* Nothing to do. */ }
    def lreturn(value: DomainValue) { /* Nothing to do. */ }
    def returnVoid() { /* Nothing to do. */ }

    //
    // ACCESSING FIELDS
    //
    def getfield(objectref: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType) =
        TypedValue(fieldType)

    def getstatic(declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType) =
        TypedValue(fieldType)

    def putfield(objectref: DomainValue,
                 value: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType) {
        /* Nothing to do. */
    }

    def putstatic(value: DomainValue,
                  declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType) {
        /* Nothing to do. */
    }

    //
    // METHOD INVOCATIONS
    //
    def invokeinterface(declaringClass: ReferenceType,
                        name: String,
                        methodDescriptor: MethodDescriptor,
                        params: List[DomainValue]): Option[DomainValue] =
        asTypedValue(methodDescriptor.returnType)

    def invokevirtual(declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      params: List[DomainValue]): Option[DomainValue] =
        asTypedValue(methodDescriptor.returnType)

    def invokespecial(declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      params: List[DomainValue]): Option[DomainValue] =
        asTypedValue(methodDescriptor.returnType)

    def invokestatic(declaringClass: ReferenceType,
                     name: String,
                     methodDescriptor: MethodDescriptor,
                     params: List[DomainValue]): Option[DomainValue] =
        asTypedValue(methodDescriptor.returnType)

    private def asTypedValue(someType: Type): Option[DomainTypedValue[someType.type]] = {
        if (someType.isVoidType)
            None
        else
            Some(TypedValue(someType))
    }

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def fcmpl(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def dcmpg(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def dcmpl(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def lcmp(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue

    //
    // UNARY EXPRESSIONS
    //
    def dneg(value: DomainValue) = SomeDoubleValue
    def fneg(value: DomainValue) = SomeFloatValue
    def ineg(value: DomainValue) = SomeIntegerValue
    def lneg(value: DomainValue) = SomeLongValue

    //
    // BINARY EXPRESSIONS
    //
    def dadd(value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def ddiv(value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def dmul(value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def drem(value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def dsub(value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue

    def fadd(value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def fdiv(value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def fmul(value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def frem(value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def fsub(value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue

    def iadd(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def iand(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def idiv(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def imul(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ior(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def irem(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ishl(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ishr(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def isub(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def iushr(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ixor(value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue

    def ladd(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def land(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def ldiv(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lmul(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lor(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lrem(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lshl(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lshr(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lsub(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lushr(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lxor(value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue

    //
    // "OTHER" INSTRUCTIONS
    //
    def iinc(value: DomainValue, increment: Int) = SomeIntegerValue

}