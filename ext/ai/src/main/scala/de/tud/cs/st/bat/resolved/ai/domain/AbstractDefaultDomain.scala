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

    // (BY default we ignore ArrayIndexOutOfBoundsExceptions and NegativeArraySizeExceptions.)

    //
    // CREATE ARRAY
    //
    def newarray(pc: Int, count: DomainValue, componentType: FieldType): NewArrayOrNegativeArraySizeException =
        //ComputedValueAndException(TypedValue(ArrayType(componentType)), TypedValue(ObjectType.ArithmeticException))
        ComputedValue(TypedValue(ArrayType(componentType)))

    /**
     * @note The componentType may be (again) an array type.
     */
    def multianewarray(pc: Int, counts: List[DomainValue], arrayType: ArrayType): NewArrayOrNegativeArraySizeException =
        //ComputedValueAndException(TypedValue(arrayType), TypedValue(ObjectType.ArithmeticException))
        ComputedValue(TypedValue(arrayType))

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult = {
        arrayref match {
            case TypedValue(ArrayType(componentType)) ⇒
                ComputedValue(TypedValue(componentType))
            case _ ⇒
                AIImplementationError("aaload - tracking of array type failed; array contains reference values of unknown type")
        }
    }
    def aastore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) = { ComputationWithSideEffectOnly }
    def baload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult = { // byte or boolean load...
        arrayref match {
            case TypedValue(ArrayType(componentType)) ⇒ ComputedValue(TypedValue(componentType))
            case _ ⇒
                AIImplementationError("baload - tracking of array type failed; array may contain booleans or byte values")
        }
    }
    def bastore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) = ComputationWithSideEffectOnly
    def caload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult = ComputedValue(SomeCharValue)
    def castore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) = ComputationWithSideEffectOnly
    def daload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult = ComputedValue(SomeDoubleValue)
    def dastore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) = ComputationWithSideEffectOnly
    def faload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult = ComputedValue(SomeFloatValue)
    def fastore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) = ComputationWithSideEffectOnly
    def iaload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult = ComputedValue(SomeIntegerValue)
    def iastore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) = ComputationWithSideEffectOnly
    def laload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult = ComputedValue(SomeLongValue)
    def lastore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) = ComputationWithSideEffectOnly
    def saload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult = ComputedValue(SomeShortValue)
    def sastore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) = ComputationWithSideEffectOnly

    //
    // LENGTH OF AN ARRAY
    //
    def arraylength(pc: Int, value: DomainValue): ComputationWithReturnValueOrNullPointerException =
        ComputedValue(SomeIntegerValue)

    //
    // PUSH CONSTANT VALUE
    //
    def byteValue(pc: Int, value: Int) = SomeByteValue
    def shortValue(pc: Int, value: Int) = SomeShortValue
    def intValue(pc: Int, value: Int) = SomeIntegerValue
    def longValue(pc: Int, value: Long) = SomeLongValue
    def floatValue(pc: Int, value: Float) = SomeFloatValue
    def doubleValue(pc: Int, value: Double) = SomeDoubleValue
    def stringValue(pc: Int, value: String) = AStringObject
    def classValue(pc: Int, t: ReferenceType) = AClassObject

    //
    // TYPE CHECKS AND CONVERSION
    //

    def checkcast(pc: Int, value: DomainValue, resolvedType: ReferenceType) =
        ComputedValue(TypedValue(resolvedType))

    def instanceof(pc: Int, value: DomainValue, resolvedType: ReferenceType) =
        SomeBooleanValue

    def d2f(pc: Int, value: DomainValue): DomainValue = SomeFloatValue
    def d2i(pc: Int, value: DomainValue): DomainValue = SomeIntegerValue
    def d2l(pc: Int, value: DomainValue): DomainValue = SomeLongValue

    def f2d(pc: Int, value: DomainValue): DomainValue = SomeDoubleValue
    def f2i(pc: Int, value: DomainValue): DomainValue = SomeIntegerValue
    def f2l(pc: Int, value: DomainValue): DomainValue = SomeLongValue

    def i2b(pc: Int, value: DomainValue): DomainValue = SomeByteValue
    def i2c(pc: Int, value: DomainValue): DomainValue = SomeCharValue
    def i2d(pc: Int, value: DomainValue): DomainValue = SomeDoubleValue
    def i2f(pc: Int, value: DomainValue): DomainValue = SomeFloatValue
    def i2l(pc: Int, value: DomainValue): DomainValue = SomeLongValue
    def i2s(pc: Int, value: DomainValue): DomainValue = SomeShortValue

    def l2d(pc: Int, value: DomainValue): DomainValue = SomeDoubleValue
    def l2f(pc: Int, value: DomainValue): DomainValue = SomeFloatValue
    def l2i(pc: Int, value: DomainValue): DomainValue = SomeIntegerValue

    //
    // RETURN FROM METHOD 
    // The domain is called to inform it about the values that are returned
    //
    def areturn(pc: Int, value: DomainValue) { /* Nothing to do. */ }
    def dreturn(pc: Int, value: DomainValue) { /* Nothing to do. */ }
    def freturn(pc: Int, value: DomainValue) { /* Nothing to do. */ }
    def ireturn(pc: Int, value: DomainValue) { /* Nothing to do. */ }
    def lreturn(pc: Int, value: DomainValue) { /* Nothing to do. */ }
    def returnVoid(pc: Int) { /* Nothing to do. */ }

    //
    // ACCESSING FIELDS
    //
    def getfield(pc: Int,
                 objectref: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType) =
        ComputedValue(TypedValue(fieldType))

    def getstatic(pc: Int,
                  declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType) =
        TypedValue(fieldType)

    def putfield(pc: Int,
                 objectref: DomainValue,
                 value: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType) = {
        ComputationWithSideEffectOnly
        /* Nothing to do. */
    }

    def putstatic(pc: Int,
                  value: DomainValue,
                  declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType) {
        /* Nothing to do. */
    }

    //
    // METHOD INVOCATIONS
    //
    def invokeinterface(pc: Int,
                        declaringClass: ReferenceType,
                        name: String,
                        methodDescriptor: MethodDescriptor,
                        params: List[DomainValue]) =
        ComputedValue(asTypedValue(methodDescriptor.returnType))

    def invokevirtual(pc: Int,
                      declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      params: List[DomainValue]) =
        ComputedValue(asTypedValue(methodDescriptor.returnType))

    def invokespecial(pc: Int,
                      declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      params: List[DomainValue]) =
        ComputedValue(asTypedValue(methodDescriptor.returnType))

    def invokestatic(pc: Int,
                     declaringClass: ReferenceType,
                     name: String,
                     methodDescriptor: MethodDescriptor,
                     params: List[DomainValue]) =
        ComputedValue(asTypedValue(methodDescriptor.returnType))

    private def asTypedValue(someType: Type): Option[DomainTypedValue[someType.type]] = {
        if (someType.isVoidType)
            None
        else
            Some(TypedValue(someType))
    }

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def fcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def dcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def dcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def lcmp(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue

    //
    // UNARY EXPRESSIONS
    //
    def dneg(pc: Int, value: DomainValue) = SomeDoubleValue
    def fneg(pc: Int, value: DomainValue) = SomeFloatValue
    def ineg(pc: Int, value: DomainValue) = SomeIntegerValue
    def lneg(pc: Int, value: DomainValue) = SomeLongValue

    //
    // BINARY EXPRESSIONS
    //
    def dadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def ddiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def dmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def drem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def dsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue

    def fadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def fdiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def fmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def frem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def fsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue

    def iadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def iand(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def idiv(pc: Int, value1: DomainValue, value2: DomainValue) = ComputedValue(SomeIntegerValue)
    def imul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ior(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def irem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ishl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ishr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def isub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def iushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ixor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue

    def ladd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def land(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def ldiv(pc: Int, value1: DomainValue, value2: DomainValue) = ComputedValue(SomeLongValue)
    def lmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lrem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lshl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lshr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lxor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue

    //
    // "OTHER" INSTRUCTIONS
    //
    def iinc(pc: Int, value: DomainValue, increment: Int) = SomeIntegerValue

}