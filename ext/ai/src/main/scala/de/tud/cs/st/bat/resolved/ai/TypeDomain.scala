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

/**
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
class TypeDomain extends Domain {

    //
    // CREATE ARRAY
    //
    def newarray(count: Value, componentType: FieldType): Value = TypedValue(ArrayType(componentType))
    def multianewarray(counts: List[Value], arrayType: FieldType) = {
        TypedValue(arrayType)
    }

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(index: Value, arrayref: Value): Value = {
        arrayref match {
            case TypedValue(ArrayType(componentType)) ⇒ TypedValue(componentType)
            case _                                    ⇒ CTReferenceValue
        }
    }
    def aastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def baload(index: Value, arrayref: Value): Value = { // byte or boolean load...
        arrayref match {
            case TypedValue(ArrayType(componentType)) ⇒ TypedValue(componentType)
            case _                                    ⇒ CTIntegerValue
        }
    }
    def bastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def caload(index: Value, arrayref: Value): Value = SomeCharValue
    def castore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def daload(index: Value, arrayref: Value): Value = SomeDoubleValue
    def dastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def faload(index: Value, arrayref: Value): Value = SomeFloatValue
    def fastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def iaload(index: Value, arrayref: Value): Value = SomeIntegerValue
    def iastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def laload(index: Value, arrayref: Value): Value = SomeLongValue
    def lastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def saload(index: Value, arrayref: Value): Value = SomeShortValue
    def sastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }

    //
    // LENGTH OF AN ARRAY
    //
    def arraylength(value: Value): Value = SomeIntegerValue

    //
    // PUSH CONSTANT VALUE
    //
    def byteValue(value: Int) = SomeByteValue
    def shortValue(value: Int) = SomeShortValue
    def intValue(value: Int) = SomeIntegerValue
    def longValue(vlaue: Long) = SomeLongValue
    def floatValue(value: Float) = SomeFloatValue
    def doubleValue(value: Double) = SomeDoubleValue
    def stringValue(value: String) = TypedValue.AString
    def classValue(t: ReferenceType) = TypedValue.AClass

    //
    // TYPE CHECKS AND CONVERSION
    //

    def checkcast(value: Value, resolvedType: ReferenceType) = TypedValue(resolvedType)
    def instanceof(value: Value, resolvedType: ReferenceType) = SomeBooleanValue

    def d2f(value: Value): Value = SomeFloatValue
    def d2i(value: Value): Value = SomeIntegerValue
    def d2l(value: Value): Value = SomeLongValue

    def f2d(value: Value): Value = SomeDoubleValue
    def f2i(value: Value): Value = SomeIntegerValue
    def f2l(value: Value): Value = SomeLongValue

    def i2b(value: Value): Value = SomeByteValue
    def i2c(value: Value): Value = SomeCharValue
    def i2d(value: Value): Value = SomeDoubleValue
    def i2f(value: Value): Value = SomeFloatValue
    def i2l(value: Value): Value = SomeLongValue
    def i2s(value: Value): Value = SomeShortValue

    def l2d(value: Value): Value = SomeDoubleValue
    def l2f(value: Value): Value = SomeFloatValue
    def l2i(value: Value): Value = SomeIntegerValue

    //
    // RETURN FROM METHOD
    //
    def areturn(value: Value) { /* Nothing to do. */ }
    def dreturn(value: Value) { /* Nothing to do. */ }
    def freturn(value: Value) { /* Nothing to do. */ }
    def ireturn(value: Value) { /* Nothing to do. */ }
    def lreturn(value: Value) { /* Nothing to do. */ }
    def returnVoid() { /* Nothing to do. */ }

    def athrow(value: Value): Value = {
        value match {
            case NullValue ⇒ TypedValue(ObjectType.NullPointerException)
            case _         ⇒ value
        }
    }

    //
    // ACCESSING FIELDS
    //
    def getfield(objectref: Value, declaringClass: ObjectType, name: String, fieldType: FieldType) = TypedValue(fieldType)
    def getstatic(declaringClass: ObjectType, name: String, fieldType: FieldType) = TypedValue(fieldType)
    def putfield(objectref: Value, value: Value, declaringClass: ObjectType, name: String, fieldType: FieldType) { /* Nothing to do. */ }
    def putstatic(value: Value, declaringClass: ObjectType, name: String, fieldType: FieldType) { /* Nothing to do. */ }

    //
    // METHOD INVOCATIONS
    //
    def invokeinterface(declaringClass: ReferenceType, name: String, methodDescriptor: MethodDescriptor, params: List[Value]) = handleInvoke(methodDescriptor)
    def invokevirtual(declaringClass: ReferenceType, name: String, methodDescriptor: MethodDescriptor, params: List[Value]) = handleInvoke(methodDescriptor)
    def invokespecial(declaringClass: ReferenceType, name: String, methodDescriptor: MethodDescriptor, params: List[Value]) = handleInvoke(methodDescriptor)
    def invokestatic(declaringClass: ReferenceType, name: String, methodDescriptor: MethodDescriptor, params: List[Value]) = handleInvoke(methodDescriptor)
    private def handleInvoke(methodDescriptor: MethodDescriptor) = {
        if (methodDescriptor.returnType.isVoidType)
            None
        else
            Some(TypedValue(methodDescriptor.returnType))
    }

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(value1: Value, value2: Value): Value = SomeIntegerValue
    def fcmpl(value1: Value, value2: Value): Value = SomeIntegerValue
    def dcmpg(value1: Value, value2: Value): Value = SomeIntegerValue
    def dcmpl(value1: Value, value2: Value): Value = SomeIntegerValue
    def lcmp(value1: Value, value2: Value): Value = SomeIntegerValue

    //
    // UNARY EXPRESSIONS
    //
    def dneg(value: Value) = SomeDoubleValue
    def fneg(value: Value) = SomeFloatValue
    def ineg(value: Value) = SomeIntegerValue
    def lneg(value: Value) = SomeLongValue

    //
    // BINARY EXPRESSIONS
    //
    def dadd(value1: Value, value2: Value): Value = SomeDoubleValue
    def ddiv(value1: Value, value2: Value): Value = SomeDoubleValue
    def dmul(value1: Value, value2: Value): Value = SomeDoubleValue
    def drem(value1: Value, value2: Value): Value = SomeDoubleValue
    def dsub(value1: Value, value2: Value): Value = SomeDoubleValue

    def fadd(value1: Value, value2: Value): Value = SomeFloatValue
    def fdiv(value1: Value, value2: Value): Value = SomeFloatValue
    def fmul(value1: Value, value2: Value): Value = SomeFloatValue
    def frem(value1: Value, value2: Value): Value = SomeFloatValue
    def fsub(value1: Value, value2: Value): Value = SomeFloatValue

    def iadd(value1: Value, value2: Value): Value = SomeIntegerValue
    def iand(value1: Value, value2: Value): Value = SomeIntegerValue
    def idiv(value1: Value, value2: Value): Value = SomeIntegerValue
    def imul(value1: Value, value2: Value): Value = SomeIntegerValue
    def ior(value1: Value, value2: Value): Value = SomeIntegerValue
    def irem(value1: Value, value2: Value): Value = SomeIntegerValue
    def ishl(value1: Value, value2: Value): Value = SomeIntegerValue
    def ishr(value1: Value, value2: Value): Value = SomeIntegerValue
    def isub(value1: Value, value2: Value): Value = SomeIntegerValue
    def iushr(value1: Value, value2: Value): Value = SomeIntegerValue
    def ixor(value1: Value, value2: Value): Value = SomeIntegerValue

    def ladd(value1: Value, value2: Value): Value = SomeLongValue
    def land(value1: Value, value2: Value): Value = SomeLongValue
    def ldiv(value1: Value, value2: Value): Value = SomeLongValue
    def lmul(value1: Value, value2: Value): Value = SomeLongValue
    def lor(value1: Value, value2: Value): Value = SomeLongValue
    def lrem(value1: Value, value2: Value): Value = SomeLongValue
    def lshl(value1: Value, value2: Value): Value = SomeLongValue
    def lshr(value1: Value, value2: Value): Value = SomeLongValue
    def lsub(value1: Value, value2: Value): Value = SomeLongValue
    def lushr(value1: Value, value2: Value): Value = SomeLongValue
    def lxor(value1: Value, value2: Value): Value = SomeLongValue

    //
    // "OTHER" INSTRUCTIONS
    //
    def iinc(value: Value, increment: Int) = SomeIntegerValue

    def monitorenter(value: Value) { /* Nothing to do. */ }
    def monitorexit(value: Value) { /* Nothing to do. */ }

    def newObject(t: ObjectType) = TypedValue(t)

    //
    // QUESTION'S ABOUT VALUES
    //
    def isNull(value: Value): BooleanAnswer = {
        if (value == NullValue)
            return BooleanAnswer.YES // TODO should we refines this?
        else
            return BooleanAnswer.UNKNOWN
    }

    //
    // HANDLING CONSTRAINTS
    //
    def addIsNullConstraint(value: Value, memoryLayout: MemoryLayout): MemoryLayout = {
        memoryLayout
    }
    def addIsNonNullConstraint(value: Value, memoryLayout: MemoryLayout): MemoryLayout = {
        memoryLayout
    }
}