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
 * A domain is the fundamental abstraction mechanism in BATAI that enables the customization
 * of BATAI towards the needs of a specific analysis. A domain encodes the semantics of
 * computations (e.g., the addition of two values) with respect to a domain's values
 * (e.g., the representation of integer values). Customizing a domain is the
 * fundamental mechanism of adapting BATAI to one's needs.
 *
 * This trait defines the interface between the abstract interpretation framework (BATAI)
 * and some (user defined) domain. I.e., this interface defines all methods that
 * are needed by BATAI to perform an abstract interpretation. While it is perfectly
 * possible to implement a new domain by inheriting from this trait, it is recommended
 * to first study the already implemented domains and to use them as a foundation.
 * To facilitate the usage of BATAI several classes/traits that implement parts of
 * this `Domain` trait are pre-defined and can be flexibly combined (mixed together)
 * when needed.
 *
 * ==Control Flow==
 * BATAI controls the process of evaluating the code of a method, but requires a
 * domain to perform the actual computations of an instruction's result. E.g., to
 * calculate the result of adding two integer values, or to perform the comparison
 * of two object instances, or to get the result of converting a `long` value to an
 * `int` value.
 *
 * Handling of instructions that manipulate the stack (e.g. `dup`), that move values
 * between the stack and the locals (e.g., `Xload_Y`) or that determine the control
 * flow is, however, completely embedded into BATAI.
 *
 * ==Thread Safety==
 * When every analyzed method is associated with a unique `Domain` instance and – given
 * that BATAI only uses one thread to analyze a given method at a time – no special care
 * has to be taken. However, if a domain needs to consult another domain which is, e.g,
 * associated with a project as a whole, it is then the responsibility of the domain to
 * make sure that coordination with the world is thread safe.
 *
 * This trait does not have any state that is associated with the analysis of a method.
 *
 * ==Extending Domain==
 * When you extends this trait or implement parts of it you should keep as many methods/
 * fields private to facilitate mix-in composition of multiple traits.
 *
 * @note BATAI assumes that conceptually every method/code block is associated
 *      with its own instance of a domain object.
 * @tparam I The type which is used to identify this domain's context. E.g., if a new
 *      domain is created it may be associated with the instruction that created it and
 *      this domain's identifier.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait Domain[+I] {

    /**
     * Returns the value that identifies this domain (usually it is loosely
     * connected to the analyzed method).
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
     * '''In general, subclasses and users of a `Domain` should not have/declare
     * a direct dependency on `Value`'''. Instead they should use `DomainValue` as otherwise
     * extensibility of a `Domain` may be hampered or even be impossible. The only
     * exception are, of course, classes that directly inherit from this class.
     *
     * ==Extending Value==
     * If you directly extend/refine this trait (i.e., in a subclass of the `Domain` trait
     * you write something like `trait Value extends super.Value`), make sure that
     * you also extend all classes/traits that inherit from this type
     * (this may require a deep mixin composition and that you refine the type
     * `DomainType` accordingly).
     * However, BATAI was designed such that extending this class should – in general
     * – not be necessary. It may also be easier to encode the desired semantics – as
     * far as possible – as part of the domain.
     *
     * Please note, that standard inheritance from this trait is always
     * supported and is the primary mechanism to model an abstract domain's lattice
     * w.r.t. some special type of value.
     *
     * @note BATAI does not rely on any special equality semantics w.r.t. values and
     *      never directly or indirectly calls a `Value`'s `equals` method. Hence,
     *      a domain can encode equality such that it best fits its need.
     */
    trait Value { this: DomainValue ⇒

        /**
         * The computational type of the value.
         *
         * The precise computational type is needed by BATAI to calculate the effect
         * of generic stack manipulation instructions (e.g., `dup_...` and swap)
         * on the stack as well as to calculate the jump targets of `RET`
         * instructions and to determine which values are actually copied by, e.g., the
         * `dup_XX` instructions.
         *
         * @note The computational type has to be precise/correct.
         */
        def computationalType: ComputationalType

        @throws[DomainException]("This method is not supported.")
        private[ai] def asReturnAddressValue: PC =
            throw new DomainException("this value ("+this+") is not a return address")

        /**
         * Joins this value and the given value.
         *
         * This basically implements the join operator of complete lattices.
         *
         * Join is called whenever two control-flow paths join and, hence, the values
         * found on the paths need to be joined. This method is called by BATAI whenever
         * two '''intra-procedural''' control-flow paths join and the two values are
         * are two different objects (`(this ne value) == true`).
         *
         * ==Example==
         * For example, joining a `DomainValue` that represents the integer value 0
         * with a `DomainValue` that represents the integer value 1 may return a new
         * `DomainValue` that precisely captures the range [0..'''1'''] or that captures
         * '''all positive''' integer values or just '''some integer value'''.
         *
         * ==Contract==
         * '''`this` value''' is always the value that was previously used by BATAI to
         * perform subsequent computations/analyses. Hence, if `this` value subsumes
         * the given value, the result has to be either `NoUpdate` or a `MetaInformationUpdate`.
         * In case that the given value subsumes `this` value, the result has to be
         * a `StructuralUpdate`. Hence, '''this `join` operation is not commutative'''.
         * If the result is a `StructuralUpdate` BATAI will continue with the
         * interpretation.
         *
         * The termination of the abstract interpretation directly depends on the fact
         * that at some point all values are fixed and don't change anymore. Hence,
         * it is important that '''the type of the update is only a
         * [[de.tud.cs.st.bat.resolved.ai.StructuralUpdate]] if the value has changed in
         * a way relevant for future computations/analyses''' involving this value.
         * In other words, when two values are joined it has to be ensured that no
         * fall back to a previous value occurs. E.g., if you join the existing integer
         * value 0 and the given value 1 and the result would be 1, then it must be
         * ensured that a subsequent join with the value 0 will not result in the value
         * 0 again.
         *
         * Conceptually, the join of an object with itself has to return the object
         * itself. Note, that this is a conceptual requirement as such a call
         * (`this.doJoin(..,this)`) will not be done by BATAI.
         *
         * ==Performance==
         * In general, the domain should try to minimize the number of objects that it
         * uses to represent values. That is, two values that are conceptually equal
         * should – whenever possible – use only one object. This has a significant
         * impact on functions such as `join`.
         *
         * @param pc The program counter of the instruction where the paths converge.
         * @param value The "new" domain value with which this domain value should be
         *      joined.
         *      '''The given `value` and this value are guaranteed to have
         *      the same computational type, but that they are two different objects.'''
         */
        protected def doJoin(pc: PC, value: DomainValue): Update[DomainValue]

        /**
         * Checks that the given value and this value are compatible and – if so –
         * calls `doJoin(PC,DomainValue)`.
         *
         * See `doJoin(..)` for details.
         *
         * @note It is generally not recommended/needed to override this method.
         *
         * @param pc The program counter of the instruction where the paths converge.
         * @param value The "new" domain value with which this domain value should be
         *      joined. The given value and `this` value are guaranteed to be two different
         *      objects.
         */
        def join(pc: PC, that: DomainValue): Update[DomainValue] = {
            if ((that eq TheIllegalValue) ||
                (this.computationalType ne that.computationalType))
                MetaInformationUpdateIllegalValue
            else
                doJoin(pc, that)
        }

        //
        // METHODS THAT ARE PREDEFINED BECAUSE THEY ARE GENERALLY USEFUL WHEN
        // ANALYZING PROJECTS, BUT WHICH ARE NOT REQUIRED BY BATAI! 
        // I.E. THESE METHODS ARE USED - IF AT ALL - BY THE DOMAIN.
        //

        /**
         * Creates a summary of this value.
         *
         * In general, creating a summary of a value may be useful/required
         * for values that are potentially returned by a called method and which
         * will then be used by the calling method. For example,
         * it may be useful to precisely track the flow of values within a method to
         * be able to distinguish between all sources of a value (E.g., to be able to
         * distinguish between a `NullPointerException` created by instruction A and another
         * one created by instruction B (`A != B`).) However, from the caller perspective
         * it may be absolutely irrelevant where/how the value was created in the called
         * method and, hence, keeping all information would just waste memory and
         * a summary may be sufficient.
         *
         * @note __The precise semantics and usage of `summarize(...)` is determined
         *      by the domain as BATAI does not use/call this method.__ This method
         *      is solely predefined to facilitate the development of project-wide
         *      analyses.
         */
        def summarize(pc: PC): DomainValue

        /**
         * Adapts this value to the given domain (default: throws a domain exception
         * that adaptation is not supported). '''This method needs to be overridden
         * by concrete `Value` classes to support the adaptation for a specific domain.'''
         *
         * Supporting the `adapt` method is primarily necessary when you want to
         * analyze a method that is called by the currently analyzed method
         * and you need to adapt this domain's values (the parameters of the method)
         * to the domain used for analyzing the called method.
         *
         * Additionally, the `adapt` method is BATAI's main mechanism to enable dynamic
         * domain-adaptation. I.e., to make it possible to change the abstract domain at
         * runtime if the analysis time takes too long using a (more) precise domain.
         *
         * @note __The precise semantics of `adapt` can be determined by the domain
         *      as BATAI does not use/call this method.__
         */
        @throws[DomainException]("Adaptation of this value is not supported.")
        def adapt[TDI >: I](target: Domain[TDI], pc: PC): target.DomainValue =
            throw new DomainException("This value "+this+" cannot be adapted for "+target)
    }

    /**
     * Abstracts over the concrete type of `Value`. Needs to be refined by traits that
     * inherit from `Domain` and which extend `Domain`'s `Value` trait.
     */
    type DomainValue <: Value

    /**
     * An instruction's operands are represented using a list where the first
     * element of the list represents the top level operand stack value.
     */
    type Operands = List[DomainValue]
    /**
     * An instruction's current register values/locals are represented using an array.
     */
    type Locals = Array[DomainValue]

    /**
     * A simple type alias of the type `DomainValue`.
     * Used to facilitate comprehension.
     */
    type ExceptionValue = DomainValue

    /**
     * A type alias for `Iterable`s of `ExceptionValue`s.
     * Primarily used to facilitate comprehension.
     */
    type ExceptionValues = Iterable[ExceptionValue]

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
     * Represents a value that has no well defined state/type. Such values are
     * the result of a join of two incompatible values and are generally only found in
     * registers (in the locals) and then identify a value that is dead.
     *
     * @see [[de.tud.cs.st.bat.resolved.ai.Domain.Value]] for further details.
     */
    protected class IllegalValue extends Value { this: DomainIllegalValue ⇒

        @throws[DomainException]("This method is not supported.")
        final override def computationalType: ComputationalType =
            throw DomainException("the illegal value has no computational type")

        @throws[DomainException]("This method is not supported.")
        override protected def doJoin(pc: PC, other: DomainValue): Update[DomainValue] =
            throw DomainException("this method is not supported")

        override def join(pc: PC, other: DomainValue): Update[DomainValue] =
            if (other eq TheIllegalValue)
                NoUpdate
            else
                MetaInformationUpdateIllegalValue

        @throws[DomainException]("This method is not supported.")
        override def summarize(pc: PC): DomainValue =
            throw DomainException("creating a summary of an illegal value is meaningless")

        override def adapt[TDI >: I](target: Domain[TDI], pc: PC): target.DomainValue =
            target.TheIllegalValue

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
     * The '''singleton''' instance of the `IllegalValue`.
     */
    val TheIllegalValue: DomainIllegalValue

    /**
     * The result of the merge of two incompatible values has
     * to be reported as a `MetaInformationUpdate`.
     */
    def MetaInformationUpdateIllegalValue: MetaInformationUpdate[DomainIllegalValue]

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
        throw new DomainException(
            "implementation error (see documentation of Domain.StructuralUpdateIllegalValue())"
        )

    /**
     * Stores a single return address (i.e., a program counter/index into the code array).
     *
     * @note Though the framework completely handles all aspects related to return address
     *      values, it is nevertheless necessary that this class inherits from `Value`
     *      as return addresses are stored on the stack/in the registers. However,
     *      if the `Value` trait should be refined, all additional methods may – from
     *      the point-of-view of BATAI - just throw an `OperationNotSupportedException`
     *      as these additional methods will never be called by BATAI.
     */
    class ReturnAddressValue(
        val address: PC)
            extends Value { this: DomainReturnAddressValue ⇒

        private[ai] final override def asReturnAddressValue: Int = address

        final override def computationalType: ComputationalType =
            ComputationalTypeReturnAddress

        @throws[DomainException]("This method is not supported.")
        override protected def doJoin(pc: PC, other: DomainValue): Update[DomainValue] =
            // Note that the framework already handles the case where this 
            // value is joined with itself! A join of this value with a different return 
            // address value does not make sense!
            throw DomainException("return address values cannot be joined")

        @throws[DomainException]("This method is not supported.")
        override def summarize(pc: PC): DomainValue =
            throw DomainException("summarizing return address values is meaningless")

        override def adapt[TDI >: I](target: Domain[TDI], pc: PC): target.DomainValue =
            target.ReturnAddressValue(address)

        override def toString = "ReturnAddress("+address+")"
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

    /**
     * Factory method to create an instance of a `ReturnAddressValue`.
     */
    def ReturnAddressValue(address: PC): DomainReturnAddressValue

    // -----------------------------------------------------------------------------------
    //
    // FACTORY METHODS TO CREATE GENERAL VALUES
    //
    // -----------------------------------------------------------------------------------

    final def justThrows(value: ExceptionValue): ThrowsException[ExceptionValues] =
        ThrowsException(Seq(value))

    final def throws(value: ExceptionValue): ThrowsException[ExceptionValue] =
        ThrowsException(value)

    def ClassCastException(pc: PC): ExceptionValue =
        InitializedObjectValue(pc, ObjectType.ClassCastException)

    def NullPointerException(pc: PC): ExceptionValue =
        InitializedObjectValue(pc, ObjectType.NullPointerException)

    def NegativeArraySizeException(pc: PC): ExceptionValue =
        InitializedObjectValue(pc, ObjectType.NegativeArraySizeException)

    def ArrayIndexOutOfBoundsException(pc: PC): ExceptionValue =
        InitializedObjectValue(pc, ObjectType.ArrayIndexOutOfBoundsException)

    def ArrayStoreException(pc: PC): ExceptionValue =
        InitializedObjectValue(pc, ObjectType.ArrayStoreException)

    /**
     * Factory method to create domain values with a specific type. I.e., values for
     * which we have some type information but no value or location information.
     * However, the value is guaranteed to be proper initialized.
     *
     * For example, if `valueType` is a reference type it may be possible
     * that the actual value is `null`, but such knowledge is not available.
     *
     * BATAI uses this method when a method is to be analyzed, but no parameter
     * values are given and initial values need to be generated. This method is not
     * used elsewhere by BATAI.
     *
     * BATAI assigns the `pc` "-1" to the first parameter and -2 for the second... This
     * property is, however, not ensured by this method.
     */
    def TypedValue(pc: PC, valueType: Type): DomainValue = valueType match {
        case BooleanType       ⇒ BooleanValue(pc)
        case ByteType          ⇒ ByteValue(pc)
        case ShortType         ⇒ ShortValue(pc)
        case CharType          ⇒ CharValue(pc)
        case IntegerType       ⇒ IntegerValue(pc)
        case FloatType         ⇒ FloatValue(pc)
        case LongType          ⇒ LongValue(pc)
        case DoubleType        ⇒ DoubleValue(pc)
        case rt: ReferenceType ⇒ ReferenceValue(pc, rt)
        case VoidType ⇒
            throw DomainException("a domain value cannot have the type void")
    }

    /**
     * Factory method to create a representation of a boolean value if we know the
     * origin of the value.
     *
     * The domain may ignore the information about the origin (`pc`).
     */
    def BooleanValue(pc: PC): DomainValue

    /**
     * Factory method to create a representation of a boolean value with the given
     * initial value and origin.
     *
     * The domain may ignore the information about the value and the origin (`pc`).
     */
    def BooleanValue(pc: PC, value: Boolean): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`pc`).
     */
    def ByteValue(pc: PC): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given byte value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`pc`).
     */
    def ByteValue(pc: PC, value: Byte): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`pc`).
     */
    def ShortValue(pc: PC): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given short value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def ShortValue(pc: PC, value: Short): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`pc`).
     */
    def CharValue(pc: PC): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given char value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def CharValue(pc: PC, value: Char): DomainValue

    /**
     * Factory method to create a representation of the integer constant value 0.
     *
     * (The program counter  (`pc`) that should be assigned with the value (if any)
     * should be Int.MinValue to signify that this value was not created by the program.)
     *
     * BATAI in particular uses this special value for performing subsequent
     * computations against the fixed value 0 (e.g., for if_XX instructions).
     *
     * The domain may ignore the information about the value.
     */
    def IntegerConstant0: DomainValue = IntegerValue(Int.MinValue, 0)

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`pc`).
     */
    def IntegerValue(pc: PC): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given integer value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`pc`).
     */
    def IntegerValue(pc: PC, value: Int): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`pc`).
     */
    def FloatValue(pc: PC): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given float value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`pc`).
     */
    def FloatValue(pc: PC, value: Float): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`pc`).
     */
    def LongValue(pc: PC): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given long value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`pc`).
     */
    def LongValue(pc: PC, value: Long): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`pc`).
     */
    def DoubleValue(pc: PC): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given double value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`pc`).
     */
    def DoubleValue(pc: PC, value: Double): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents a `null` value and
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`pc`).
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: N/A
     *  - Type: '''Null'''
     *  - Null: '''Yes'''
     */
    def NullValue(pc: PC): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents ''either a reference
     * value that has the given type and is initialized or the value `null`''. However, the
     * information whether the value is `null` or not is not available. Furthermore, the
     * type may also just be an upper bound.
     *
     * The domain may ignore the information about the value and the origin (`pc`), but
     * it has to remain possible for the domain to identify the component type of an
     * array.
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: '''yes''' (if non-null then the constructor was called)
     *  - Type: '''Upper Bound'''
     *  - Null: '''Unknown'''
     *  - Content: '''Unknown'''
     */
    def ReferenceValue(pc: PC, referenceType: ReferenceType): DomainValue

    /**
     * Represents ''a non-null reference value with the given type as an upper type bound''.
     *
     * The domain may ignore the information about the value and the origin (pc).
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes''' (the constructor was called)
     *  - Type: '''Upper Bound'''
     *  - Null: '''No''' (This value is not `null`.)
     */
    def NonNullObjectValue(pc: PC, objectType: ObjectType): DomainValue

    /**
     * Creates a new `DomainValue` that represents ''a new,
     * uninitialized instance of an object of the given type''. The object was
     * created by the (`NEW`) instruction with the specified program counter.
     *
     * BATAI calls this method when it evaluates `newobject` instructions.
     * If the bytecode is valid a call of one of the object's constructors will
     * subsequently initialize the object.
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''no''' (only the memory is allocated for the object)
     *  - Type: '''precise''' (i.e., this type is not an upper bound,
     *      the type correctly models the runtime type.)
     *  - Null: '''No''' (This value is not `null`.)
     *
     * @note Instances of arrays are created by the `newarray` and
     *      `multianewarray` instructions and in both cases an exception may be thrown
     *      (e.g., `NegativeArraySizeException`).
     */
    def NewObject(pc: PC, objectType: ObjectType): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents an '''initialized'''
     * reference value of the given type and that was created (explicitly or implicitly)
     * by the instruction with the specified program counter.
     *
     * ==General Remarks==
     * The given type usually identifies a class type (not an interface type) that is
     * not abstract, but in some cases (e.g. consider `java.awt.Toolkit()`)
     * it may be useful/meaningful to relax this requirement and to state that the
     * class precisely represents the runtime type – even
     * so the class is abstract. However, such decisions need to be made by the domain.
     *
     * This method is used by BATAI to create reference values that are normally
     * internally created by the JVM (in particular exceptions such as
     * `NullPointExeception` and `ClassCastException`). However, it can generally
     * be used to create initialized objects/arrays.
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes'''
     *  - Type: '''precise''' (i.e., this type is not an upper bound, the type
     *      correctly models the runtime type.)
     *  - Null: '''No''' (This value is not `null`.)
     */
    def InitializedObjectValue(pc: PC, objectType: ObjectType): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given string value
     * and that was created by the instruction with the specified program counter.
     *
     * This function is called by BATAI when a string constant (`LDC(_W)` instruction) is
     * put on the stack.
     *
     * The domain may ignore the information about the value and the origin (pc).
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes''' and the String's value is the given value. The string
     *      may be empty (""), but it is never `null`.
     *  - Type: '''java.lang.String'''
     *  - Null: '''No'''
     *
     * @param value A non-null string. (The string may be empty, though.)
     */
    def StringValue(pc: PC, value: String): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents a runtime value of
     * type "`Class&lt;T&gt;`" and that was created by the instruction with the
     * specified program counter.
     *
     * This function is called by BATAI when a class constant (`LDC(_W)` instruction) is
     * put on the stack.
     *
     * The domain may ignore the information about the value and the origin (pc).
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes''' and the type represented by the class is the given type.
     *  - Type: '''java.lang.Class<t:Type>'''
     *  - Null: '''No'''
     */
    def ClassValue(pc: PC, t: Type): DomainValue

    // -----------------------------------------------------------------------------------
    //
    // QUESTION'S ABOUT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Returns `Yes` or `Unknown` iff at least one possible extension of the given
     * `value` is in the specified range; that is if the intersection of the range of
     * values captured by the given `value` and the specified range is non-empty.
     *
     * For example, if the given value captures all positive integer values and the
     * specified range is [-1,1] then the answer has to be `Yes`. If we know nothing
     * about the potential extension of the given value the answer will be `Unknown`.
     *
     * @param value A value that has to be of computational type integer.
     * @param lowerBound The range's lower bound (inclusive).
     * @param upperBound The range's upper bound (inclusive).
     */
    /*ABSTRACT*/ def isSomeValueInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer

    /**
     * Returns `Yes` or Unknown` iff at least one (possible) extension of given value is not in the
     * specified range; that is, if the set difference of the range of values captured
     * by the given `value` and  the specified range is non-empty.
     * For example, if the given `value` has the integer value `10` and the
     * specified range is [0,Integer.MAX_VALUE] then the answer has to be `No`. But,
     * if the given `value` represents the range [-5,Integer.MAX_VALUE] and the specified
     * range is again [0,Integer.MAX_VALUE] then the answer has to be `Yes` `Unknown`.
     *
     * The answer is Yes iff the analysis determined that at runtime `value`  will have
     * a value that is not in the specified range. If the analysis(domain) is not able
     * to determine whether the value is or is not in the given range then the answer
     * has to be unknown.
     *
     * @param value A value that has to be of computational type integer.
     * @param lowerBound The range's lower bound (inclusive).
     * @param upperBound The range's upper bound (inclusive).
     */
    /*ABSTRACT*/ def isSomeValueNotInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer

    /**
     * Determines whether the given value is `null` (`Yes`), maybe `null` (`Unknown`) or
     * is not `null` (`No`).
     *
     * @param value A value of computational type reference.
     */
    /*ABSTRACT*/ def isNull(value: DomainValue): Answer

    final private[ai] def isNonNull(value: DomainValue): Answer = isNull(value).negate

    /**
     * Compares the given values for reference equality. Returns `Yes` if both values
     * point to the '''same instance''' and returns `No` if both objects are known not to
     * point to the same instance. The latter is, e.g., trivially the case when both
     * values have a different concrete type. Otherwise `Unknown` is returned.
     *
     * @param value1 A value of computational type reference.
     * @param value2 A value of computational type reference.
     */
    /*ABSTRACT*/ def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer

    final private[ai] def areNotEqualReferences(
        value1: DomainValue,
        value2: DomainValue): Answer =
        areEqualReferences(value1, value2).negate

    /**
     * Returns the type(type bounds) of the given value.
     * In general a single value can have multiple type bounds which depend on the
     * control flow.
     * However, all types that the value represents must belong to the same
     * computational type category. I.e., it is possible that the value either has the
     * type "`NullPointerException` or `IllegalArgumentException`", but it will never have
     * – at the same time – the (Java) types `int` and `long`. Furthermore,
     * it is possible that the returned type(s) is(are) only an upper bound of the
     * real type.
     *
     * This default implementation always returns
     * [[de.tud.cs.st.bat.resolved.ai.TypeUnknown]].
     *
     * ==Implementing `typeOfValue`==
     * This method is typically not implemented by a single `Domain` trait/object, but is
     * instead implemented collaboratively by all domains that implement the semantics
     * of certain values. To achieve that, other `Domain` traits that implement a
     * concrete domain's semantics have to `abstract override` this method and only
     * return the value's type if the domain knows anything about the type. If a method
     * that overrides this method has no knowledge about the given value, it should
     * delegate this call to its super method.
     *
     * '''Example'''
     * {{{
     * trait FloatValues extends Domain[...] {
     *   ...
     *     abstract override def typeOfValue(value: DomainValue): TypesAnswer =
     *     value match {
     *       case r: FloatValue ⇒ IsFloatValue
     *       case _             ⇒ super.typeOfValue(value)
     *     }
     * }
     * }}}
     */
    def typeOfValue(value: DomainValue): TypesAnswer =
        value match {
            case ta: TypesAnswer ⇒ ta
            case _               ⇒ TypeUnknown
        }

    /**
     * Tries to determine if the type referred to as `subtype` is a subtype of the
     * specified reference type `supertype`.
     */
    /*ABSTRACT*/ def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer

    /**
     * Tries to determine – under the assumption that the given `value` is not `null` – 
     * if the runtime type of the given reference value could be a
     * subtype of the specified reference type `supertype`. I.e., if the type of the
     * value is not precisely known, then all subtypes of the value's type are also
     * taken into consideration when analyzing the subtype relation and only if we
     * can guarantee that none is a subtype of the given `supertype` the answer will be
     * `No`.
     *
     * @note The returned value is only meaningful if the value does not represent
     *      the runtime value `null`.
     */
    /*ABSTRACT*/ def isValueSubtypeOf(
        value: DomainValue,
        supertype: ReferenceType): Answer

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
    final private[ai] def areNotEqual(
        value1: DomainValue,
        value2: DomainValue): Answer =
        areEqual(value1, value2).negate

    /**
     * Tests if the first integer value is smaller than the second value.
     *
     * @param smallerValue A value with computational type integer.
     * @param largerValue A value with computational type integer.
     */
    /*ABSTRACT*/ def isLessThan(
        smallerValue: DomainValue,
        largerValue: DomainValue): Answer

    /**
     * Tests if the first integer value is less than or equal to the second value.
     *
     * @param smallerOrEqualValue A value with computational type integer.
     * @param equalOrLargerValue A value with computational type integer.
     */
    /*ABSTRACT*/ def isLessThanOrEqualTo(
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue: DomainValue): Answer

    /**
     * Tests if the first integer value is larger than the second value.
     *
     * @param largerValue A value with computational type integer.
     * @param smallerValue A value with computational type integer.
     */
    final private[ai] def isGreaterThan(
        largerValue: DomainValue,
        smallerValue: DomainValue): Answer =
        isLessThan(smallerValue, largerValue)

    /**
     * Tests if the first integer value is larger than or equal to the second value.
     *
     * @param largerOrEqualValue A value with computational type integer.
     * @param smallerOrEqualValue A value with computational type integer.
     */
    final private[ai] def isGreaterThanOrEqualTo(
        largerOrEqualValue: DomainValue,
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

    //
    // W.r.t Reference Values
    /**
     * Called by BATAI when the value is known to be `null`/has to be `null`.
     * E.g., after a comparison with `null` (IFNULL/IFNONNULL) BATAI knows that the
     * value has to be `null` on one branch and that the value is not `null` on the
     * other branch.
     */
    def establishIsNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] final def IsNull = establishIsNull _

    /**
     * Called by BATAI when it establishes that the value is guaranteed not to be `null`.
     * E.g., after a comparison with `null` BATAI can establish that the
     * value has to be `null` on one branch and that the value is not `null` on the
     * other branch.
     */
    def establishIsNonNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] final def IsNonNull = establishIsNonNull _

    /**
     * Called by BATAI when two values were compared for reference equality and
     * we are currently analyzing the branch where the comparison succeeded.
     */
    def establishAreEqualReferences(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] final def AreEqualReferences = establishAreEqualReferences _

    def establishAreNotEqualReferences(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] final def AreNotEqualReferences = establishAreNotEqualReferences _

    /**
     * Called by BATAI to inform the domain that the given type is a new additional upper
     * bound of the given value that the value is guaranteed to satisfy from here on.
     *
     * This method is called iff a subtype query (typeOf(value) <: bound) returned
     * `Unknown`.
     */
    def establishUpperBound(
        pc: PC,
        bound: ReferenceType,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (operands, locals)

    //
    // W.r.t. Integer values

    def establishValue(
        pc: PC,
        theValue: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (operands, locals)

    def establishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] final def AreEqual = establishAreEqual _

    def establishAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] final def AreNotEqual = establishAreNotEqual _

    def establishIsLessThan(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] final def IsLessThan = establishIsLessThan _

    def establishIsLessThanOrEqualTo(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (operands, locals)
    private[ai] final def IsLessThanOrEqualTo = establishIsLessThanOrEqualTo _

    /**
     * A function that takes a program counter (`PC`), a value, the current operands
     * and the register assignment and updates the operands and the register
     * assignment w.r.t. the given value and the modeled constraint.
     */
    private[ai]type SingleValueConstraint = ((PC, DomainValue, Operands, Locals) ⇒ (Operands, Locals))

    /**
     * A function that takes a program counter (`PC`), two values, the current operands
     * and the register assignment and updates the operands and the register
     * assignment w.r.t. the given values and the modeled constraint.
     */
    private[ai]type TwoValuesConstraint = ((PC, DomainValue, DomainValue, Operands, Locals) ⇒ (Operands, Locals))

    private[ai] final def IsGreaterThan: TwoValuesConstraint =
        (pc: PC, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThan(pc, value2, value1, operands, locals)

    private[ai] final def IsGreaterThanOrEqualTo: TwoValuesConstraint =
        (pc: PC, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThanOrEqualTo(pc, value2, value1, operands, locals)

    private[ai] final def Is0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishAreEqual(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IsNot0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishAreNotEqual(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IsLessThan0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThan(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IsLessThanOrEqualTo0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThanOrEqualTo(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IsGreaterThan0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThan(pc, IntegerConstant0, value, operands, locals)

    private[ai] final def IsGreaterThanOrEqualTo0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            establishIsLessThanOrEqualTo(pc, IntegerConstant0, value, operands, locals)

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO INSTRUCTIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // METHODS TO IMPLEMENT THE SEMANTICS OF INSTRUCTIONS
    //

    //
    // CREATE ARRAY
    //

    /**
     * The return value is either a new array or a `NegativeArraySizeException` if
     * count is negative.
     */
    def newarray(
        pc: PC,
        count: DomainValue,
        componentType: FieldType): Computation[DomainValue, ExceptionValue]

    /**
     * The return value is either a new array or a `NegativeArraySizeException` if
     * count is negative.
     */
    def multianewarray(
        pc: PC,
        counts: List[DomainValue],
        arrayType: ArrayType): Computation[DomainValue, ExceptionValue]

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //

    /**
     * Computation that returns the value stored in an array at a given index or an
     * exception. The exceptions that may be thrown are: `NullPointerException` and
     * `ArrayIndexOutOfBoundsException`.
     */
    type ArrayLoadResult = Computation[DomainValue, ExceptionValues]
    /**
     * Computation that succeeds (updates the value stored in the array at the given
     * index) or that throws an exception. The exceptions that may be thrown are:
     * `NullPointerException`, `ArrayIndexOutOfBoundsException` and `ArrayStoreException`.
     */
    type ArrayStoreResult = Computation[Nothing, ExceptionValues]

    //
    // STORING VALUES IN AND LOADING VALUES FROM ARRAYS
    //

    def aaload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def aastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def baload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def bastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def caload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def castore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def daload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def dastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def faload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def fastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def iaload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def iastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def laload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def lastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def saload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def sastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    //
    // LENGTH OF AN ARRAY
    //

    /**
     * Returns the array's length or throws a `NullPointerException`.
     */
    def arraylength(
        pc: PC,
        arrayref: DomainValue): Computation[DomainValue, ExceptionValue]

    //
    // TYPE CONVERSION
    //

    def d2f(pc: PC, value: DomainValue): DomainValue
    def d2i(pc: PC, value: DomainValue): DomainValue
    def d2l(pc: PC, value: DomainValue): DomainValue

    def f2d(pc: PC, value: DomainValue): DomainValue
    def f2i(pc: PC, value: DomainValue): DomainValue
    def f2l(pc: PC, value: DomainValue): DomainValue

    def i2b(pc: PC, value: DomainValue): DomainValue
    def i2c(pc: PC, value: DomainValue): DomainValue
    def i2d(pc: PC, value: DomainValue): DomainValue
    def i2f(pc: PC, value: DomainValue): DomainValue
    def i2l(pc: PC, value: DomainValue): DomainValue
    def i2s(pc: PC, value: DomainValue): DomainValue

    def l2d(pc: PC, value: DomainValue): DomainValue
    def l2f(pc: PC, value: DomainValue): DomainValue
    def l2i(pc: PC, value: DomainValue): DomainValue

    //
    // RETURN FROM METHOD
    //

    /**
     * The given `value`, which is a value with ''computational type reference'', is returned
     * by the return instruction with the given `pc`.
     */
    def areturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type double'', is returned
     * by the return instruction with the given `pc`.
     */
    def dreturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type float'', is returned
     * by the return instruction with the given `pc`.
     */
    def freturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type integer'', is returned
     * by the return instruction with the given `pc`.
     */
    def ireturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type long'', is returned
     * by the return instruction with the given `pc`.
     */
    def lreturn(pc: PC, value: DomainValue): Unit

    /**
     * Called by BATAI when a return instruction with the given `pc` is reached.
     * In other words, when the method returns normally.
     */
    def returnVoid(pc: PC): Unit

    /**
     * Called by BATAI when an exception is thrown that is not (guaranteed to be) handled
     * within the same method.
     *
     * @note If the original exception value is `null` (`/*E.g.*/throw null;`), then
     *      the exception that is actually thrown is a new `NullPointerException`. This
     *      situation is, however, completely handled by BATAI and the exception
     *      is hence never `null`.
     */
    def abruptMethodExecution(pc: PC, exception: DomainValue): Unit

    //
    // ACCESSING FIELDS
    //

    /**
     * Returns the field's value and/or a new `NullPointerException` if the given
     * `objectref` represents the value `null`.
     *
     * @return The field's value or a new `NullPointerException`.
     */
    def getfield(
        pc: PC,
        objectref: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, ExceptionValue]

    /**
     * Returns the field's value.
     *
     * @return The field's value or a new `LinkageException`.
     */
    def getstatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, Nothing]

    /**
     * Sets the fields values if the given `objectref` is not `null`.
     */
    def putfield(
        pc: PC,
        objectref: DomainValue,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, ExceptionValue]

    /**
     * Sets the fields values if the given class can be found.
     */
    def putstatic(
        pc: PC,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, Nothing]

    //
    // METHOD INVOCATIONS
    //
    protected type MethodCallResult = Computation[Option[DomainValue], ExceptionValues]

    def invokedynamic(
        pc: PC,
        bootstrapMethod: BootstrapMethod,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): Computation[DomainValue, ExceptionValues]

    def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType, // e.g., Array[] x = ...; x.clone()
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult

    def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult

    def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult

    def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def fcmpl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def dcmpg(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def dcmpl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def lcmp(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    //
    // UNARY EXPRESSIONS
    //
    def fneg(pc: PC, value: DomainValue): DomainValue
    def dneg(pc: PC, value: DomainValue): DomainValue
    def lneg(pc: PC, value: DomainValue): DomainValue
    def ineg(pc: PC, value: DomainValue): DomainValue

    //
    // BINARY EXPRESSIONS
    //

    /**
     * Computation that returns a numeric value or an `ObjectType.ArithmeticException`.
     */
    type IntegerLikeValueOrArithmeticException = Computation[DomainValue, ExceptionValue]

    def dadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def ddiv(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def dmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def drem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def dsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    def fadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def fdiv(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def fmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def frem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def fsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def idiv(pc: PC, value1: DomainValue, value2: DomainValue): IntegerLikeValueOrArithmeticException
    def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def irem(pc: PC, value1: DomainValue, value2: DomainValue): IntegerLikeValueOrArithmeticException
    def ishl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def ishr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def isub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def iushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def iinc(pc: PC, value: DomainValue, increment: Int): DomainValue

    def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def ldiv(pc: PC, value1: DomainValue, value2: DomainValue): IntegerLikeValueOrArithmeticException
    def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def lrem(pc: PC, value1: DomainValue, value2: DomainValue): IntegerLikeValueOrArithmeticException
    def lshl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def lshr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def lsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def lushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    //
    // "OTHER" INSTRUCTIONS
    //

    /**
     * Handles a `monitorenter` instruction.
     */
    def monitorenter(pc: PC, value: DomainValue): Computation[Nothing, ExceptionValue]

    /**
     * Handles a `monitorenter` instruction.
     */
    def monitorexit(pc: PC, value: DomainValue): Computation[Nothing, ExceptionValue]

    //
    //
    // GENERAL METHODS
    //
    //

    /**
     * Merges the given value v1 with the value v2.
     *
     * This operation is commutative.
     */
    def mergeDomainValues(pc: PC, v1: DomainValue, v2: DomainValue): DomainValue = {
        v1.join(pc, v2) match {
            case NoUpdate      ⇒ v1
            case SomeUpdate(v) ⇒ v
        }
    }

    /**
     * Joins the given operand stacks and local variables.
     *
     * In general there should be no need to override this method.
     *
     * @return The joined operand stack and registers.
     *      Returns `NoUpdate` if this memory layout already subsumes the given memory
     *      layout.
     * @note The size of the operands stacks that are to be joined and the number of
     *      registers/locals that are to be joined can be expected to be identical
     *      under the assumption that the bytecode is valid and BATAI contains no
     *      bugs.
     * @note The operand stacks are guaranteed to contain compatible values w.r.t. the
     *      computational type (unless the bytecode is not valid or BATAI contains
     *      an error). I.e., if the result of joining two operand stack values is an
     *      `IllegalValue` we assume that the domain implementation is incomplete.
     *      However, the joining of two register values can result in an illegal value.
     */
    def join(
        pc: PC,
        thisOperands: Operands,
        thisLocals: Locals,
        otherOperands: Operands,
        otherLocals: Locals): Update[(Operands, Locals)] = {

        // These internal tests should no longer be necessary!
        // assume(thisOperands.size == otherOperands.size,
        //      "domain join - different stack sizes: "+thisOperands+" <=> "+otherOperands)
        //
        // assume(thisLocals.size == otherLocals.size,
        //      "domain join - different register sizes: "+thisLocals+" <=> "+otherLocals)

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
                            val updatedOperand = thisOperand.join(pc, otherOperand)
                            val newOperand = updatedOperand match {
                                case NoUpdate   ⇒ thisOperand
                                case someUpdate ⇒ someUpdate.value
                            }
                            assume(newOperand ne TheIllegalValue,
                                "an operand stack value must never be an illegal value")
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
                    // The value calculated by "join" may be the value "IllegalValue" 
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
                            val updatedLocal = thisLocal.join(pc, otherLocal)
                            if (updatedLocal eq NoUpdate) {
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

    /**
     * '''Called by BATAI after performing a computation'''; that is, after
     * evaluating the effect of the instruction with `currentPC` on the stack and register.
     * This function basically informs the domain about the instruction that
     * will be evaluated in the future. The flow function is called for every possible
     * successor of the instruction with the `currentPC`. In some cases it may even be
     * the case that the flow function is called multiple times with the same pair
     * of program counters: (`currentPC`, `successorPC`). This may happen, e.g., in case
     * of a switch instruction where multiple values have the same body/target instruction.
     * E.g., as in the following snippet:
     * {{{
     * switch (i) {
     * case 1:
     * case 2:
     * case 3: System.out.println("Great.");
     * default: System.out.println("Not So Great.");
     * }
     * }}}
     * The flow function is also called after instructions that are domain independent
     * such as `dup` and `xLoad` instructions which ''just'' manipulate the registers
     * and stack in a generic way.
     * This enables the domain to precisely follow the evaluation
     * progress and in particular to perform control-flow dependent analyses.
     *
     * @param currentPC The program counter of the instruction that is evaluated.
     * @param successorPC The program counter of an instruction that is a potential
     *      successor of the instruction with `currentPC`. If the had of the
     *      given `worklist` is not `successorPC` the abstract interpreter did
     *      not (again) schedule the evaluation of the instruction with `successorPC`.
     *      This means that the instruction was evaluated in the past and that
     *      the abstract did not change in a way that a reevaluation is (from the point
     *      of view of the AI) not necessary.
     * @param operandsArray The array that associates every instruction with its
     *      operand stack that is in effect.  Note, that only those elements of the
     *      array contain values that are related to instructions. The other elements
     *      are `null`.
     * @param localsArray The array that associates every instruction with its current
     *      register values. Note, that only those elements of the
     *      array contain values that are related to instructions. The other elements
     *      are `null`.
     * @param worklist The current list of instructions that will be evaluated next.
     * @return The updated worklist. In most cases this is simply the given `worklist`.
     *      However, if you want to force the evaluation of the instruction
     *      with the pc `successorPC`, it is sufficient to test whether the list already
     *      contains `successorPC` and – if not – to prepend it. If the worklist
     *      already contains `successorPC`, the domain is always allowed to move
     *      the PC to the beginning of the worklist.
     *      If the domain updates the worklist, it is the responsibility of the domain
     *      to call the tracer and to inform it about the changes.
     *      Note that the worklist is not allowed to contain duplicates.
     * @note The domain is allowed to modify the `worklist`, `operandsArray` and
     *      `localsArray. However, the AI will not check that all constraints
     *      are satisfied.
     */
    def flow(
        currentPC: PC,
        successorPC: PC,
        operandsArray: Array[List[DomainValue]],
        localsArray: Array[Array[DomainValue]],
        worklist: List[PC],
        tracer: Option[AITracer]): List[PC] = worklist

    /**
     * Creates a summary of the given domain values. For the precise details
     * regarding the calculation of a summary see `Value.summarize(...)`.
     *
     * @param pc The program counter that will be used for the summary value if
     *      a new value is returned that abstracts over/summarizes the given values.
     * @param values An `Iterable` over one or more values.
     *
     * @note The current algorithm is very generic and should satisfy most needs, but
     * 		it is also not very efficient. However, it should be easy to tailor it for a
     *   	specific domain, if need be.
     */
    def summarize(pc: PC, values: Iterable[DomainValue]): DomainValue = {
        var summary = values.head.summarize(pc)
        values.tail foreach { value ⇒
            summary.join(pc, value.summarize(pc)) match {
                case NoUpdate ⇒ /*nothing to do*/
                case SomeUpdate(newSummary) ⇒
                    summary = newSummary.summarize(pc)
            }
        }
        summary
    }

    /**
     * Returns a string representation of the properties associated with
     * the instruction with the respective program counter.
     *
     * Associating properties with an instruction and maintaining those properties
     * is, however, at the sole responsibility of the `Domain`.
     *
     * This method is predefined to facilitate the development of support tools
     * and is not called by BATAI.
     */
    def properties(pc: PC): Option[String] = None
}
