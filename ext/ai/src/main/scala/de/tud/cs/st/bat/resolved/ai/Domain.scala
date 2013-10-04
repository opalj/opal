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
 * A domain is the fundamental abstraction mechanism in BATAI that contains all
 * information about a program's types and values and performs the computations with
 * respect to a domain's values. Customizing a domain is the fundamental mechanism
 * of adapting BATAI to one's needs.
 *
 * This trait defines the interface between the abstract interpretation framework (BATAI)
 * and some (user defined) domain. I.e., this interface defines all methods that
 * are needed by BATAI to perform an abstract interpretation. While it is perfectly
 * possible to implement a new domain by inheriting from this trait it is recommended
 * to first study the already implemented domains and to use one of them as a foundation.
 * To facilitate the usage of BATAI several classes/traits that implement parts of
 * the `Domain` trait are pre-defined and can be mixed in when needed.
 *
 * ==Control Flow==
 * BATAI controls the process of evaluating the code of a method, but requires a
 * domain to perform the actual computations of an instruction's result. E.g., to
 * calculate the result of adding two integer values, the comparison of two object
 * instances or the result of converting a long value to an int value.
 * Handling of instructions that manipulate the stack (e.g. `dup`), that move values
 * between the stack and the locals (e.g., `aload_X`) or that determine the control
 * flow is, however, completely embedded into BATAI.
 *
 * ==Thread Safety==
 * When every analyzed method is associated with a unique `Domain` instance and – given
 * that BATAI only uses one thread to analyze a given method at a time – no special care
 * has to be taken. However, if a domain needs to consult a domain which is associated with
 * a project as a whole, which we will refer to as "World" in BATAI, it is then the
 * responsibility of the domain to make sure that coordination with the world is thread
 * safe.
 *
 * @note BATAI assumes that conceptually every method/code block is associated
 *      with its own instance of a domain object.
 * @tparam I The type which is used to identify this domain's context. E.g., if a new
 *      object is created it may be associated with the instruction that created it and
 *      this domain's identifier.
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait Domain[+I] {

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
    // REPRESENTATION OF VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over a concrete operand stack value or a value stored in one of the local
     * variables.
     *
     * ==Use Of Value/Dependencies On Value==
     * In general, subclasses and users of a `Domain` should not have/declare
     * a direct dependency on `Value`. Instead they should use `DomainValue` as otherwise
     * extensibility of a `Domain` may be hampered or even be impossible. The only
     * exception are, of course, classes that directly inherit from this class.
     *
     * ==Extending Value==
     * If you directly extend this trait (i.e., in a subclass of the `Domain` trait
     * you write something like `trait Value extends super.Value`), make sure that
     * you also extend all classes/traits that inherit from this type
     * (this may require a deep mixin composition and that you refine the type
     * `DomainType` accordingly).
     * However, BATAI was designed such that extending this class should – in general
     * – not be necessary.
     *
     * Please note, that standard inheritance from this trait is always
     * supported and is the primary mechanism to model an abstract domain's lattice.
     * ''Extending Value'' in this case refers to a class that inherits from `Domain`
     * and that defines a new class `Value` that inherits from this class and where
     * we redefine `DomainValue`.
     */
    trait Value { this: DomainValue ⇒

        /**
         * The computational type of the value.
         *
         * The precise computational type is needed by BATAI to calculate, e.g., the effect
         * of generic stack manipulation instructions (e.g., `dup_...` and swap)
         * on the stack. This is required to calculate the jump targets of RET
         * instructions and to determine which values are actually copied by, e.g., the
         * `dup_XX` instructions.
         *
         * @note The computational type has to be precise/correct.
         */
        def computationalType: ComputationalType

        /**
         * Merges this value with the given value.
         *
         * This basically implements the join operator of complete lattices.
         *
         * ==Example==
         * For example, merging a `DomainValue` that represents the integer value 0
         * with a `DomainValue` that represents the integer value 1 may return a new
         * `DomainValue` that precisely captures the range or that captures all positive
         * integer values or just '''some integer value'''.
         *
         * ==Contract==
         * '''`this` value''' is always the value that was previously used by BATAI to
         * perform subsequent computations. Hence, if `this` value subsumes the given
         * value the result has to be either a `NoUpdate` or a `MetaInformationUpdate`.
         * In case that the given value subsumes `this` value, the result has to be
         * a `StructuralUpdate`. Hence, the merge operation is not commutative.
         *
         * The termination of the abstract interpretation directly depends on the fact
         * that at some point all values are fixed and don't change anymore. Hence,
         * it is important that the type of the update is only a
         * [[de.tud.cs.st.bat.resolved.ai.StructuralUpdate]] if the value has changed.
         * In other words, when two values are merged it has to be ensured that no
         * fall back occurs. E.g., if you merge the existing integer value 0 and
         * the given value 1 and the result would be 1, then it must be ensured that
         * a subsequent merge will never result in the value 0 again.
         *
         * ==Merging Of Incompatible Values==
         * If this value is incompatible with the given value the result has
         * to be an `IllegalValue`. This may happen, e.g., when BATAI tries to
         * merge two register values/locals that are not live (i.e., which should not be
         * live) and, hence, are actually allowed to contain incompatible values.
         * (`Not live` means that the value will not be used in the future.)
         *
         * It is the responsibility of the domain to check that the given value is
         * compatible with the current value and – if not – to return the value
         * `MetaInformationUpdateIllegalValue`.
         *
         * @param pc The program counter of the instruction where the paths converge.
         * @param value The "new" domain value.
         */
        def merge(pc: Int, value: DomainValue): Update[DomainValue]

        /**
         * Adapts this value to the given domain.
         *
         * The `adapt` method is BATAIs main mechanism to enable dynamic domain-adaptation.
         * I.e., to make it possible to change the abstract domain at runtime if the
         * analysis time takes too long using a (more) precise domain.
         */
        def adapt[TDI >: I](targetDomain: Domain[TDI], pc: Int): targetDomain.DomainValue =
            domainException(
                Domain.this,
                "adapting this value for the target domain is not supported")

        private[Domain] def asReturnAddressValue: Int =
            BATException("this value cannot be converted to a return address")

    }

    /**
     * Abstracts over the concrete type of `Value`. Needs to be refined by traits that
     * inherit from `Domain` and which extend `Domain`'s `Value` trait.
     */
    type DomainValue <: Value

    /**
     * The class tag for the type `DomainValue`.
     *
     * Required by BATAI to generate instances of arrays in which values of type
     * `DomainValue` can be stored in a type-safe manner.
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
     * Represents a value that has no well defined state/type.
     *
     * @see [[de.tud.cs.st.bat.resolved.ai.Domain.Value]] for further details.
     */
    type ⊥ = IllegalValue
    protected class IllegalValue extends Value { this: DomainIllegalValue ⇒

        final def computationalType: ComputationalType =
            domainException(
                Domain.this,
                "a dead value does not have a computational type")

        final def merge(pc: Int, value: DomainValue): Update[DomainValue] =
            if (value == TheIllegalValue)
                NoUpdate
            else
                MetaInformationUpdateIllegalValue

        override def adapt[ThatI >: I](targetDomain: Domain[ThatI], pc: Int): targetDomain.DomainValue =
            targetDomain.TheIllegalValue

        override def toString: String = "IllegalValue"
    }

    /**
     * Abstracts over the concrete type of `IllegalValue`.
     *
     * This type needs to be refined whenever the class `IllegalValue`
     * is refined or the type `DomainValue` is refined.
     */
    type DomainIllegalValue <: IllegalValue with DomainValue

    /**
     * The ''singleton'' instance of a `IllegalValue`.
     */
    val TheIllegalValue: DomainIllegalValue
    final def ⊥ = TheIllegalValue

    /**
     * If the result of the merge of two values is a non-legal value. The result has
     * to be reported as a `MetaInformationUpdate`.
     */
    val MetaInformationUpdateIllegalValue: MetaInformationUpdate[DomainIllegalValue]

    /**
     * The result of merging two values should never be reported as a
     * `StructuralUpdate` if the computed value is an `IllegalValue`. The JVM semantics
     * guarantee that the value was not used in the first case and, hence, continuing
     * the interpretation is meaningless.
     *
     * @note This method is solely defined for documentation purposes and to catch
     *      implementation errors early on.
     */
    final def StructuralUpdateIllegalValue: StructuralUpdate[Nothing] =
        domainException(Domain.this,
            "merging of values with an incompatible value "+
                "always has to be a MetaInformationUpdate and not more")

    /**
     * Stores a single return address (i.e., a program counter/index into the code array).
     *
     * @note Though the framework completely handles all aspects related to return address
     *      values, it is nevertheless necessary that this class inherits from `Value`
     *      as return addresses are stored on the stack / in the registers. However,
     *      if the `Value` trait should be refined, all additional methods may – from
     *      the point-of-view of BATAI - just throw an `OperationNotSupportedException`
     *      as these additional methods will never be called by BATAI.
     */
    class ReturnAddressValue(
        val address: Int)
            extends Value { this: DomainReturnAddressValue ⇒

        final def computationalType: ComputationalType = ComputationalTypeReturnAddress

        final def merge(pc: Int, value: DomainValue): Update[DomainValue] =
            if (address == value.asReturnAddressValue)
                NoUpdate
            else
                BATException("return address values cannot be merged")

        private[Domain] override def asReturnAddressValue: Int = address

        override def adapt[ThatI >: I](targetDomain: Domain[ThatI], pc: Int): targetDomain.DomainValue =
            targetDomain.ReturnAddressValue(address)

        override def toString = "ReturnAddress: "+address
    }

    /**
     * Defines an extractor method to facilitate matching against return addresses.
     */
    object ReturnAddressValue {
        def unapply(retAddress: ReturnAddressValue): Option[Int] =
            Some(retAddress.address)
    }

    /**
     * Abstracts over the concrete type of `ReturnAddressValue`. Needs to be fixed
     * by some sub-trait/sub-class. In the simplest case (i.e., when neither the
     * `Value` trait nor the `ReturnAddressValue` trait was refined) it is sufficient
     * to write:
     * {{{
     * type DomainReturnAddressValue = ReturnAddressValue
     * }}}
     */
    type DomainReturnAddressValue <: ReturnAddressValue with DomainValue

    // -----------------------------------------------------------------------------------
    //
    // FACTORY METHODS TO CREATE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Factory method to create an instance of a `ReturnAddressValue`.
     */
    def ReturnAddressValue(address: Int): DomainReturnAddressValue

    /**
     * Factory method to create domain values with a specific type. I.e., values for
     * which we have some type information but no value or location information.
     *
     * For example, if `valueType` is a reference type it may be possible
     * that the actual value is `null`, but such knowledge is not available.
     *
     * BATAI uses this method when a method is to be analyzed, but no parameter
     * values are given and initial values need to be generated. This method is not
     * used elsewhere by BATAI.
     *
     * BATAI uses the pc -1 for the first parameter and -2 for the second...
     *
     * @note This method is primarily a convenience method.
     */
    def newTypedValue(pc: Int, valueType: Type): DomainValue = valueType match {
        case BooleanType       ⇒ newBooleanValue(pc)
        case ByteType          ⇒ newByteValue(pc)
        case ShortType         ⇒ newShortValue(pc)
        case CharType          ⇒ newCharValue(pc)
        case IntegerType       ⇒ newIntegerValue(pc)
        case FloatType         ⇒ newFloatValue(pc)
        case LongType          ⇒ newLongValue(pc)
        case DoubleType        ⇒ newDoubleValue(pc)
        case rt: ReferenceType ⇒ newReferenceValue(pc, rt)
        case VoidType          ⇒ domainException(this, "there are no void typed values")
    }

    /**
     * Factory method to create a representation of a boolean value if we now the
     * origin of the value.
     */
    def newBooleanValue(pc: Int): DomainValue

    /**
     * Factory method to create a representation of a boolean value with the given value.
     */
    def newBooleanValue(pc: Int, value: Boolean): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     */
    def newByteValue(pc: Int): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given byte value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def newByteValue(pc: Int, value: Byte): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     */
    def newShortValue(pc: Int): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given short value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def newShortValue(pc: Int, value: Short): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     */
    def newCharValue(pc: Int): DomainValue

    /**
     * Factory method to create a representation of the integer constant value 0.
     *
     * BATAI uses this special value for performing subsequent computations against
     * the fixed value 0 (e.g., for if_XX instructions). Hence, BATAI will never
     * push the returned value on the stack or store it in a local variable.
     */
    def newIntegerConstant0: DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     */
    def newIntegerValue(pc: Int): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given integer value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def newIntegerValue(pc: Int, value: Int): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     */
    def newFloatValue(pc: Int): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given float value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def newFloatValue(pc: Int, value: Float): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     */
    def newLongValue(pc: Int): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given long value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def newLongValue(pc: Int, value: Long): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     */
    def newDoubleValue(pc: Int): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given double value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def newDoubleValue(pc: Int, value: Double): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents a `null` value and
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def newNullValue(pc: Int): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents a new, '''initialized'''
     * object of the given type and that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     */
    def newReferenceValue(pc: Int, referenceType: ReferenceType): DomainValue

    /**
     * Factory method to create a new `DomainValue` that represents a new,
     * '''uninitialized''' instance of an object of the given type that was
     * created by the instruction with the specified program counter.
     *
     * BATAI calls this method when it evaluates `newobject` instructions.
     * If the bytecode is valid a call of one of the object's constructors will
     * subsequently initialize the object.
     *
     * @note Instances of arrays are created by the `newarray` and
     * 		`multianewarray` instructions and in both cases an exception may be thrown
     *   	(e.g., `NegativeArraySizeException`).
     */
    def newObject(pc: Int, objectType: ObjectType): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents a new, '''initialized'''
     * object of the given type and that was created (explicitly or implicitly) by the
     * instruction with the specified program counter.
     *
     * This method is used by BATAI to create reference values that are normally
     * internally created by the JVM (in particular exceptions such as
     * `NullPointExeception` and `ClassCastException`).
     */
    def newInitializedObject(pc: Int, objectType: ObjectType): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given string value
     * and that was created by the instruction with the specified program counter.
     */
    def newStringValue(pc: Int, value: String): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents a runtime value of
     * type "`Class&lt;T&gt;`" and that was created by the instruction with the
     * specified program counter.
     */
    def newClassValue(pc: Int, t: Type): DomainValue

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
     * all types that the domain value represents must belong to the same
     * computational type category. I.e., it is possible that the value captures the
     * types "`NullPointerException` and `IllegalArgumentException`", but it will never
     * capture – at the same time – the (Java) types `int` and/or `long`.
     */
    /*ABSTRACT*/ def types(value: DomainValue): TypesAnswer[_]

    /**
     * Tries to determine if the type referred to as `subtype` is a subtype of the
     * specified reference type `supertype`.
     */
    /*ABSTRACT*/ def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer

    /**
     * Tries to determine if the runtime type of the given reference value could be a
     * subtype of the specified reference type `supertype`. I.e., if the type of the
     * value is not precisely known then all subtypes of the values type are also
     * taken into consideration when analyzing the subtype relation and only if we
     * can guarantee that none is a subtype of the given `supertype` the answer will be
     * `No`.
     */
    /*ABSTRACT*/ def isSubtypeOf(
        value: DomainValue,
        supertype: ReferenceType,
        onNull: ⇒ Answer): Answer

    /**
     * Tests if the two given integer values are equal.
     *
     * @param value1 A value with computational type integer.
     * @param value2 A value with computational type integer.
     */
    /*ABSTRACT*/ def areEqual(value1: DomainValue, value2: DomainValue): Answer

    /**
     * Tests if the two given integer values are not equal.
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
        areEqual(value, newIntegerConstant0)

    /**
     * Tests if the given integer value is not 0 or maybe not 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def isNot0(value: DomainValue): Answer =
        areNotEqual(value, newIntegerConstant0)

    /**
     * Tests if the given integer value is &lt; 0 or maybe &lt; 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def isLessThan0(value: DomainValue): Answer =
        isLessThan(value, newIntegerConstant0)

    /**
     * Tests if the given integer value is less than or equal to 0 or maybe
     * less than or equal to 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def isLessThanOrEqualTo0(value: DomainValue): Answer =
        isLessThanOrEqualTo(value, newIntegerConstant0)

    /**
     * Tests if the given integer value is &gt; 0 or maybe &gt; 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def isGreaterThan0(value: DomainValue): Answer =
        isGreaterThan(value, newIntegerConstant0)

    /**
     * Tests if the given value is greater than or equal to 0 or maybe greater
     * than or equal to 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def isGreaterThanOrEqualTo0(value: DomainValue): Answer =
        isGreaterThanOrEqualTo(value, newIntegerConstant0)

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
    private[ai] val IsNull = establishIsNull _

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
    private[ai] val IsNonNull = establishIsNonNull _

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
    private[ai] val AreEqualReferences = establishAreEqualReferences _

    def establishAreNotEqualReferences(pc: Int,
                                       value1: DomainValue,
                                       value2: DomainValue,
                                       operands: Operands,
                                       locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] val AreNotEqualReferences = establishAreNotEqualReferences _

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
    private[ai] val AreEqual = establishAreEqual _

    def establishAreNotEqual(pc: Int,
                             value1: DomainValue,
                             value2: DomainValue,
                             operands: Operands,
                             locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] val AreNotEqual = establishAreNotEqual _

    def establishIsLessThan(pc: Int,
                            value1: DomainValue,
                            value2: DomainValue,
                            operands: Operands,
                            locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] val IsLessThan = establishIsLessThan _

    def establishIsLessThanOrEqualTo(pc: Int,
                                     value1: DomainValue,
                                     value2: DomainValue,
                                     operands: Operands,
                                     locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] val IsLessThanOrEqualTo = establishIsLessThanOrEqualTo _

    private[ai] val IsGreaterThan: TwoValuesConstraint =
        (pc: Int, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThan(pc, value2, value1, operands, locals)

    private[ai] val IsGreaterThanOrEqualTo: TwoValuesConstraint =
        (pc: Int, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThanOrEqualTo(pc, value2, value1, operands, locals)

    private[ai] val Is0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishAreEqual(pc, value, newIntegerConstant0, operands, locals)

    private[ai] val IsNot0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishAreNotEqual(pc, value, newIntegerConstant0, operands, locals)

    private[ai] val IsLessThan0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThan(pc, value, newIntegerConstant0, operands, locals)

    private[ai] val IsLessThanOrEqualTo0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThanOrEqualTo(pc, value, newIntegerConstant0, operands, locals)

    private[ai] val IsGreaterThan0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThan(pc, newIntegerConstant0, value, operands, locals)

    private[ai] val IsGreaterThanOrEqualTo0: SingleValueConstraint =
        (pc: Int, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThanOrEqualTo(pc, newIntegerConstant0, value, operands, locals)

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO INSTRUCTIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // HELPER TYPES AND FUNCTIONS RELATED TO THE RESULT OF INSTRUCTIONS
    //

    protected type SucceedsOrNullPointerException = Computation[Nothing, DomainValue]
    protected type OptionalReturnValueOrExceptions = Computation[Option[DomainValue], Set[DomainValue]]

    protected def sideEffectOnlyOrNullPointerException(
        pc: Int,
        value: DomainValue): SucceedsOrNullPointerException = {
        isNull(value) match {
            case Yes ⇒
                ThrowsException(newInitializedObject(pc, ObjectType.NullPointerException))
            case No ⇒
                ComputationWithSideEffectOnly
            case Unknown ⇒
                ComputationWithSideEffectOrException(
                    newInitializedObject(pc, ObjectType.NullPointerException)
                )
        }
    }

    //
    // METHODS TO IMPLEMENT THE SEMANTICS OF INSTRUCTIONS
    //

    //
    // CREATE ARRAY
    //

    /**
     * The return value is either a new array, a `NegativeArraySizeException`
     * or some linking exception.
     */
    def newarray(
        pc: Int,
        count: DomainValue,
        componentType: FieldType): Computation[DomainValue, DomainValue]

    def multianewarray(
        pc: Int,
        counts: List[DomainValue],
        arrayType: ArrayType): Computation[DomainValue, DomainValue]

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
    def arraylength(pc: Int, arrayref: DomainValue): Computation[DomainValue, DomainValue]

    //
    // TYPE CONVERSION
    //

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

    /**
     * Returns the field's value and/or a new `NullPointerException` if the given
     * `objectref` is `null`.
     *
     * @return The field's value or a new `NullPointerException`.
     */
    def getfield(pc: Int,
                 objectref: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType): Computation[DomainValue, DomainValue]

    /**
     * Returns the field's value and/or a new `LinkageException` if the specified
     * class is not found.
     *
     * @return The field's value or a new `LinkageException`.
     */
    def getstatic(pc: Int,
                  declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType): Computation[DomainValue, DomainValue]

    /**
     * Sets the fields values if the given `objectref` is not `null`.
     */
    def putfield(pc: Int,
                 objectref: DomainValue,
                 value: DomainValue,
                 declaringClass: ObjectType,
                 name: String,
                 fieldType: FieldType): Computation[Nothing, DomainValue]

    /**
     * Sets the fields values if the given class can be found.
     */
    def putstatic(pc: Int,
                  value: DomainValue,
                  declaringClass: ObjectType,
                  name: String,
                  fieldType: FieldType): Computation[Nothing, DomainValue]

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
    def iinc(pc: Int, value: DomainValue, increment: Int): DomainValue

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

    //
    //
    // GENERAL METHODS
    //
    //

    /**
     * Merges the given operand stacks and local variables.
     *
     * In general there should be no need to override this method.
     *
     * @return The merged operand stack and registers.
     *      Returns `NoUpdate` if this memory layout already subsumes the given memory
     *      layout.
     * @note The size of the operands stacks that are to be merged and the number of
     *      registers/locals that are to be merged can be expected to be identical
     *      under the assumption that the bytecode is valid and BATAI contains no
     *      bugs.
     * @note The operand stacks are guaranteed to contain compatible values w.r.t. the
     *      computational type (unless the bytecode is not valid or BATAI contains
     *      an error). I.e., if the result of merging two operand stack is an
     *      `IllegalValue` we assume that the domain implementation is incomplete.
     *      However, the merging of two register values can result in an illegal value.
     */
    def merge(
        pc: Int,
        thisOperands: Operands,
        thisLocals: Locals,
        otherOperands: Operands,
        otherLocals: Locals): Update[(Operands, Locals)] = {

        assume(thisOperands.size == otherOperands.size,
            "domain merge - different stack sizes: "+thisOperands+" <=> "+otherOperands)

        assume(thisLocals.size == otherLocals.size,
            "domain merge - different register sizes: "+thisLocals+" <=> "+otherLocals)

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
                        if (thisOperand eq otherOperand) {
                            thisOperand
                        } else {
                            val updatedOperand = thisOperand.merge(pc, otherOperand)
                            val newOperand = updatedOperand match {
                                case NoUpdate   ⇒ thisOperand
                                case someUpdate ⇒ someUpdate.value
                            }
                            assume(!newOperand.isInstanceOf[IllegalValue],
                                "merging of the operands "+thisOperand+" and "+otherOperand+" failed")
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
                    // The value calculated by "merge" may be the value "IllegalValue" 
                    // which means the values in the corresponding register were 
                    // different – w.r.t. its type – on the different paths. 
                    // Hence, the values are no longer useful.
                    // If we would have a liveness analysis, we could avoid the use of 
                    // "IllegalValue" and would avoid the useless merging of 
                    // incompatible values.
                    val newLocal =
                        if ((thisLocal eq null) || (otherLocal eq null)) {
                            if (thisLocal eq otherLocal /* <=> both are null*/ ) {
                                thisLocal
                            } else {
                                localsUpdated = localsUpdated &: MetaInformationUpdateType
                                TheIllegalValue
                            }
                        } else if (thisLocal eq otherLocal) {
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


