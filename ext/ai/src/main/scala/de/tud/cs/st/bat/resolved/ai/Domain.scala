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
 * A domain contains all information about a program's types and values.
 *
 * The AI framework controls the process of evaluating the program, but requires the
 * domain to perform the actual computations of an operations result. The minimal
 * information that every domain needs to maintain is the type of a value
 * (cf. [[de.tud.cs.st.bat.resolved.ai.Value]]).
 * The framework assumes that every method/code block is associated with its own
 * domain object.
 *
 * ==Thread Safety==
 * When every method is associated with a unique Domain instance and – given that BAT
 * only uses one thread to analyze a given method at a time – no special care has to
 * be taken. However, if a domain needs to consult a domain which is associated with
 * a Project as a whole (called in BAT "World") it is then the responsibility of the
 * domain to make sure that everything is thread safe.
 *
 * @see [[de.tud.cs.st.bat.resolved.ai.TypeDomain]]
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait Domain { 
    
    //
    // CREATE ARRAY
    //
    def newarray(count: Value, componentType: FieldType): Value
    def multianewarray(counts: List[Value], arrayType: FieldType): Value

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(index: Value, arrayref: Value): Value
    def aastore(value: Value, index: Value, arrayref: Value): Unit
    def baload(index: Value, arrayref: Value): Value
    def bastore(value: Value, index: Value, arrayref: Value): Unit
    def caload(index: Value, arrayref: Value): Value
    def castore(value: Value, index: Value, arrayref: Value): Unit
    def daload(index: Value, arrayref: Value): Value
    def dastore(value: Value, index: Value, arrayref: Value): Unit
    def faload(index: Value, arrayref: Value): Value
    def fastore(value: Value, index: Value, arrayref: Value): Unit
    def iaload(index: Value, arrayref: Value): Value
    def iastore(value: Value, index: Value, arrayref: Value): Unit
    def laload(index: Value, arrayref: Value): Value
    def lastore(value: Value, index: Value, arrayref: Value): Unit
    def saload(index: Value, arrayref: Value): Value
    def sastore(value: Value, index: Value, arrayref: Value): Unit

    //
    // LENGTH OF AN ARRAY
    //
    def arraylength(arrayref: Value): Value

    //
    // PUSH CONSTANT VALUE
    //
    def nullValue: Value = { NullValue }
    def byteValue(value: Int): Value
    def shortValue(value: Int): Value
    def intValue(value: Int): Value
    def longValue(vlaue: Long): Value
    def floatValue(value: Float): Value
    def doubleValue(value: Double): Value
    def stringValue(value: String): Value
    /**
     * @return A value that represents a runtime value of type "Class<t>"
     */
    def classValue(t: ReferenceType): Value

    //
    // TYPE CHECKS AND CONVERSION
    //

    def checkcast(objectref: Value, resolvedType: ReferenceType): Value
    def instanceof(objectref: Value, resolvedType: ReferenceType): Value

    def d2f(value: Value): Value
    def d2i(value: Value): Value
    def d2l(value: Value): Value

    def f2d(value: Value): Value
    def f2i(value: Value): Value
    def f2l(value: Value): Value

    def i2b(value: Value): Value
    def i2c(value: Value): Value
    def i2d(value: Value): Value
    def i2f(value: Value): Value
    def i2l(value: Value): Value
    def i2s(value: Value): Value

    def l2d(value: Value): Value
    def l2f(value: Value): Value
    def l2i(value: Value): Value

    //
    // RETURN FROM METHOD
    //
    def areturn(value: Value): Unit
    def dreturn(value: Value): Unit
    def freturn(value: Value): Unit
    def ireturn(value: Value): Unit
    def lreturn(value: Value): Unit
    def returnVoid(): Unit

    def athrow(objectref: Value): Value

    //
    // ACCESSING FIELDS
    //
    def getfield(objectref: Value,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType): Value
    def getstatic(declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType): Value
    def putfield(objectref: Value,
                 value: Value,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType): Unit
    def putstatic(value: Value,
                  declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType): Unit

    //
    // METHOD INVOCATIONS
    //
    // TODO [AI] Add support Java7's Invokedynamic to the Domain.
    def invokeinterface(declaringClass: ReferenceType,
                        name: String,
                        methodDescriptor: MethodDescriptor,
                        params: List[Value]): Option[Value]
    def invokevirtual(declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      params: List[Value]): Option[Value]
    def invokespecial(declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      params: List[Value]): Option[Value]
    def invokestatic(declaringClass: ReferenceType,
                     name: String,
                     methodDescriptor: MethodDescriptor,
                     params: List[Value]): Option[Value]

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(value1: Value, value2: Value): Value
    def fcmpl(value1: Value, value2: Value): Value
    def dcmpg(value1: Value, value2: Value): Value
    def dcmpl(value1: Value, value2: Value): Value
    def lcmp(value1: Value, value2: Value): Value

    //
    // UNARY EXPRESSIONS
    //
    def fneg(value: Value): Value
    def dneg(value: Value): Value
    def lneg(value: Value): Value
    def ineg(value: Value): Value

    //
    // BINARY EXPRESSIONS
    //
    def dadd(value1: Value, value2: Value): Value
    def ddiv(value1: Value, value2: Value): Value
    def dmul(value1: Value, value2: Value): Value
    def drem(value1: Value, value2: Value): Value
    def dsub(value1: Value, value2: Value): Value

    def fadd(value1: Value, value2: Value): Value
    def fdiv(value1: Value, value2: Value): Value
    def fmul(value1: Value, value2: Value): Value
    def frem(value1: Value, value2: Value): Value
    def fsub(value1: Value, value2: Value): Value

    def iadd(value1: Value, value2: Value): Value
    def iand(value1: Value, value2: Value): Value
    def idiv(value1: Value, value2: Value): Value
    def imul(value1: Value, value2: Value): Value
    def ior(value1: Value, value2: Value): Value
    def irem(value1: Value, value2: Value): Value
    def ishl(value1: Value, value2: Value): Value
    def ishr(value1: Value, value2: Value): Value
    def isub(value1: Value, value2: Value): Value
    def iushr(value1: Value, value2: Value): Value
    def ixor(value1: Value, value2: Value): Value

    def ladd(value1: Value, value2: Value): Value
    def land(value1: Value, value2: Value): Value
    def ldiv(value1: Value, value2: Value): Value
    def lmul(value1: Value, value2: Value): Value
    def lor(value1: Value, value2: Value): Value
    def lrem(value1: Value, value2: Value): Value
    def lshl(value1: Value, value2: Value): Value
    def lshr(value1: Value, value2: Value): Value
    def lsub(value1: Value, value2: Value): Value
    def lushr(value1: Value, value2: Value): Value
    def lxor(value1: Value, value2: Value): Value

    //
    // "OTHER" INSTRUCTIONS
    //

    def iinc(value: Value, increment: Int): Value

    def monitorenter(value: Value): Unit
    def monitorexit(value: Value): Unit

    def newObject(t: ObjectType): Value

    //
    // QUESTION'S ABOUT VALUES
    //
    def isNull(value: Value): BooleanAnswer

    //
    // HANDLING CONSTRAINTS
    //
    def addIsNullConstraint(
        value: Value,
        memoryLayout: MemoryLayout): MemoryLayout

    def addIsNonNullConstraint(
        value: Value,
        memoryLayout: MemoryLayout): MemoryLayout

}