/*
 * License (BSD Style License):
 * Copyright (c) 2012
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Software Technology Group or Technische
 *   Universität Darmstadt nor the names of its contributors may be used to
 *   endorse or promote products derived from this software without specific
 *   prior written permission.
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
package de.tud.cs.st.bat
package resolved
package ai

/**
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de), Dennis Siebert
 */
class TypeDomain extends Domain {

    //
    // CREATE ARRAY
    //
    def newarray(count: Value, componentType: FieldType): Value = TypedValue(ArrayType(componentType))
    def multianewarray(counts: List[Value], componentType: FieldType) = {
        TypedValue(ArrayType(counts.length, componentType))
    }

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(index: Value, arrayref: Value): Value = {
        arrayref match {
            case TypedValue(ArrayType(componentType)) ⇒ TypedValue(componentType)
            case _                                    ⇒ ComputationalTypeValue(ComputationalTypeReference)
        }
    }
    def aastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def baload(index: Value, arrayref: Value): Value = { // byte or boolean load...
        arrayref match {
            case TypedValue(ArrayType(componentType)) ⇒ TypedValue(componentType)
            case _                                    ⇒ ComputationalTypeValue(ComputationalTypeReference)
        }
    }
    def bastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def caload(index: Value, arrayref: Value): Value = TypedValue.CharValue
    def castore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def daload(index: Value, arrayref: Value): Value = TypedValue.DoubleValue
    def dastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def faload(index: Value, arrayref: Value): Value = TypedValue.FloatValue
    def fastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def iaload(index: Value, arrayref: Value): Value = TypedValue.IntegerValue
    def iastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def laload(index: Value, arrayref: Value): Value = TypedValue.LongValue
    def lastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def saload(index: Value, arrayref: Value): Value = TypedValue.ShortValue
    def sastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }

    //
    // LENGTH OF AN ARRAY
    //
    def arraylength(value: Value): Value = TypedValue.IntegerValue

    //
    // PUSH CONSTANT VALUE
    //
    def byteValue(value: Int) = TypedValue.ByteValue
    def shortValue(value: Int) = TypedValue.ShortValue
    def intValue(value: Int) = TypedValue.IntegerValue
    def longValue(vlaue: Long) = TypedValue.LongValue
    def floatValue(value: Float) = TypedValue.FloatValue
    def doubleValue(value: Double) = TypedValue.DoubleValue
    def stringValue(value: String) = TypedValue.StringValue
    def classValue(t: ReferenceType) = TypedValue.ClassValue

    //
    // TYPE CHECKS AND CONVERSION
    //

    def checkcast(value: Value, resolvedType: ReferenceType) = TypedValue(resolvedType)
    def instanceof(value: Value, resolvedType: ReferenceType) = TypedValue(resolvedType)

    def d2f(value: Value): Value = TypedValue.DoubleValue
    def d2i(value: Value): Value = TypedValue.IntegerValue
    def d2l(value: Value): Value = TypedValue.LongValue

    def f2d(value: Value): Value = TypedValue.DoubleValue
    def f2i(value: Value): Value = TypedValue.IntegerValue
    def f2l(value: Value): Value = TypedValue.LongValue

    def i2b(value: Value): Value = TypedValue.ByteValue
    def i2c(value: Value): Value = TypedValue.CharValue
    def i2d(value: Value): Value = TypedValue.DoubleValue
    def i2f(value: Value): Value = TypedValue.FloatValue
    def i2l(value: Value): Value = TypedValue.LongValue
    def i2s(value: Value): Value = TypedValue.ShortValue

    def l2d(value: Value): Value = TypedValue.DoubleValue
    def l2f(value: Value): Value = TypedValue.FloatValue
    def l2i(value: Value): Value = TypedValue.IntegerValue

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
            case NullValue ⇒ TypedValue(InstructionExceptions.NullPointerException)
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
    def invokeinterface(declaringClass: ReferenceType, name: String, methodDescriptor: MethodDescriptor, objectref: Value, params: List[Value]) = handleInvoke(methodDescriptor)
    def invokevirtual(declaringClass: ReferenceType, name: String, methodDescriptor: MethodDescriptor, objectref: Value, params: List[Value]) = handleInvoke(methodDescriptor)
    def invokespecial(declaringClass: ReferenceType, name: String, methodDescriptor: MethodDescriptor, objectref: Value, params: List[Value]) = handleInvoke(methodDescriptor)
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
    def fcmpg(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def fcmpl(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def dcmpg(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def dcmpl(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def lcmp(value1: Value, value2: Value): Value = TypedValue.IntegerValue

    //
    // UNARY EXPRESSIONS
    //
    def dneg(value: Value) = TypedValue.DoubleValue
    def fneg(value: Value) = TypedValue.FloatValue
    def ineg(value: Value) = TypedValue.IntegerValue
    def lneg(value: Value) = TypedValue.LongValue

    //
    // BINARY EXPRESSIONS
    //
    def dadd(value1: Value, value2: Value): Value = TypedValue.DoubleValue
    def ddiv(value1: Value, value2: Value): Value = TypedValue.DoubleValue
    def dmul(value1: Value, value2: Value): Value = TypedValue.DoubleValue
    def drem(value1: Value, value2: Value): Value = TypedValue.DoubleValue
    def dsub(value1: Value, value2: Value): Value = TypedValue.DoubleValue

    def fadd(value1: Value, value2: Value): Value = TypedValue.FloatValue
    def fdiv(value1: Value, value2: Value): Value = TypedValue.FloatValue
    def fmul(value1: Value, value2: Value): Value = TypedValue.FloatValue
    def frem(value1: Value, value2: Value): Value = TypedValue.FloatValue
    def fsub(value1: Value, value2: Value): Value = TypedValue.FloatValue

    def iadd(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def iand(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def idiv(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def imul(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def ior(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def irem(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def ishl(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def ishr(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def isub(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def iushr(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def ixor(value1: Value, value2: Value): Value = TypedValue.IntegerValue

    def ladd(value1: Value, value2: Value): Value = TypedValue.LongValue
    def land(value1: Value, value2: Value): Value = TypedValue.LongValue
    def ldiv(value1: Value, value2: Value): Value = TypedValue.LongValue
    def lmul(value1: Value, value2: Value): Value = TypedValue.LongValue
    def lor(value1: Value, value2: Value): Value = TypedValue.LongValue
    def lrem(value1: Value, value2: Value): Value = TypedValue.LongValue
    def lshl(value1: Value, value2: Value): Value = TypedValue.LongValue
    def lshr(value1: Value, value2: Value): Value = TypedValue.LongValue
    def lsub(value1: Value, value2: Value): Value = TypedValue.LongValue
    def lushr(value1: Value, value2: Value): Value = TypedValue.LongValue
    def lxor(value1: Value, value2: Value): Value = TypedValue.LongValue

    //
    // "OTHER" INSTRUCTIONS
    //
    def iinc(value: Value, increment: Int) = TypedValue.IntegerValue

    def monitorenter(value: Value) { /* Nothing to do. */ }
    def monitorexit(value: Value) { /* Nothing to do. */ }

    def newObject(t: ObjectType) = TypedValue(t)
}