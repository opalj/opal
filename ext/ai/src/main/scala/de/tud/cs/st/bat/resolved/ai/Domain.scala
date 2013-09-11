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

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

import reflect.ClassTag

/**
 * A domain contains all information about a program's types and values and determines
 * how a domain's values are calculated.
 *
 * This trait defines the interface between the abstract interpretation framework (BATAI)
 * and some (user defined) domain.
 *
 * To facilitate the usage of BATAI several classes/traits that implement parts of
 * the `Domain` trait are pre-defined and can be mixed in when needed.
 *
 * - [[de.tud.cs.st.bat.resolved.ai.domain.BaseDomain]]
 * - [[de.tud.cs.st.bat.resolved.ai.domain.DefaultDomain]]
 *
 * ==Control Flow==
 * BATAI controls the process of evaluating the code of a method, but requires a
 * domain to perform the actual computations of an instruction's result.
 * Handling of instructions that move values between the stack/the locals
 * is completely embedded into BATAI.
 *
 * ==Thread Safety==
 * When every method is associated with a unique `Domain` instance as proposed and – given
 * that BATAI only uses one thread to analyze a given method at a time – no special care
 * has to be taken. However, if a domain needs to consult a domain which is associated with
 * a project as a whole, which we will refer to as "World" in BATAI, it is then the
 * responsibility of the domain to make sure that coordination with the world is thread
 * safe.
 *
 * @note The framework assumes that conceptually every method/code block is associated
 *      with its own instance of a domain object.
 * @tparam I The type which is used to identify this domain's context. E.g., if a new
 *      object is created it may be associated with the instruction that created it and
 *      this domain's identifier.
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait Domain[I] {

    /**
     * Returns the value that identifies this domain (method).
     *
     * This value may subsequently be used to identify/track object instances but – if
     * so – this happens at the sole responsibility of the domain. BATAI does
     * not require any kind of tracking.
     */
    def identifier: I

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO THE HANDLING OF VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over a concrete operand stack value or a value stored in one of the local
     * variables.
     *
     * ==Extending Value==
     * If you extend this trait, make sure that you also extend all classes/traits that
     * inherit from this type (this may require a deep mixin composition and that you
     * refine the type `DomainType` accordingly).
     */
    trait Value {
        /**
         * The computational type of the value.
         *
         * The precise computational type is needed by BATAI to calculate the effect
         * of generic stack manipulation instructions (e.g., `dup_...` and swap)
         * on the stack. This is required to calculate the
         * jump targets of RET instructions and to determine which values are
         * actually copied by the dupXX instructions.
         *
         * '''W.r.t. the computational type no kind of further abstraction is allowed.'''
         */
        def computationalType: ComputationalType

        /**
         * Merges this value with the given value.
         *
         * For example, merging a `DomainValue` that represents the integer value 0
         * with a `DomainValue` that represents the integer value 1 may return a new
         * `DomainValue` that precisely captures the range or that captures all positive
         * integer values or just '''some integer value'''.
         *
         * The termination of the abstract interpretation directly depends on the fact
         * that at some point all values are fixed and don't change anymore. Hence,
         * it is important that the type of the update is only a
         * [[de.tud.cs.st.bat.resolved.ai.StructuralUpdate]] if the value has changed.
         *
         * @note ***This value*** is always the value that was used by BATAI for
         *      subsequent computations. Hence, the merge operator is not commutative!
         *      Furthermore, if ***this value*** subsumes the given value the result
         *      has to be either `NoUpdate` or a `MetaInformationUpdate`; it must not
         *      be a `StructuralUpdate`.
         * @param pc The program counter of the instruction where the paths converge.
         * @param value The "new" domain value.
         */
        def merge(pc: Int, value: DomainValue): Update[DomainValue]

        /**
         * The type of the represented value (e.g., ByteType).
         * If the value represents some reference typed value, the type maybe some
         * TypeBound or a set thereof. Imagine, e.g., a method that declares to
         * return a `java.util.List` object. In this case (i.e., if we don't analyze
         * the called method) we only have a type bound. However, if we analyze the
         * called method, we may determine that the type of the returned object is
         * always either a `java.util.ArrayList` or `java.util.LinkedList`.
         */
        type ValueType <: AnyRef

        def valueType: ValueType
    }

    /**
     * Abstracts over the concrete type of `Value`. Needs to be refined by traits that
     * inherit from `Domain` and which extend `Domain`'s `Value` trait.
     */
    type DomainValue <: Value

    /**
     * The class tag for the type `DomainValue`. Required to generate Array instances.
     *
     * ==Initialization==
     * In the sub-trait or class that fixes the type of `DomainValue` it is necessary
     * to implement this abstract `val` using:
     * {{{
     * val DomainValueTag : ClassTag[DomainValue] = implicitly
     * }}}
     * (As of Scala 2.10 it is necessary that you do not use `implicit` in the subclass -
     * it will compile, but fail at runtime.)
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
     * Represents a value that has no well defined state/type.
     *
     * If the AI framework tries to merge two values that are incompatible, the result has
     * to be an instance of `NoLegalValue`. This may happen, e.g., when BATAI tries to
     * merge two register values/locals that are not live (i.e., which should not be
     * live) and, hence, are actually allowed to contain incompatible values.
     * (`Not live` means that the value will not be used in the future.)
     */
    protected class NoLegalValue extends Value { this: DomainValue ⇒

        def computationalType: ComputationalType =
            BATException("the value \"NoLegalValue\" does not have a computational type")

        def merge(pc: Int, value: DomainValue): Update[DomainValue] = {
            if (value == TheNoLegalValue)
                NoUpdate
            else
                MetaInformationUpdateNoLegalValue
        }

        type ValueType = Nothing
        def valueType = throw DomainException(Domain.this, "a non-legal value has no type")

        override def toString = "NoLegalValue"
    }

    /**
     * Abstracts over the concrete type of `NoLegalValue`.
     *
     * This type needs to be refined whenever the class `NoLegalValue`
     * is refined or the type `DomainValue` is refined.
     */
    type DomainNoLegalValue <: NoLegalValue with DomainValue

    /**
     * The **singleton** instance of a `NoLegalVAlue`.
     */
    val TheNoLegalValue: DomainNoLegalValue

    /**
     * If the result of the merge of two values is a non-legal value. The result has
     * to be reported as a `MetaInformationUpdate`.
     */
    val MetaInformationUpdateNoLegalValue: MetaInformationUpdate[DomainNoLegalValue]

    /**
     * The result of the the merging of two values should never be reported as a
     * `StructuralUpdate` if the computer value is a `NoLegalValue`.
     *
     * This method is solely defined to catch implementation errors early on.
     */
    final def StructuralUpdateNoLegalValue: StructuralUpdate[Nothing] =
        throw DomainException(Domain.this,
            "the merging of a value with an incompatible value "+
                "always has to be a MetaInformationUpdate and not more")

    /**
     * Represents a set of concrete values that store return addresses (i.e., a program
     * counter/index into the code array).
     *
     * @note The framework completely handles all aspects related to return address values.
     */
    class ReturnAddressValue(
        val addresses: Set[Int])
            extends Value {

        override def merge(pc: Int, value: DomainValue): Update[DomainValue] = value match {
            case ReturnAddressValue(otherAddresses) ⇒ {
                if (otherAddresses subsetOf this.addresses)
                    NoUpdate
                else
                    StructuralUpdate(ReturnAddressValue(this.addresses ++ otherAddresses))
            }
            case _ ⇒ MetaInformationUpdateNoLegalValue
        }

        type ValueType = Nothing
        def valueType = throw DomainException(Domain.this,
            "ReturnAddressValues are not associated with a type")

        final def computationalType: ComputationalType = ComputationalTypeReturnAddress

        override def toString = "ReturnAddresses: "+addresses.mkString(", ")

    }
    /**
     * Abstracts over the concrete type of `ReturnAddressValue`. Needs to be fixed
     * by some sub-trait/sub-class. In the simplest case (i.e., when neither the
     * `Value` trait nor the `ReturnAddressValue` trait is refined it is sufficient
     * to write:
     * {{{
     * type DomainReturnAddressValue = ReturnAddressValue
     * }}}
     */
    type DomainReturnAddressValue <: ReturnAddressValue with DomainValue
    /**
     * Factory method to create instances of `ReturnAddressValue`s.
     */
    def ReturnAddressValue(addresses: Set[Int]): DomainReturnAddressValue
    /**
     * Factory method to create an instance of a `ReturnAddressValue`.
     */
    def ReturnAddressValue(address: Int): DomainReturnAddressValue =
        ReturnAddressValue(Set(address))

    /**
     * Facilitates matching of `ReturnAddressValue`'s.
     */
    object ReturnAddressValue {
        def unapply(value: ReturnAddressValue): Option[Set[Int]] = Some(value.addresses)
    }

    /**
     * Factory method to create domain values with a specific type. I.e., values for
     * which we have some type information but no value or location information.
     * For example, if a `TypedValue` represents a reference type it may be possible
     * that the value is `null`, but such knowledge is not available.
     *
     * BATAI only uses this method when a method is to be analyzed, but no parameter
     * values are given.
     */
    def TypedValue(valueType: Type): DomainValue = {
        val newValue = (valueType match {
            case BooleanType       ⇒ SomeBooleanValue
            case ByteType          ⇒ SomeByteValue
            case ShortType         ⇒ SomeShortValue
            case CharType          ⇒ SomeCharValue
            case IntegerType       ⇒ SomeIntegerValue
            case FloatType         ⇒ SomeFloatValue
            case LongType          ⇒ SomeLongValue
            case DoubleType        ⇒ SomeDoubleValue
            case rt: ReferenceType ⇒ SomeReferenceValue(rt)
            case VoidType ⇒
                domainException(Domain.this, "it is not possible to create a void typed value")
        })
        newValue
    }

    /**
     * Represents some boolean value, where the source of the value is not known.
     */
    def SomeBooleanValue: DomainValue

    /**
     * Represents some byte value, where the source of the value is not known.
     */
    def SomeByteValue: DomainValue
    /**
     * Represents some short value, where the source of the value is not known.
     */
    def SomeShortValue: DomainValue
    /**
     * Represents some char value, where the source of the value is not known.
     */
    def SomeCharValue: DomainValue
    /**
     * Represents some integer value, where the source of the value is not known.
     */
    def SomeIntegerValue: DomainValue
    /**
     * Represents some float value, where the source of the value is not known.
     */
    def SomeFloatValue: DomainValue
    /**
     * Represents some long value, where the source of the value is not known.
     */
    def SomeLongValue: DomainValue
    /**
     * Represents some double value, where the source of the value is not known.
     */
    def SomeDoubleValue: DomainValue
    /**
     * Represents some reference value, where the source of the value is not known.
     */
    def SomeReferenceValue(referenceType: ReferenceType): DomainValue

    /**
     * Returns a representation of the integer constant value 0.
     *
     * BATAI uses this special value for comparisons against the fixed value 0.
     * (e.g., for if_XX instructions).
     */
    val IntegerConstant0: DomainValue

    // -----------------------------------------------------------------------------------
    //
    // QUESTION'S ABOUT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Returns `true` iff at least one possible extension of the given `value` is in the
     * specified range; that is if the intersection of the range of values captured
     * by the given `value` and the specified range is non-empty.
     * For example, if the given value captures all positive integer values and the
     * specified range is [-1,1] then the answer has to be Yes.
     *
     * @param value A value that has to be of computational type integer.
     * @param lowerBound The range's lower bound (inclusive).
     * @param upperBound The range's upper bound (inclusive).
     */
    /*ABSTRACT*/ def isSomeValueInRange(value: DomainValue,
                                        lowerBound: Int,
                                        upperBound: Int): Boolean

    /**
     * Returns `true` iff at least one possible extension of given value is not in the
     * specified range; that is, if the set difference of the range of values captured
     * by the given `value` and  the specified range is non-empty.
     * For example, if the given value represents the integer value `10` and the
     * specified range is [0,Integer.MAX_VALUE] then the answer has to be `false`. But,
     * if the given value represents the range [-5,Integer.MAX_VALUE] and the specified
     * range is again [0,Integer.MAX_VALUE] then the answer has to be `true`.
     *
     * @param value A value that has to be of computational type integer.
     * @param lowerBound The range's lower bound (inclusive).
     * @param upperBound The range's upper bound (inclusive).
     */
    /*ABSTRACT*/ def isSomeValueNotInRange(value: DomainValue,
                                           lowerBound: Int,
                                           upperBound: Int): Boolean

    /**
     * Determines whether the given value is `null` (`Yes`), maybe `null` (`Unknown`) or
     * is known not to be `null` (`No`).
     *
     * @param value A value of computational type reference.
     */
    /*ABSTRACT*/ def isNull(value: DomainValue): Answer

    final private[ai] def isNonNull(value: DomainValue): Answer = isNull(value).negate

    /**
     * Compares the given values for reference equality. Returns `Yes` if both values
     * point to the same instance and returns `No` if both objects are known not to
     * point to the same instance. The latter is, e.g., trivially the case when both
     * values have a different concrete type. Otherwise `Unknown` is returned.
     *
     * @param value1 A value of computational type reference.
     * @param value2 A value of computational type reference.
     */
    /*ABSTRACT*/ def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer

    final private[ai] def areNotEqualReferences(value1: DomainValue, value2: DomainValue): Answer =
        areEqualReferences(value1, value2).negate

    /**
     * Returns the type(type bounds) of the value. Depending on the control flow, the same
     * `DomainValue` can represent different values with different types. However,
     * all types that the domain value represents have to belong to the same
     * computational type category. I.e., it is possible that the value captures the
     * types "`NullPointerException` and `IllegalArgumentException`", but it will never
     * capture – at the same time – the (Java) types `int` and `long`.
     */
    /*ABSTRACT*/ def types(value: DomainValue): TypesAnswer[_]

    /**
     * Tries to determine if the type of the given `value` is a sub-type of the
     * specified reference type (`superType`).
     *
     * @param value A value with computational type reference.
     */
    /*ABSTRACT*/ def isSubtypeOf(value: DomainValue, superType: ReferenceType): Answer

    /**
     * Tries to determine if the type referred to as `subtype` is a subtype of the
     * specified reference type `supertype`.
     */
    /*ABSTRACT*/ def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer

    /**
     * Tests if the two given integer values are equal.
     *
     * To return meaningful results a domain is required that is able to track the
     * flow of concrete integer values.
     *
     * @param value1 A value with computational type integer.
     * @param value2 A value with computational type integer.
     */
    /*ABSTRACT*/ def areEqual(value1: DomainValue, value2: DomainValue): Answer

    /**
     * Tests if the two given integer values are not equal.
     *
     * To return meaningful results a domain is required that is able to track the
     * flow of concrete integer values.
     *
     * @param value1 A value with computational type integer.
     * @param value2 A value with computational type integer.
     */
    final private[ai] def areNotEqual(value1: DomainValue, value2: DomainValue): Answer =
        areEqual(value1, value2).negate

    /**
     * Tests if the first integer value is smaller than the second value.
     *
     * @param smallerValue A value with computational type integer.
     * @param largerValue A value with computational type integer.
     */
    /*ABSTRACT*/ def isLessThan(smallerValue: DomainValue, largerValue: DomainValue): Answer

    /**
     * Tests if the first integer value is less than or equal to the second value.
     *
     * @param smallerOrEqualValue A value with computational type integer.
     * @param equalOrLargerValue A value with computational type integer.
     */
    /*ABSTRACT*/ def isLessThanOrEqualTo(smallerOrEqualValue: DomainValue,
                                         equalOrLargerValue: DomainValue): Answer

    /**
     * Tests if the first integer value is larger than the second value.
     *
     * @param largerValue A value with computational type integer.
     * @param smallerValue A value with computational type integer.
     */
    final private[ai] def isGreaterThan(largerValue: DomainValue,
                                        smallerValue: DomainValue): Answer =
        isLessThan(smallerValue, largerValue)

    /**
     * Tests if the first integer value is larger than or equal to the second value.
     *
     * @param largerOrEqualValue A value with computational type integer.
     * @param smallerOrEqualValue A value with computational type integer.
     */
    final private[ai] def isGreaterThanOrEqualTo(largerOrEqualValue: DomainValue,
                                                 smallerOrEqualValue: DomainValue): Answer =
        isLessThanOrEqualTo(smallerOrEqualValue, largerOrEqualValue)

    /**
     * Tests if the given integer value is 0 or maybe 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def is0(value: DomainValue): Answer =
        areEqual(value, IntegerConstant0)

    /**
     * Tests if the given integer value is not 0 or maybe not 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def isNot0(value: DomainValue): Answer =
        areNotEqual(value, IntegerConstant0)

    /**
     * Tests if the given integer value is &lt; 0 or maybe &lt; 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def isLessThan0(value: DomainValue): Answer =
        isLessThan(value, IntegerConstant0)

    /**
     * Tests if the given integer value is less than or equal to 0 or maybe
     * less than or equal to 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def isLessThanOrEqualTo0(value: DomainValue): Answer =
        isLessThanOrEqualTo(value, IntegerConstant0)

    /**
     * Tests if the given integer value is &gt; 0 or maybe &gt; 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def isGreaterThan0(value: DomainValue): Answer =
        isGreaterThan(value, IntegerConstant0)

    /**
     * Tests if the given value is greater than or equal to 0 or maybe greater
     * than or equal to 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def isGreaterThanOrEqualTo0(value: DomainValue): Answer =
        isGreaterThanOrEqualTo(value, IntegerConstant0)

    // -----------------------------------------------------------------------------------
    //
    // HANDLING CONSTRAINTS RELATED TO VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * An instruction's operands are represented using a list where the first
     * element of the list represents the top level operand stack value.
     */
    type Operands = List[DomainValue]
    /**
     * An instruction's current register values/locals are represented using an array.
     */
    type Locals = Array[DomainValue]

    type SingleValueConstraint = (( /* pc :*/ Int, DomainValue, Operands, Locals) ⇒ (Operands, Locals))

    type TwoValuesConstraint = (( /* pc :*/ Int, DomainValue, DomainValue, Operands, Locals) ⇒ (Operands, Locals))

    //
    // W.r.t Reference Values
    /**
     * Called by BATAI when it establishes that the value is `null` or has to be
     * `null`. E.g., after a comparison with `null` BATAI can establish that the
     * value has to be `null` on one branch and that the value is not `null` on the
     * other branch.
     */
    def establishIsNull(pc: Int,
                        value: DomainValue,
                        operands: Operands,
                        locals: Locals): (Operands, Locals) =
        (operands, locals)
    val IsNull = establishIsNull _

    /**
     * Called by BATAI when it establishes that the value is guaranteed not to be `null`.
     * E.g., after a comparison with `null` BATAI can establish that the
     * value has to be `null` on one branch and that the value is not `null` on the
     * other branch.
     */
    def establishIsNonNull(pc: Int,
                           value: DomainValue,
                           operands: Operands,
                           locals: Locals): (Operands, Locals) =
        (operands, locals)
    val IsNonNull = establishIsNonNull _

    /**
     * Called by BATAI when two values were compared for reference equality and
     * we are currently analyzing the branch where the comparison succeeded.
     */
    def establishAreEqualReferences(pc: Int,
                                    value1: DomainValue,
                                    value2: DomainValue,
                                    operands: Operands,
                                    locals: Locals): (Operands, Locals) =
        (operands, locals)
    val AreEqualReferences = establishAreEqualReferences _

    def establishAreNotEqualReferences(pc: Int,
                                       value1: DomainValue,
                                       value2: DomainValue,
                                       operands: Operands,
                                       locals: Locals): (Operands, Locals) =
        (operands, locals)
    val AreNotEqualReferences = establishAreNotEqualReferences _

    def establishUpperBound(pc: Int,
                            bound: ReferenceType,
                            value: DomainValue,
                            operands: Operands,
                            locals: Locals): (Operands, Locals) =
        (operands, locals)

    //
    // W.r.t. Integer values

    def establishValue(pc: Int,
                       theValue: Int,
                       value: DomainValue,
                       operands: Operands,
                       locals: Locals): (Operands, Locals) =
        (operands, locals)

    def establishAreEqual(pc: Int,
                          value1: DomainValue,
                          value2: DomainValue,
                          operands: Operands,
                          locals: Locals): (Operands, Locals) =
        (operands, locals)
    val AreEqual = establishAreEqual _

    def establishAreNotEqual(pc: Int,
                             value1: DomainValue,
                             value2: DomainValue,
                             operands: Operands,
                             locals: Locals): (Operands, Locals) =
        (operands, locals)
    val AreNotEqual = establishAreNotEqual _

    def establishIsLessThan(pc: Int,
                            value1: DomainValue,
                            value2: DomainValue,
                            operands: Operands,
                            locals: Locals): (Operands, Locals) =
        (operands, locals)
    val IsLessThan = establishIsLessThan _

    def establishIsLessThanOrEqualTo(pc: Int,
                                     value1: DomainValue,
                                     value2: DomainValue,
                                     operands: Operands,
                                     locals: Locals): (Operands, Locals) =
        (operands, locals)
    val IsLessThanOrEqualTo = establishIsLessThanOrEqualTo _

    protected[ai] val IsGreaterThan: TwoValuesConstraint =
        (pc: Int, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThan(pc, value2, value1, operands, locals)

    protected[ai] val IsGreaterThanOrEqualTo: TwoValuesConstraint =
        (pc: Int, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThanOrEqualTo(pc, value2, value1, operands, locals)

    protected[ai] val Is0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishAreEqual(pc, value, IntegerConstant0, operands, locals)

    protected[ai] val IsNot0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishAreNotEqual(pc, value, IntegerConstant0, operands, locals)

    protected[ai] val IsLessThan0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThan(pc, value, IntegerConstant0, operands, locals)

    protected[ai] val IsLessThanOrEqualTo0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThanOrEqualTo(pc, value, IntegerConstant0, operands, locals)

    protected[ai] val IsGreaterThan0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThan(pc, IntegerConstant0, value, operands, locals)

    protected[ai] val IsGreaterThanOrEqualTo0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThanOrEqualTo(pc, IntegerConstant0, value, operands, locals)

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO INSTRUCTIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // HELPER TYPES AND FUNCTIONS RELATED TO THE RESULT OF INSTRUCTIONS
    //

    type SucceedsOrNullPointerException = Computation[Nothing, DomainValue]
    type OptionalReturnValueOrExceptions = Computation[Option[DomainValue], Set[DomainValue]]
    type NumericValueOrNullPointerException = Computation[DomainValue, DomainValue]
    type ReferenceValueOrNullPointerException = Computation[DomainValue, DomainValue]

    /**
     * Tests if the given reference value is `null` and returns a newly created
     * `NullPointerException` if it is the case. If the value is not `null`,
     * the given value is just wrapped and returned as this computation's result.
     */
    // FIXME As soon as the athrow is gone remove this method/move it to a subclass.
    protected def givenValueOrNullPointerException(
        pc: Int,
        value: DomainValue): ReferenceValueOrNullPointerException = {
        isNull(value) match {
            case Yes ⇒ ThrowsException(newObject(pc, ObjectType.NullPointerException))
            case No  ⇒ ComputedValue(value)
            case Unknown ⇒ ComputedValueAndException(
                value,
                newObject(pc, ObjectType.NullPointerException)
            )
        }
    }

    protected def sideEffectOnlyOrNullPointerException(
        pc: Int,
        value: DomainValue): SucceedsOrNullPointerException = {
        isNull(value) match {
            case Yes ⇒ ThrowsException(newObject(pc, ObjectType.NullPointerException))
            case No  ⇒ ComputationWithSideEffectOnly
            case Unknown ⇒ ComputationWithSideEffectOrException(
                newObject(pc, ObjectType.NullPointerException)
            )
        }
    }

    //
    // METHODS TO IMPLEMENT THE SEMANTICS OF INSTRUCTIONS
    //

    //
    // THROW EXCEPTION
    //

    /**
     * The normal result is the given value unless it is `null`. If the value is `null` a
     * new instance of a `NullPointerException` is created and returned as the thrown
     * exception.
     */
    // FIXME Embed into AI/Move to AI.
    def athrow(pc: Int, exception: DomainValue): Computation[DomainValue, DomainValue] =
        givenValueOrNullPointerException(pc, exception)

    //
    // CREATE ARRAY
    //
    type NewArrayOrNegativeArraySizeException = Computation[DomainValue, DomainValue]

    def newarray(pc: Int,
                 count: DomainValue,
                 componentType: FieldType): NewArrayOrNegativeArraySizeException

    def multianewarray(pc: Int,
                       counts: List[DomainValue],
                       arrayType: ArrayType): NewArrayOrNegativeArraySizeException

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //

    /**
     * Computation that returns the value stored in an array at a given index or an
     * exception. The exceptions that may be thrown are: `NullPointerException` and
     * `ArrayIndexOutOfBoundsException`.
     */
    type ArrayLoadResult = Computation[DomainValue, Set[DomainValue]]
    /**
     * Computation that succeeds (updates the value stored in the array at the given
     * index) or that throws an exception. The exceptions that may be thrown are:
     * `NullPointerException`, `ArrayIndexOutOfBoundsException` and `ArrayStoreException`.
     */
    type ArrayStoreResult = Computation[Nothing, Set[DomainValue]]

    def aaload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def aastore(pc: Int,
                value: DomainValue,
                index: DomainValue, arrayref: DomainValue): ArrayStoreResult

    def baload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def bastore(pc: Int,
                value: DomainValue,
                index: DomainValue, arrayref: DomainValue): ArrayStoreResult

    def caload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def castore(pc: Int,
                value: DomainValue,
                index: DomainValue, arrayref: DomainValue): ArrayStoreResult

    def daload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def dastore(pc: Int,
                value: DomainValue,
                index: DomainValue, arrayref: DomainValue): ArrayStoreResult

    def faload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def fastore(pc: Int,
                value: DomainValue,
                index: DomainValue, arrayref: DomainValue): ArrayStoreResult

    def iaload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def iastore(pc: Int,
                value: DomainValue,
                index: DomainValue, arrayref: DomainValue): ArrayStoreResult

    def laload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult
    def lastore(pc: Int,
                value: DomainValue,
                index: DomainValue, arrayref: DomainValue): ArrayStoreResult

    def saload(pc: Int,
               index: DomainValue,
               arrayref: DomainValue): ArrayLoadResult
    def sastore(pc: Int,
                value: DomainValue,
                index: DomainValue, arrayref: DomainValue): ArrayStoreResult

    //
    // LENGTH OF AN ARRAY
    //

    /**
     * Returns the array's length or throws a `NullPointerException`.
     */
    def arraylength(pc: Int, arrayref: DomainValue): NumericValueOrNullPointerException

    // 
    // PUSH CONSTANT VALUE
    //

    /**
     * Called by BATAI when a null value is to be pushed onto the stack.
     */
    def nullValue(pc: Int): DomainValue

    /**
     * Called by BATAI when a constant byte value is to be pushed onto the stack.
     */
    def byteValue(pc: Int, value: Int): DomainValue
    /**
     * Called by BATAI when a constant short value is to be pushed onto the stack.
     */
    def shortValue(pc: Int, value: Int): DomainValue
    /**
     * Called by BATAI when a constant int value is to be pushed onto the stack.
     */
    def intValue(pc: Int, value: Int): DomainValue
    /**
     * Called by BATAI when a constant long value is to be pushed onto the stack.
     */
    def longValue(pc: Int, value: Long): DomainValue
    /**
     * Called by BATAI when a constant float value is to be pushed onto the stack.
     */
    def floatValue(pc: Int, value: Float): DomainValue
    /**
     * Called by BATAI when a constant double value is to be pushed onto the stack.
     */
    def doubleValue(pc: Int, value: Double): DomainValue
    /**
     * Called by BATAI when a constant string value is to be pushed onto the stack.
     */
    def stringValue(pc: Int, value: String): DomainValue
    /**
     * @return A value that represents a runtime value of type "Class<T>"
     */
    def classValue(pc: Int, t: Type): DomainValue

    //
    // TYPE CHECKS AND CONVERSION
    //

    def checkcast(pc: Int,
                  objectref: DomainValue,
                  resolvedType: ReferenceType): Computation[DomainValue, DomainValue]

    def instanceof(pc: Int,
                   objectref: DomainValue,
                   resolvedType: ReferenceType): DomainValue

    def d2f(pc: Int, value: DomainValue): DomainValue
    def d2i(pc: Int, value: DomainValue): DomainValue
    def d2l(pc: Int, value: DomainValue): DomainValue

    def f2d(pc: Int, value: DomainValue): DomainValue
    def f2i(pc: Int, value: DomainValue): DomainValue
    def f2l(pc: Int, value: DomainValue): DomainValue

    def i2b(pc: Int, value: DomainValue): DomainValue
    def i2c(pc: Int, value: DomainValue): DomainValue
    def i2d(pc: Int, value: DomainValue): DomainValue
    def i2f(pc: Int, value: DomainValue): DomainValue
    def i2l(pc: Int, value: DomainValue): DomainValue
    def i2s(pc: Int, value: DomainValue): DomainValue

    def l2d(pc: Int, value: DomainValue): DomainValue
    def l2f(pc: Int, value: DomainValue): DomainValue
    def l2i(pc: Int, value: DomainValue): DomainValue

    //
    // RETURN FROM METHOD
    //
    def areturn(pc: Int, value: DomainValue): Unit
    def dreturn(pc: Int, value: DomainValue): Unit
    def freturn(pc: Int, value: DomainValue): Unit
    def ireturn(pc: Int, value: DomainValue): Unit
    def lreturn(pc: Int, value: DomainValue): Unit
    def returnVoid(pc: Int): Unit

    /**
     * Called by BATAI when an exception is thrown that is not (guaranteed to be) handled
     * within the same method.
     *
     * @note If the original exception value is `null`, then
     *      the exception that is actually thrown is a new `NullPointerException`. This
     *      situation is, however, completely handled by BATAI.
     */
    def abruptMethodExecution(pc: Int, exception: DomainValue): Unit

    //
    // ACCESSING FIELDS
    //
    type FieldValueOrNullPointerException = Computation[DomainValue, DomainValue]

    def getfield(pc: Int,
                 objectref: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType): FieldValueOrNullPointerException

    def getstatic(pc: Int,
                  declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType): DomainValue

    def putfield(pc: Int,
                 objectref: DomainValue,
                 value: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType): SucceedsOrNullPointerException

    def putstatic(pc: Int,
                  value: DomainValue,
                  declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType): Unit

    //
    // METHOD INVOCATIONS
    //

    // TODO [AI] Add support for Java7's Invokedynamic to the Domain.
    def invokeinterface(pc: Int,
                        declaringClass: ReferenceType,
                        name: String,
                        methodDescriptor: MethodDescriptor,
                        operands: List[DomainValue]): OptionalReturnValueOrExceptions

    def invokevirtual(pc: Int,
                      declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      operands: List[DomainValue]): OptionalReturnValueOrExceptions

    def invokespecial(pc: Int,
                      declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      operands: List[DomainValue]): OptionalReturnValueOrExceptions

    def invokestatic(pc: Int,
                     declaringClass: ReferenceType,
                     name: String,
                     methodDescriptor: MethodDescriptor,
                     operands: List[DomainValue]): OptionalReturnValueOrExceptions

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def fcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def dcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def dcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def lcmp(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    //
    // UNARY EXPRESSIONS
    //
    def fneg(pc: Int, value: DomainValue): DomainValue
    def dneg(pc: Int, value: DomainValue): DomainValue
    def lneg(pc: Int, value: DomainValue): DomainValue
    def ineg(pc: Int, value: DomainValue): DomainValue

    //
    // BINARY EXPRESSIONS
    //

    /**
     * Computation that returns a numeric value or an `ObjectType.ArithmeticException`.
     */
    type IntegerDivisionResult = Computation[DomainValue, DomainValue]

    def dadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def ddiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def dmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def drem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def dsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    def fadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def fdiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def fmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def frem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def fsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    def iadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def iand(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def idiv(pc: Int, value1: DomainValue, value2: DomainValue): IntegerDivisionResult
    def imul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def ior(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def irem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def ishl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def ishr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def isub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def iushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def ixor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    def ladd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def land(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def ldiv(pc: Int, value1: DomainValue, value2: DomainValue): IntegerDivisionResult
    def lmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def lor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def lrem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def lshl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def lshr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def lsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def lushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def lxor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    //
    // "OTHER" INSTRUCTIONS
    //

    def iinc(pc: Int, value: DomainValue, increment: Int): DomainValue

    /**
     * Handles a `monitorenter` instruction.
     *
     * @note The default implementation checks if the given value is `null` and raises
     * an exception if it is `null` or maybe `null`. In the later case or in case that
     * the value is known not to be `null` the given value is (also) returned as this
     * computation's results.
     */
    def monitorenter(pc: Int, value: DomainValue): SucceedsOrNullPointerException = {
        sideEffectOnlyOrNullPointerException(pc, value)
    }

    /**
     * Handles a `monitorenter` instruction.
     *
     * @note The default implementation checks if the given value is `null` and raises
     * an exception if it is `null` or maybe `null`. In the later case or in case that
     * the value is known not to be `null` the given value is (also) returned as this
     * computation's results.
     */
    def monitorexit(pc: Int, value: DomainValue): SucceedsOrNullPointerException = {
        sideEffectOnlyOrNullPointerException(pc, value)
    }

    /**
     * Creates a new `DomainValue` that represents an (new) instance of an
     * object of the given type.
     */
    def newObject(pc: Int, t: ObjectType): DomainValue

    //
    //
    // GENERAL METHODS
    //
    //

    /**
     * Merges the two given memory layouts.
     *
     * @return The merged memory layout. Returns `NoUpdate` if this memory layout
     *      already subsumes the given memory layout.
     * @note The size of the operands stacks and the number of registers/locals
     *      has to be the same.
     * @note The operand stacks have to contain compatible values. I.e., it has to be
     *      possible to merge operand stack values without getting a `NoLegalValue`.
     *      In the latter case – i.e. if the result of the merging of two operand
     *      stacks is a `NoLegalValue` – either the bytecode is invalid, which is
     *      extremely unlikely, or the implementation of the domain is incomplete.
     */
    def merge(
        pc: Int,
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
                var newOperands: List[DomainValue] = List.empty // during the update we build the operands stack in reverse order

                while (thisRemainingOperands.nonEmpty /* && both stacks contain the same number of elements */ ) {
                    val thisOperand = thisRemainingOperands.head
                    thisRemainingOperands = thisRemainingOperands.tail
                    val otherOperand = otherRemainingOperands.head
                    otherRemainingOperands = otherRemainingOperands.tail

                    val newOperand =
                        if (thisOperand == otherOperand) {
                            thisOperand
                        } else {
                            val updatedOperand = thisOperand.merge(pc, otherOperand)
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
                            if (thisLocal eq otherLocal /* <=> both are null*/ ) {
                                thisLocal
                            } else {
                                localsUpdated = localsUpdated &: MetaInformationUpdateType
                                TheNoLegalValue
                            }
                        } else if (thisLocal == otherLocal) {
                            thisLocal
                        } else {
                            val updatedLocal = thisLocal.merge(pc, otherLocal)
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


