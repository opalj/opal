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
import collection.immutable.Range

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
 * Handling of instructions that directly manipulate the stack/the locals
 * is completely embedded into BATAI.
 *
 * The framework assumes that every method/code block is associated with its
 * own instance of a domain object.
 *
 * ==Thread Safety==
 * When every method is associated with a unique `Domain` instance as proposed and – given
 * that BATAI only uses one thread to analyze a given method at a time – no special care
 * has to be taken. However, if a domain needs to consult a domain which is associated with
 * a Project as a whole, which we will refer to as "World" in BATAI, it is then the
 * responsibility of the domain to make sure that everything is thread safe.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait Domain {

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO HANDLING VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over a concrete operand stack value or a value stored in one of the local
     * variables.
     *
     * ==Extending Value==
     * If you extend this trait, make sure that you also extend all classes/traits that
     * inherit from this type (this may require a deep mixin composition).
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
        def merge(value: DomainValue): Update[DomainValue]

        /**
         * Returns a string that states that this value and the given value are
         * structurally incompatible and, hence, merging or comparing them doesn't
         * make sense.
         */
        protected def incompatibleValues(other: DomainValue): String =
            "incompatible: "+this.toString()+" and "+other.toString()

        /**
         * Returns a string that states that merging and comparing this value with
         * the given could makes sense, but is not yet implemented.
         */
        protected def missingSupport(other: DomainValue): String =
            "the value \""+this.toString()+"\" and \""+other.toString()+"\" are "+
                "structurally comparable, but no support for comparing/merging them "+
                "is implemented (the domain implementation is probably incomplete)"
    }
    /**
     * Abstracts over the concrete type of `Value`. Needs to be refined by traits that
     * inherit from Domain and which extend `Domain`'s `Value` trait.
     */
    type DomainValue <: Value

    /**
     * The class tag
     *
     * ==Initialization==
     * In the sub-trait or class that fixes the type of `DomainValue` it is necessary
     * to implement this abstract `val`. Please note, that it has to be implemented
     * using:
     * {{{
     * val DomainValueTag : ClassTag[DomainValue] = implicitly
     * }}}
     * (As of Scala 2.10 it is necessary that you do not use `implicit` - it will
     * compile, but fail at runtime.)
     */
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
     * to be an instance of `NoLegalValue`. This may happen when BATAI tries to
     * merge two register values/locals that are not live (i.e., which should not be
     * live) and, hence, are actually allowed to contain incompatible values.
     * (`Not live` means that the value will not be used in the future.)
     */
    class NoLegalValue(val initialReason: String) extends Value { this: DomainValue ⇒

        def computationalType: ComputationalType =
            BATError(
                "the value \"NoLegalValue\" does not have a computational type "+
                    "(underlying initial reason:"+initialReason+")")

        final def merge(value: DomainValue): Update[DomainValue] = NoUpdate

        override def toString = "NoLegalValue"
    }
    object NoLegalValue {
        def unapply(value: NoLegalValue): Option[String] = Some(value.initialReason)
    }
    type DomainNoLegalValue <: NoLegalValue with DomainValue
    def NoLegalValue(initialReason: String): DomainNoLegalValue

    /**
     * Represents a set of concrete values that store return addresses (i.e., a program
     * counter/index into the code array).
     */
    trait ReturnAddressValue extends Value {
        def addresses: Set[Int]

        final def computationalType: ComputationalType = ComputationalTypeReturnAddress

        override def toString = "ReturnAddresses: "+addresses.mkString(", ")

    }
    /**
     * Abstracts over the concrete type of `ReturnAddressValue`. Needs to be fixed
     * by some sub-trait /sub-class. In the simplest case (i.e., when neither the
     * `Value` trait nor the `ReturnAddressValue` trait is refined it is sufficient
     * to write:
     * {{{
     * type DomainReturnAddressValue = ReturnAddressValue
     * }}}
     */
    type DomainReturnAddressValue <: ReturnAddressValue with DomainValue
    /**
     * Factory method to create instances of `ReturnAddressValue`s
     */
    def ReturnAddressValue(addresses: Set[Int]): DomainReturnAddressValue
    /**
     * Factory method to create instances of `ReturnAddressValue`s
     */
    def ReturnAddressValue(address: Int): DomainReturnAddressValue
    /**
     * Facilitates matching of `ReturnAddressValue`'s.
     */
    object ReturnAddressValue {
        def unapply(value: ReturnAddressValue): Option[Set[Int]] = Some(value.addresses)
    }

    /**
     * Factory method to create `TypedValue`s; i.e., values for which we have (more)
     * precise type information.
     */
    def TypedValue(someType: Type): DomainValue

    /**
     * Returns a representation of the integer constant value 0
     */
    val IntegerConstant0: DomainValue

    // -----------------------------------------------------------------------------------
    //
    // QUESTION'S ABOUT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Returns `true` iff at least one possible extension of given value is in the
     * specified range; that is if the intersection of the range of values captured
     * by the given value and the specified range is non-empty.
     * For example, if the given value captures all positive integer values and the
     * specified range is [-1,1] then the answer has to be Yes.
     *
     * Both bounds are inclusive.
     */
    def isValueInRange(value: DomainValue, lowerBound: Int, upperBound: Int): Answer 

    /**
     * Returns `true` iff at least one possible extension of given value is not in the
     * specified range; that is, if the range of values captured by the give value is 
     * not a strict subset of the specified range.
     * For example, if the given value is `10` and the
     * specified range is [0,Integer.MAX_VALUE] then the answer has to be No.
     *
     * Both bounds are inclusive.
     */
    def isValueNotInRange(value: DomainValue, lowerBound: Int, upperBound: Int): Answer 

    /*ABSTRACT*/ def isNull(value: DomainValue): Answer

    final private[ai] def isNonNull(value: DomainValue): Answer = isNull(value).negate

    /**
     * Are equal compares the values represented by the given values. If the values
     * are representing "ReferenceType" values - the object reference needs to be compared.
     */
    /*ABSTRACT*/ def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer

    final private[ai] def areNotEqualReferences(value1: DomainValue, value2: DomainValue): Answer =
        areEqualReferences(value1, value2).negate

    /**
     * Returns the type(s) of the value(s). Depending on the control flow the same
     * DomainValue can represent different values with different types.
     */
    /*ABSTRACT*/ def types(value: DomainValue): ValuesAnswer[Set[Type]]

    /*ABSTRACT*/ def isSubtypeOf(value: DomainValue, someType: Type): Answer

    /*ABSTRACT*/ def isSubtypeOf(subType: Type, superType: Type): Answer

    /*ABSTRACT*/ def areEqualIntegers(value1: DomainValue, value2: DomainValue): Answer

    final private[ai] def areNotEqualIntegers(value1: DomainValue, value2: DomainValue): Answer = areEqualIntegers(value1, value2).negate

    /*ABSTRACT*/ def isLessThan(smallerValue: DomainValue, largerValue: DomainValue): Answer

    /*ABSTRACT*/ def isLessThanOrEqualTo(smallerOrEqualValue: DomainValue, equalOrLargerValue: DomainValue): Answer

    final private[ai] def isGreaterThan(largerValue: DomainValue, smallerValue: DomainValue): Answer =
        isLessThan(smallerValue, largerValue)

    final private[ai] def isGreaterThanOrEqualTo(largerValue: DomainValue, smallerValue: DomainValue): Answer =
        isLessThanOrEqualTo(smallerValue, largerValue)

    final private[ai] def is0(value: DomainValue): Answer = areEqualIntegers(value, IntegerConstant0)

    final private[ai] def isNot0(value: DomainValue): Answer = areNotEqualIntegers(value, IntegerConstant0)

    final private[ai] def isLessThan0(value: DomainValue): Answer = isLessThan(value, IntegerConstant0)

    final private[ai] def isLessThanOrEqualTo0(value: DomainValue): Answer = isLessThanOrEqualTo(value, IntegerConstant0)

    final private[ai] def isGreaterThan0(value: DomainValue): Answer = isGreaterThan(value, IntegerConstant0)

    final private[ai] def isGreaterThanOrEqualTo0(value: DomainValue): Answer = isGreaterThanOrEqualTo(value, IntegerConstant0)

    // -----------------------------------------------------------------------------------
    //
    // HANDLING CONSTRAINTS RELATED TO VALUES
    //
    // -----------------------------------------------------------------------------------

    type Operands = List[DomainValue]
    type Locals = IndexedSeq[DomainValue]

    trait ValuesConstraint

    trait SingleValueConstraint extends (( /* pc :*/ Int, DomainValue, Operands, Locals) ⇒ (Operands, Locals)) with ValuesConstraint

    trait SingleValueConstraintWithBound[Bound] extends (( /* pc :*/ Int, /*bound :*/ Bound, DomainValue, Operands, Locals) ⇒ (Operands, Locals)) with ValuesConstraint

    trait TwoValuesConstraint extends (( /* pc :*/ Int, DomainValue, DomainValue, Operands, Locals) ⇒ (Operands, Locals)) with ValuesConstraint

    final private[ai] class ChangedOrderTwoValuesConstraint(f: () ⇒ TwoValuesConstraint) extends TwoValuesConstraint {
        def apply(pc: Int, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals): (Operands, Locals) =
            f()(pc, value2, value1, operands, locals)
    }

    final private[ai] class TwoValuesConstraintWithFixedSecondValue(f: () ⇒ TwoValuesConstraint, value2: DomainValue) extends SingleValueConstraint {
        def apply(pc: Int, value1: DomainValue, operands: Operands, locals: Locals): (Operands, Locals) =
            f()(pc, value2, value1, operands, locals)
    }

    final private[ai] class TwoValuesConstraintWithFixedFirstValue(f: () ⇒ TwoValuesConstraint, value1: DomainValue) extends SingleValueConstraint {
        def apply(pc: Int, value2: DomainValue, operands: Operands, locals: Locals): (Operands, Locals) =
            f()(pc, value2, value1, operands, locals)
    }

    //
    // W.r.t Reference Values
    def IsNull: SingleValueConstraint
    def IsNonNull: SingleValueConstraint
    def AreEqualReferences: TwoValuesConstraint
    def AreNotEqualReferences: TwoValuesConstraint
    def UpperBound: SingleValueConstraintWithBound[ReferenceType]
    //
    // W.r.t. Integer values
    def hasValue: SingleValueConstraintWithBound[Int]
    def AreEqualIntegers: TwoValuesConstraint
    def AreNotEqualIntegers: TwoValuesConstraint
    def IsLessThan: TwoValuesConstraint
    def IsLessThanOrEqualTo: TwoValuesConstraint
    private[ai] val IsGreaterThan: TwoValuesConstraint = new ChangedOrderTwoValuesConstraint(IsLessThan _)
    private[ai] val IsGreaterThanOrEqualTo: TwoValuesConstraint = new ChangedOrderTwoValuesConstraint(IsLessThanOrEqualTo _)
    private[ai] val Is0: SingleValueConstraint = new TwoValuesConstraintWithFixedSecondValue(AreEqualIntegers _, IntegerConstant0)
    private[ai] val IsNot0: SingleValueConstraint = new TwoValuesConstraintWithFixedSecondValue(AreNotEqualIntegers _, IntegerConstant0)
    private[ai] val IsLessThan0: SingleValueConstraint = new TwoValuesConstraintWithFixedSecondValue(IsLessThan _, IntegerConstant0)
    private[ai] val IsLessThanOrEqualTo0: SingleValueConstraint = new TwoValuesConstraintWithFixedSecondValue(IsLessThanOrEqualTo _, IntegerConstant0)
    private[ai] val IsGreaterThan0: SingleValueConstraint = new TwoValuesConstraintWithFixedFirstValue(IsLessThan _, IntegerConstant0)
    private[ai] val IsGreaterThanOrEqualTo0: SingleValueConstraint = new TwoValuesConstraintWithFixedFirstValue(IsLessThanOrEqualTo _, IntegerConstant0)

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO INSTRUCTIONS
    //
    // -----------------------------------------------------------------------------------

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

    /**
     * Called by BATAI when an exception is thrown that is not handled within the
     * same method.
     *
     * ==Note==
     * If the value has a specific type but is actually the value "null", then
     * the exception that is actually thrown is a `NullPointerException`. This
     * situation needs to be handled by the domain if necessary.
     *
     * This method is intended to be overridden; by default this method does nothing.
     */
    def abnormalReturn(exception: DomainValue): Unit = {}

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
    // TODO [AI] Add support for Java7's Invokedynamic to the Domain.
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

    /**
     * Merges the two given memory layouts. Returns `NoUpdate` if this
     * memory layout already subsumes the given memory layout.
     */
    def merge(
        thisOperands: Operands,
        thisLocals: Locals,
        otherOperands: Operands,
        otherLocals: Locals): Update[(Operands, Locals)] = {

        assume(thisOperands.size == otherOperands.size,
            "merging the memory layouts (A:"+thisOperands+"and B:"+otherOperands+" ) is not possible due to different stack sizes")

        assume(thisLocals.size == otherLocals.size,
            "merging the memory layouts is not possible due to different numbers of locals")

        var updateType: UpdateType = NoUpdateType

        var thisRemainingOperands = thisOperands
        var otherRemainingOperands = otherOperands
        var newOperands = List[DomainValue]() // during the update we build the operands stack in reverse order

        while (thisRemainingOperands.nonEmpty /* the number of operands of both memory layouts is equal */ ) {
            val thisOperand = thisRemainingOperands.head
            val otherOperand = otherRemainingOperands.head
            otherRemainingOperands = otherRemainingOperands.tail
            thisRemainingOperands = thisRemainingOperands.tail

            val updatedOperand = thisOperand merge otherOperand
            val newOperand = updatedOperand match {
                case SomeUpdate(operand) ⇒ operand
                case NoUpdate            ⇒ thisOperand
            }
            assume(!newOperand.isInstanceOf[NoLegalValue], "merging of stack values ("+thisOperand+" and "+otherOperand+") led to an illegal value")
            updateType = updateType &: updatedOperand
            newOperands = newOperand :: newOperands
        }

        val maxLocals = thisLocals.size

        val newLocals = new Array[DomainValue](maxLocals)
        var i = 0;
        while (i < maxLocals) {
            val thisLocal = thisLocals(i)
            val otherLocal = otherLocals(i)
            // The value calculated by "merge" may be the value "NoLegalValue" which means
            // the values in the corresponding register were different (path dependent)
            // on the different paths.
            // If we would have a liveness analysis, we could avoid the use of 
            // "NoLegalValue"
            val newLocal =
                if ((thisLocal eq null) || (otherLocal eq null)) {
                    updateType = updateType &: MetaInformationUpdateType
                    NoLegalValue("a register/local did not contain any value")
                } else {
                    val updatedLocal = thisLocal merge otherLocal
                    updateType = updateType &: updatedLocal
                    updatedLocal match {
                        case SomeUpdate(operand) ⇒ operand
                        case NoUpdate            ⇒ thisLocal
                    }

                }
            newLocals(i) = newLocal
            i += 1
        }

        updateType((newOperands.reverse, newLocals))
    }
}


