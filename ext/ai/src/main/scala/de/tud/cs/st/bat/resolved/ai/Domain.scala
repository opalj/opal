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
 * A domain contains all information about a program's types and values and determines
 * how a domain's values are calculated.
 *
 * This trait defines the interface between the abstract interpretation framework (BATAI)
 * and some domain. I.e., all that is required by BATAI is an implementation of this
 * trait. However, several classes/traits are pre-defined to facilitate the usage of
 * BATAI.
 *
 * ==Control Flow==
 * BATI controls the process of evaluating the program, but requires a
 * domain to perform the actual computations of an instruction's result.
 *
 * The framework assumes that every method/code block is associated with its own instance
 * of a domain object.
 *
 * ==Thread Safety==
 * When every method is associated with a unique `Domain` instance as suggested and – given
 * that BAT only uses one thread to analyze a given method at a time – no special care
 * has to be taken. However, if a domain needs to consult a domain which is associated with
 * a Project as a whole, which we will refer to as "World" in BATAI, it is then the
 * responsibility of the domain to make sure that everything is thread safe.
 *
 * @see [[de.tud.cs.st.bat.resolved.ai.TypeDomain]]
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait Domain {

    /**
     * Abstracts over a concrete operand stack value or a value stored in one of the local
     * variables.
     */
    trait Value {
        /**
         * The computational type of the value.
         *
         * The precise computational type is needed by BATAI to calculate the effect
         * of generic stack manipulation instructions (e.g., `dup_...` and swap)
         * on the stack. This in turn is in particular required to calculate the
         * jump targets of RET instructions.
         *
         * '''W.r.t. the computationalType no abstraction is allowed.'''
         */
        def computationalType: ComputationalType

        /**
         * Merges this value with the given value; has to return `this` when this value
         * subsumes the given value or is structurally identical to the given
         * value; has to return an instance of
         * [[de.tud.cs.st.bat.resolved.ai.Domain.NoLegalValue]] when the two values
         * are incompatible.
         *
         * For example, merging a `DomainValue` that represents the integer value 0
         * with a `DomainValue` that represents the integer value 1 may return a new
         * `DomainValue` that precisely captures the range, that capture all positive
         * integer value or just some integer value.
         *
         * The termination of the abstract interpretation directly depends on the fact
         * that at some point all values are fixed and don't change anymore.
         */
        def merge(value: DomainValue): DomainValue

    }
    type DomainValue <: Value
    implicit val DomainValueTag: ClassTag[DomainValue]

    /**
     * Facilitates matching against values of computational type category 1.
     */
    object CTC1 {
        def unapply(v: Value): Boolean = v.computationalType.category == 1
    }
    /**
     * Facilitates matching against values of computational type category 2.
     */
    object CTC2 {
        def unapply(v: Value): Boolean = v.computationalType.category == 2
    }

    /**
     * If BATAI tries to merge two values that are incompatible the result has
     * to be an instance of `NoLegalValue`
     */
    trait NoLegalValue extends Value { this: DomainValue ⇒

        def computationalType: ComputationalType =
            BATError("a value that is not legal does not have a computational type ")

        def merge(value: DomainValue): DomainValue = this

        override def toString = "NoLegalValue"
    }

    type DomainReturnAddressValue <: ReturnAddressValue with DomainValue
    trait ReturnAddressValue extends Value {
        def addresses: Set[Int]

        final def computationalType: ComputationalType = ComputationalTypeReturnAddress

        override def toString = "ReturnAddresses: "+addresses.mkString(", ")

    }
    def ReturnAddressValue(addresses: Set[Int]): DomainReturnAddressValue
    def ReturnAddressValue(address: Int): DomainReturnAddressValue
    object ReturnAddressValue {
        def unapply(value: ReturnAddressValue): Option[Set[Int]] = Some(value.addresses)
    }

    def ReferenceValue(referenceType: ReferenceType): DomainValue
    def TypedValue(someType: Type): DomainValue

    //
    // CREATE ARRAY
    //
    def newarray(count: DomainValue, componentType: FieldType): DomainValue
    def multianewarray(counts: List[DomainValue], arrayType: FieldType): DomainValue

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(index: DomainValue, arrayref: DomainValue): DomainValue
    def aastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): Unit
    def baload(index: DomainValue, arrayref: DomainValue): DomainValue
    def bastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): Unit
    def caload(index: DomainValue, arrayref: DomainValue): DomainValue
    def castore(value: DomainValue, index: DomainValue, arrayref: DomainValue): Unit
    def daload(index: DomainValue, arrayref: DomainValue): DomainValue
    def dastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): Unit
    def faload(index: DomainValue, arrayref: DomainValue): DomainValue
    def fastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): Unit
    def iaload(index: DomainValue, arrayref: DomainValue): DomainValue
    def iastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): Unit
    def laload(index: DomainValue, arrayref: DomainValue): DomainValue
    def lastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): Unit
    def saload(index: DomainValue, arrayref: DomainValue): DomainValue
    def sastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): Unit

    //
    // LENGTH OF AN ARRAY
    //
    def arraylength(arrayref: DomainValue): DomainValue

    // 
    // PUSH CONSTANT VALUE
    //
    def nullValue: DomainValue
    def byteValue(value: Int): DomainValue
    def shortValue(value: Int): DomainValue
    def intValue(value: Int): DomainValue
    def longValue(vlaue: Long): DomainValue
    def floatValue(value: Float): DomainValue
    def doubleValue(value: Double): DomainValue
    def stringValue(value: String): DomainValue
    /**
     * @return A value that represents a runtime value of type "Class<t>"
     */
    def classValue(t: ReferenceType): DomainValue

    //
    // TYPE CHECKS AND CONVERSION
    //

    def checkcast(objectref: DomainValue, resolvedType: ReferenceType): DomainValue
    def instanceof(objectref: DomainValue, resolvedType: ReferenceType): DomainValue

    def d2f(value: DomainValue): DomainValue
    def d2i(value: DomainValue): DomainValue
    def d2l(value: DomainValue): DomainValue

    def f2d(value: DomainValue): DomainValue
    def f2i(value: DomainValue): DomainValue
    def f2l(value: DomainValue): DomainValue

    def i2b(value: DomainValue): DomainValue
    def i2c(value: DomainValue): DomainValue
    def i2d(value: DomainValue): DomainValue
    def i2f(value: DomainValue): DomainValue
    def i2l(value: DomainValue): DomainValue
    def i2s(value: DomainValue): DomainValue

    def l2d(value: DomainValue): DomainValue
    def l2f(value: DomainValue): DomainValue
    def l2i(value: DomainValue): DomainValue

    //
    // RETURN FROM METHOD
    //
    def areturn(value: DomainValue): Unit
    def dreturn(value: DomainValue): Unit
    def freturn(value: DomainValue): Unit
    def ireturn(value: DomainValue): Unit
    def lreturn(value: DomainValue): Unit
    def returnVoid(): Unit

    def athrow(objectref: DomainValue): DomainValue

    //
    // ACCESSING FIELDS
    //
    def getfield(objectref: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType): DomainValue
    def getstatic(declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType): DomainValue
    def putfield(objectref: DomainValue,
                 value: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType): Unit
    def putstatic(value: DomainValue,
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
                        params: List[DomainValue]): Option[DomainValue]
    def invokevirtual(declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      params: List[DomainValue]): Option[DomainValue]
    def invokespecial(declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      params: List[DomainValue]): Option[DomainValue]
    def invokestatic(declaringClass: ReferenceType,
                     name: String,
                     methodDescriptor: MethodDescriptor,
                     params: List[DomainValue]): Option[DomainValue]

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(value1: DomainValue, value2: DomainValue): DomainValue
    def fcmpl(value1: DomainValue, value2: DomainValue): DomainValue
    def dcmpg(value1: DomainValue, value2: DomainValue): DomainValue
    def dcmpl(value1: DomainValue, value2: DomainValue): DomainValue
    def lcmp(value1: DomainValue, value2: DomainValue): DomainValue

    //
    // UNARY EXPRESSIONS
    //
    def fneg(value: DomainValue): DomainValue
    def dneg(value: DomainValue): DomainValue
    def lneg(value: DomainValue): DomainValue
    def ineg(value: DomainValue): DomainValue

    //
    // BINARY EXPRESSIONS
    //
    def dadd(value1: DomainValue, value2: DomainValue): DomainValue
    def ddiv(value1: DomainValue, value2: DomainValue): DomainValue
    def dmul(value1: DomainValue, value2: DomainValue): DomainValue
    def drem(value1: DomainValue, value2: DomainValue): DomainValue
    def dsub(value1: DomainValue, value2: DomainValue): DomainValue

    def fadd(value1: DomainValue, value2: DomainValue): DomainValue
    def fdiv(value1: DomainValue, value2: DomainValue): DomainValue
    def fmul(value1: DomainValue, value2: DomainValue): DomainValue
    def frem(value1: DomainValue, value2: DomainValue): DomainValue
    def fsub(value1: DomainValue, value2: DomainValue): DomainValue

    def iadd(value1: DomainValue, value2: DomainValue): DomainValue
    def iand(value1: DomainValue, value2: DomainValue): DomainValue
    def idiv(value1: DomainValue, value2: DomainValue): DomainValue
    def imul(value1: DomainValue, value2: DomainValue): DomainValue
    def ior(value1: DomainValue, value2: DomainValue): DomainValue
    def irem(value1: DomainValue, value2: DomainValue): DomainValue
    def ishl(value1: DomainValue, value2: DomainValue): DomainValue
    def ishr(value1: DomainValue, value2: DomainValue): DomainValue
    def isub(value1: DomainValue, value2: DomainValue): DomainValue
    def iushr(value1: DomainValue, value2: DomainValue): DomainValue
    def ixor(value1: DomainValue, value2: DomainValue): DomainValue

    def ladd(value1: DomainValue, value2: DomainValue): DomainValue
    def land(value1: DomainValue, value2: DomainValue): DomainValue
    def ldiv(value1: DomainValue, value2: DomainValue): DomainValue
    def lmul(value1: DomainValue, value2: DomainValue): DomainValue
    def lor(value1: DomainValue, value2: DomainValue): DomainValue
    def lrem(value1: DomainValue, value2: DomainValue): DomainValue
    def lshl(value1: DomainValue, value2: DomainValue): DomainValue
    def lshr(value1: DomainValue, value2: DomainValue): DomainValue
    def lsub(value1: DomainValue, value2: DomainValue): DomainValue
    def lushr(value1: DomainValue, value2: DomainValue): DomainValue
    def lxor(value1: DomainValue, value2: DomainValue): DomainValue

    //
    // "OTHER" INSTRUCTIONS
    //

    def iinc(value: DomainValue, increment: Int): DomainValue

    def monitorenter(value: DomainValue): Unit
    def monitorexit(value: DomainValue): Unit

    def newObject(t: ObjectType): DomainValue

    //
    // QUESTION'S ABOUT VALUES
    //
    def isNull(value: DomainValue): BooleanAnswer
    def isNonNull(value: DomainValue): BooleanAnswer

    //
    // HANDLING CONSTRAINTS
    //

    /**
     * The AI framework determined that the value is guaranteed to be null.
     *
     * ==Example""
     * An `ifnull` check was performed against the given value and the AI will next analyze
     * the `true` branch – i.e., the branch that would be executed if the check succeeded.
     */
    def addIsNullConstraint(
        value: DomainValue,
        memoryLayout: MemoryLayout[this.type, this.type#DomainValue]): MemoryLayout[this.type, this.type#DomainValue]

    def addIsNonNullConstraint(
        value: DomainValue,
        memoryLayout: MemoryLayout[this.type, this.type#DomainValue]): MemoryLayout[this.type, this.type#DomainValue]

}