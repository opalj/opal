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

import language.higherKinds

/**
 * A domain contains all information about a program's types and values and determines
 * how a domain's values are calculated.
 *
 * This trait defines the interface between the abstract interpretation framework (BATAI)
 * and some (user defined) domain. I.e., all that is required by BATAI is an
 * implementation of this trait.
 *
 * To facilitate the usage of BATAI several classes/traits that implement parts of
 * the Domain trait are pre-defined.
 *
 * ==Control Flow==
 * BATAI controls the process of evaluating the program, but requires a
 * domain to perform the actual computations of an instruction's result.
 * Handling of instructions that move values between the stack/the locals
 * is completely embedded into BATAI.
 *
 * ==Thread Safety==
 * When every method is associated with a unique `Domain` instance as proposed and – given
 * that BATAI only uses one thread to analyze a given method at a time – no special care
 * has to be taken. However, if a domain needs to consult a domain which is associated with
 * a Project as a whole, which we will refer to as "World" in BATAI, it is then the
 * responsibility of the domain to make sure that everything is thread safe.
 * @note The framework assumes that every method/code block is associated with its
 *      own instance of a domain object.
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
     * inherit from this type (this may require a deep mixin composition) and that you
     * refine the type `DomainType` accordingly.
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
         * [[de.tud.cs.st.bat.resolved.ai.Domain.NoLegalValue]] when this value and
         * the given value are incompatible.
         *
         * For example, merging a `DomainValue` that represents the integer value 0
         * with a `DomainValue` that represents the integer value 1 may return a new
         * `DomainValue` that precisely captures the range or that captures all positive
         * integer values or just '''some integer value'''.
         *
         * The termination of the abstract interpretation directly depends on the fact
         * that at some point all values are fixed and don't change anymore.
         */
        def merge(value: DomainValue): Update[DomainValue]

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
     * The class tag for the type `DomainValue`.
     *
     * ==Initialization==
     * In the sub-trait or class that fixes the type of `DomainValue` it is necessary
     * to implement this abstract `val` using:
     * {{{
     * val DomainValueTag : ClassTag[DomainValue] = implicitly
     * }}}
     * (As of Scala 2.10 it is necessary that you do not use `implicit` - it will
     * compile, but fail at runtime.)
     */
    implicit val DomainValueTag: ClassTag[DomainValue]

    /**
     * Facilitates matching against values of computational type category 1.
     *
     * @example
     * {{{
     * case v @ CTC1() => ...
     * }}}
     */
    object CTC1 {
        def unapply(v: Value): Boolean = v.computationalType.category == 1
    }

    /**
     * Facilitates matching against values of computational type category 2.
     *
     * @example
     * {{{
     * case v @ CTC2() => ...
     * }}}
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
    class NoLegalValue extends Value { this: DomainValue ⇒

        def computationalType: ComputationalType =
            BATError("the value \"NoLegalValue\" does not have a computational type")

        def merge(value: DomainValue): Update[DomainValue] = {
            if (value == TheNoLegalValue)
                NoUpdate
            else
                MetaInformationUpdateNoLegalValue
        }

        override def toString = "NoLegalValue"
    }

    /**
     * Facilitates matching against `NoLegalValues`
     */
    object NoLegalValue {
        def unapply(value: NoLegalValue): Boolean = value ne null
    }

    /**
     * Abstracts over the concrete type of `NoLegalValue`.
     *
     * This type needs to be refined whenever the class `NoLegalValue`
     * is refined or the type `DomainValue` is refined.
     */
    type DomainNoLegalValue <: NoLegalValue with DomainValue

    val TheNoLegalValue: DomainNoLegalValue

    val MetaInformationUpdateNoLegalValue: MetaInformationUpdate[DomainNoLegalValue]

    final def StructuralUpdateNoLegalValue: StructuralUpdate[Nothing] =
        BATError("the merging of a value with an incompatible value always has to be a MetaInformationUpdate and not more")

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
     * Trait that is mixed in by values for which we have more precise type information
     * than just the computational type.
     */
    trait TypedValue[+T <: Type] extends Value {
        def valueType: T
    }

    /**
     * Enables matching against `TypedValues`.
     */
    object TypedValue {
        def unapply[T <: Type](tv: TypedValue[T]): Option[T] = Some(tv.valueType)
    }

    type DomainTypedValue[+T <: Type] <: TypedValue[T] with DomainValue

    /**
     * Factory method to create `TypedValue`s; i.e., values for which we have (more)
     * precise type information.
     */
    def TypedValue[T <: Type](someType: T): DomainTypedValue[T]

    /**
     * Returns a representation of the integer constant value 0
     */
    val IntegerConstant0: DomainValue = intValue(0)

    // -----------------------------------------------------------------------------------
    //
    // QUESTION'S ABOUT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Returns `true` iff at least one possible extension of given `value` is in the
     * specified range; that is if the intersection of the range of values captured
     * by the given `value` and the specified range is non-empty.
     * For example, if the given value captures all positive integer values and the
     * specified range is [-1,1] then the answer has to be Yes.
     *
     * @note Both bounds are inclusive.
     */
    def isValueInRange(value: DomainValue, lowerBound: Int, upperBound: Int): Boolean

    /**
     * Returns `true` iff at least one possible extension of given value is not in the
     * specified range; that is, if the set difference of the range of values captured
     * by the given `value` and  the specified range is non-empty.
     * For example, if the given value represents the integer value `10` and the
     * specified range is [0,Integer.MAX_VALUE] then the answer has to be No.
     *
     * @note Both bounds are inclusive.
     */
    def isValueNotInRange(value: DomainValue, lowerBound: Int, upperBound: Int): Boolean

    /**
     * Determines whether the given value is `null`, maybe `null` or is known not to be
     * `null`.
     */
    /*ABSTRACT*/ def isNull(value: DomainValue): Answer

    final private[ai] def isNonNull(value: DomainValue): Answer = isNull(value).negate

    /**
     * Compares the given values for reference equality. Returns `Yes` if both values
     * point to the same instance and returns `No` if both objects are known not to
     * point to the same instance. Otherwise `Unknown` is returned.
     */
    /*ABSTRACT*/ def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer

    final private[ai] def areNotEqualReferences(value1: DomainValue, value2: DomainValue): Answer =
        areEqualReferences(value1, value2).negate

    /**
     * Returns the type(s) of the value(s). Depending on the control flow the same
     * DomainValue can represent different values with different types.
     */
    /*ABSTRACT*/ def types(value: DomainValue): ValuesAnswer[Set[Type]]

    /*ABSTRACT*/ def isSubtypeOf(value: DomainValue, someType: ReferenceType): Answer

    /*ABSTRACT*/ def isSubtypeOf(subType: ReferenceType, superType: ReferenceType): Answer

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
    type Locals = Array[DomainValue]

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
    // HELPER TYPES AND FUNCTIONS RELATED TO THE RESULT OF INSTRUCTIONS
    //
    // -----------------------------------------------------------------------------------

    type SomeValueOrNullPointerException = Computation[DomainValue, DomainTypedValue[ObjectType.NullPointerException.type]]

    /**
     * Tests if the given value is `null` and returns a newly created
     * `NullPointerException` if it is the case. If the value is not `null`,
     * the given value is just wrapped and returned as this computation's result.
     */
    protected def givenValueOrNullPointerException(value: DomainValue): SomeValueOrNullPointerException = {
        isNull(value) match {
            case Yes     ⇒ ThrowsException(newObject(ObjectType.NullPointerException))
            case No      ⇒ ComputedValue(value)
            case Unknown ⇒ ComputedValueAndException(value, newObject(ObjectType.NullPointerException))
        }
    }

    type SideEffectOnlyOrNullPointerException = Computation[Nothing, DomainTypedValue[ObjectType.NullPointerException.type]]

    protected def sideEffectOnlyOrNullPointerException(value: DomainValue): SideEffectOnlyOrNullPointerException = {
        isNull(value) match {
            case Yes     ⇒ ThrowsException(newObject(ObjectType.NullPointerException))
            case No      ⇒ ComputationWithSideEffectOnly
            case Unknown ⇒ ComputationWithSideEffectOrException(newObject(ObjectType.NullPointerException))
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO INSTRUCTIONS
    //
    // -----------------------------------------------------------------------------------

    def athrow(exception: DomainValue): SomeValueOrNullPointerException =
        givenValueOrNullPointerException(exception)

    //
    // CREATE ARRAY
    //
    type NewArrayOrNegativeArraySizeException = Computation[DomainTypedValue[ArrayType], DomainTypedValue[ObjectType.ArithmeticException.type]]

    def newarray(count: DomainValue, componentType: FieldType): NewArrayOrNegativeArraySizeException
    def multianewarray(counts: List[DomainValue], arrayType: ArrayType): NewArrayOrNegativeArraySizeException

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //

    /**
     * The exceptions that may be thrown are: `NullPointerException` and
     * `ArrayIndexOutOfBoundsException`.
     */
    type ArrayLoadResult = Computation[DomainValue, Set[DomainTypedValue[ObjectType]]]
    /**
     * The exceptions that may be thrown are: `NullPointerException`,
     * `ArrayIndexOutOfBoundsException` and `ArrayStoreException`.
     */
    type ArrayStoreResult = Computation[Nothing, Set[DomainTypedValue[ObjectType]]]

    def aaload(index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def aastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): ArrayStoreResult
    def baload(index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def bastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): ArrayStoreResult
    def caload(index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def castore(value: DomainValue, index: DomainValue, arrayref: DomainValue): ArrayStoreResult
    def daload(index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def dastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): ArrayStoreResult
    def faload(index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def fastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): ArrayStoreResult
    def iaload(index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def iastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): ArrayStoreResult
    def laload(index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def lastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): ArrayStoreResult
    def saload(index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def sastore(value: DomainValue, index: DomainValue, arrayref: DomainValue): ArrayStoreResult

    //
    // LENGTH OF AN ARRAY
    //
    def arraylength(arrayref: DomainValue): SomeValueOrNullPointerException

    // 
    // PUSH CONSTANT VALUE
    //
    val theNullValue: DomainValue
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

    def checkcast(objectref: DomainValue, resolvedType: ReferenceType): Computation[DomainValue,DomainTypedValue[ObjectType.ClassCastException.type]]
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
     * Called by BATAI when an exception is thrown that is not guaranteed to be handled
     * within the same method.
     *
     * @note If the value has a specific type but is actually the value `null`, then
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
                 fieldType: FieldType): SomeValueOrNullPointerException
    def getstatic(declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType): DomainValue
    def putfield(objectref: DomainValue,
                 value: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType): SideEffectOnlyOrNullPointerException
    def putstatic(value: DomainValue,
                  declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType): Unit

    //
    // METHOD INVOCATIONS
    //
    type InvokeResult = Computation[Option[DomainValue], Set[DomainTypedValue[ObjectType]]]
    // TODO [AI] Add support for Java7's Invokedynamic to the Domain.
    def invokeinterface(declaringClass: ReferenceType,
                        name: String,
                        methodDescriptor: MethodDescriptor,
                        params: List[DomainValue]): InvokeResult
    def invokevirtual(declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      params: List[DomainValue]): InvokeResult
    def invokespecial(declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      params: List[DomainValue]): InvokeResult
    def invokestatic(declaringClass: ReferenceType,
                     name: String,
                     methodDescriptor: MethodDescriptor,
                     params: List[DomainValue]): InvokeResult

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

    type IntegerDivisionResult = Computation[DomainValue, DomainTypedValue[ObjectType.ArithmeticException.type]]

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
    def idiv(value1: DomainValue, value2: DomainValue): IntegerDivisionResult
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
    def ldiv(value1: DomainValue, value2: DomainValue): IntegerDivisionResult
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

    /**
     * Handles a `monitorenter` instruction.
     *
     * @note The default implementation checks if the given value is `null` and raises
     * an exception if it is `null` or maybe `null`. In the later case or in case that
     * the value is known not to be `null` the given value is (also) returned as this
     * computation's results.
     */
    def monitorenter(value: DomainValue): SideEffectOnlyOrNullPointerException = {
        sideEffectOnlyOrNullPointerException(value)
    }

    /**
     * Handles a `monitorenter` instruction.
     *
     * @note The default implementation checks if the given value is `null` and raises
     * an exception if it is `null` or maybe `null`. In the later case or in case that
     * the value is known not to be `null` the given value is (also) returned as this
     * computation's results.
     */
    def monitorexit(value: DomainValue): SideEffectOnlyOrNullPointerException = {
        sideEffectOnlyOrNullPointerException(value)
    }

    /**
     * Creates a new `DomainTypeValue` that represents an (new) instance of an
     * object of the given type.
     */
    def newObject(t: ObjectType): DomainTypedValue[t.type]

    //
    //
    // GENERAL METHODS
    //
    //

    /**
     * Merges the two given memory layouts.
     *
     * @return The merged memory layout. Returns `NoUpdate` if this memory layout
     * already subsumes the given memory layout.
     * @note The size of the operands stacks and the number of registers/locals
     * has to be the same.
     * @note The operand stacks have to contain compatible values. I.e., it has to be
     * possible to merge operand stack values without getting a `NoLegalValue`. In the
     * latter case – i.e., if the result of the merging of two operand stacks is
     * a `NoLegalValue` – either the bytecode is valid, which is extremely unlikely,
     * or the implementation of the domain is incomplete.
     */
    def merge(
        thisOperands: Operands,
        thisLocals: Locals,
        otherOperands: Operands,
        otherLocals: Locals): Update[(Operands, Locals)] = {

        assume(thisOperands.size == otherOperands.size,
            "domain merge - the stack sizes are different: "+thisOperands+" <=> "+otherOperands)

        assume(thisLocals.size == otherLocals.size,
            "domain merge - the number of registers differ: "+thisLocals+" <=> "+otherLocals)

        var operandsUpdated: UpdateType = NoUpdateType
        val newOperands =
            if (thisOperands eq otherOperands) {
                thisOperands
            } else {
                var thisRemainingOperands = thisOperands
                var otherRemainingOperands = otherOperands
                var newOperands = List[DomainValue]() // during the update we build the operands stack in reverse order

                while (thisRemainingOperands.nonEmpty /* && both stacks contain the same number of elements */ ) {
                    val thisOperand = thisRemainingOperands.head
                    thisRemainingOperands = thisRemainingOperands.tail
                    val otherOperand = otherRemainingOperands.head
                    otherRemainingOperands = otherRemainingOperands.tail

                    val newOperand =
                        if (thisOperand == otherOperand) {
                            thisOperand
                        } else {
                            val updatedOperand = thisOperand merge otherOperand
                            val newOperand = updatedOperand match {
                                case SomeUpdate(operand) ⇒ operand
                                case NoUpdate            ⇒ thisOperand
                            }
                            assume(!newOperand.isInstanceOf[NoLegalValue],
                                "domain merge - the result of merging the operands "+thisOperand+" and "+otherOperand+" is a NoLegalValue")
                            operandsUpdated = operandsUpdated &: updatedOperand
                            newOperand
                        }
                    newOperands = newOperand :: newOperands
                }
                if (operandsUpdated.noUpdate) {
                    thisOperands
                } else {
                    newOperands.reverse
                }
            }

        var localsUpdated: UpdateType = NoUpdateType
        val newLocals: Array[DomainValue] =
            if (thisLocals eq otherLocals) {
                thisLocals
            } else {
                val maxLocals = thisLocals.size
                val newLocals = new Array[DomainValue](maxLocals)
                var i = 0;
                while (i < maxLocals) {
                    val thisLocal = thisLocals(i)
                    val otherLocal = otherLocals(i)
                    // The value calculated by "merge" may be the value "NoLegalValue" 
                    // which means the values in the corresponding register were 
                    // different (path dependent) on the different paths. Hence, the
                    // values are no longer useful.
                    // If we would have a liveness analysis, we could avoid the use of 
                    // "NoLegalValue" and would avoid the useless merging of 
                    // dead values.
                    val newLocal =
                        if ((thisLocal eq null) || (otherLocal eq null)) {
                            localsUpdated = localsUpdated &: MetaInformationUpdateType
                            TheNoLegalValue
                        } else if (thisLocal == otherLocal) {
                            thisLocal
                        } else {
                            val updatedLocal = thisLocal merge otherLocal
                            if (updatedLocal == NoUpdate) {
                                thisLocal
                            } else {
                                localsUpdated = localsUpdated &: updatedLocal
                                updatedLocal.value
                            }
                        }
                    newLocals(i) = newLocal
                    i += 1
                }
                if (localsUpdated.noUpdate)
                    thisLocals
                else
                    newLocals
            }

        (operandsUpdated &: localsUpdated)((newOperands, newLocals))
    }
}


