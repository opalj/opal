/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package org.opalj
package ai

import scala.reflect.ClassTag

import org.opalj.util.{ Answer, Yes, No, Unknown }

import br._

/**
 * A domain is the fundamental abstraction mechanism in OPAL that enables the customization
 * of the abstract interpretation framework towards the needs of a specific analysis.
 *
 * A domain encodes the semantics of computations (e.g., the addition of two values)
 * with respect to a domain's values (e.g., the representation of integer values).
 * Customizing a domain is the fundamental mechanism of adapting the AI framework
 * to one's needs.
 *
 * This trait defines the interface between the abstract interpretation framework
 * and some (user defined) domain. I.e., this interface defines all methods that
 * are needed by OPAL to perform an abstract interpretation.
 *
 * ==Control Flow==
 * OPAL controls the process of evaluating the code of a method, but requires a
 * domain to perform the actual computations of an instruction's result. E.g., to
 * calculate the result of adding two integer values, or to perform the comparison
 * of two object instances, or to get the result of converting a `long` value to an
 * `int` value the framework always consults the domain.
 *
 * Handling of instructions that manipulate the stack (e.g. `dup`), that move values
 * between the stack and the locals (e.g., `Xload_Y`) or that determine the control
 * flow is, however, completely embedded into OPAL-AI.
 *
 * OPAL uses the following three methods to inform a domain about the progress of the
 * abstract interpretation:
 *  - [[org.opalj.ai.Domain.flow]]
 *  - [[org.opalj.ai.Domain.evaluationCompleted]]
 *  - [[org.opalj.ai.Domain.abstractInterpretationEnded]]
 * A domain that implements (`overrides`) one of these methods should always also delegate
 * the call to its superclass to make sure that every domain interested in these
 * events is informed.
 *
 * ==Implementing Abstract Domains==
 * While it is perfectly possible to implement a new domain by inheriting from this
 * trait, it is recommended  to first study the already implemented domains and to
 * use them as a foundation.
 * To facilitate the usage of OPAL several classes/traits that implement parts of
 * this `Domain` trait are pre-defined and can be flexibly combined (mixed together)
 * when needed.
 *
 * When you extend this trait or implement parts of it you should keep as many methods/
 * fields private to facilitate mix-in composition of multiple traits.
 *
 * ==Thread Safety==
 * When every analyzed method is associated with a unique `Domain` instance and – given
 * that OPAL only uses one thread to analyze a given method at a time – no special care
 * has to be taken. However, if a domain needs to consult another domain which is, e.g,
 * associated with a project as a whole, it is then the responsibility of the domain to
 * make sure that coordination with the world is thread safe.
 *
 * @note OPAL assumes that – at least conceptually – every method/code block is associated
 *      with its own instance of a domain object.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait Domain {

    /**
     * The type which is used to identify the domain's context.
     * E.g., if a new domain is created to analyze a called method it may be
     * associated with the instruction caused its creation. It can, however,
     * also just identify the method (by means of, e.g., the pair `(classFile,method)`
     * that it is used for.
     */
    type Id

    /**
     * Returns the value that identifies this domain (usually it is loosely
     * connected to the analyzed method).
     *
     * This value may subsequently be used to identify/track object instances but – if
     * so – this happens at the sole responsibility of the domain. OPAL-AI does
     * not require any kind of tracking.
     */
    def id: Id

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
     * exceptions are, of course, classes that directly inherit from this class.
     *
     * ==Refining Value==
     * If you directly extend/refine this trait (i.e., in a subclass of the `Domain` trait
     * you write something like `trait Value extends super.Value`), make sure that
     * you also extend all classes/traits that inherit from this type
     * (this may require a deep mixin composition and that you refine the type
     * `DomainType` accordingly).
     * However, OPAL was designed such that extending this class should – in general
     * – not be necessary. It may also be easier to encode the desired semantics – as
     * far as possible – as part of the domain.
     *
     * ==Implementing Value==
     * Standard inheritance from this trait is always
     * supported and is the primary mechanism to model an abstract domain's lattice
     * w.r.t. some special type of value. In general, the implementation should try
     * to avoid creating new instances of values unless strictly required to model the
     * domain's semantics. This will greatly improve the overall performance as this
     * framework heavily uses reference-based equality checks to speed up the evaluation.
     *
     * @note OPAL does not rely on any special equality semantics w.r.t. values and
     *      never directly or indirectly calls a `Value`'s `equals` or `eq` method. Hence,
     *      a domain can encode equality such that it best fits its need.
     *      However, some of the provided domains rely on the following semantics for equals:
     *      '''Two domain values have to be equal (`==`) iff they represent the same
     *      information. This includes additional information, such as, the value of
     *      the origin.'''
     *      E.g., a value (`AnIntegerValue`) that represents an arbitrary `Integer` value
     *      has to return `true` if the domain value with which it is compared also
     *      represents an arbitrary `Integer` value (`AnIntegerValue`). However,
     *      it may still be necessary to use multiple objects to represent an arbitrary
     *      integer value if, e.g., constraints should be attached to specific values.
     *      For example, after a comparison of an integer value with a predefined
     *      value (e.g., `AnIntegerValue < 4`) it is possible to constrain the respective
     *      value on the subsequent paths (< 4 on one path and >= 4 on the other path).
     *      To make that possible, it is however necessary to distinguish the
     *      `AnIntegervalue` from some other `AnIntegerValue` to avoid constraining
     *      unrelated values.
     *      {{{
     *      public void foo(int a,int b) {
     *          if(a < 4) {
     *              z = a - 2 // here a is constrained (< 4), b and z are unconstrained
     *          }
     *          else {
     *              z = a + 2 // here a is constrained (>= 4), b and z are unconstrained
     *          }
     *      }
     *      }}}
     *
     *      In general, `equals` is only defined for values belonging to the same
     *      domain. If values need to be compared across domains, they need to be adapted
     *      to a target domain first.
     */
    trait Value { this: DomainValue ⇒

        /**
         * The computational type of the value.
         *
         * The precise computational type is needed by OPAL to calculate the effect
         * of generic stack manipulation instructions (e.g., `dup_...` and swap)
         * on the stack as well as to calculate the jump targets of `RET`
         * instructions and to determine which values are actually copied by, e.g., the
         * `dup_XX` instructions.
         *
         * @note The computational type has to be precise/correct.
         */
        def computationalType: ComputationalType

        // only used by the abstract interpretation framework 
        // and implemented only by ReturnAddressValue
        @throws[DomainException]("This method is not supported.")
        private[ai] def asReturnAddressValue: PC =
            throw new DomainException("this value ("+this+") is not a return address")

        /**
         * Joins this value and the given value.
         *
         * This basically implements the join operator of complete lattices.
         *
         * Join is called whenever an instruction is evaluated more than once and, hence,
         * the values found on the paths need to be joined. This method is, however,
         * only called if the two values are two different objects
         * (`(this ne value) == true`), but both values have the same computational type.
         *
         * ==Example==
         * For example, joining a `DomainValue` that represents the integer value 0
         * with a `DomainValue` that represents the integer value 1 may return a new
         * `DomainValue` that precisely captures the range [0..'''1'''] or that captures
         * '''all positive''' integer values or just '''some integer value'''.
         *
         * ==Contract==
         * '''`this` value''' is always the value that was previously used to
         * perform subsequent computations/analyses. Hence, if `this` value subsumes
         * the given value, the result has to be either `NoUpdate` or a `MetaInformationUpdate`.
         * In case that the given value subsumes `this` value, the result has to be
         * a `StructuralUpdate` with the given value as the new value. Hence,
         * '''this `join` operation is not commutative'''. If a new (more abstract)
         * abstract value is created that represents both values the result always has to
         * be a `StructuralUpdate`.
         * If the result is a `StructuralUpdate` the framework will continue with the
         * interpretation.
         *
         * The termination of the abstract interpretation directly depends on the fact
         * that at some point all values are fixed and don't change anymore. Hence,
         * it is important that '''the type of the update is only a
         * [[org.opalj.ai.StructuralUpdate]] if the value has changed in
         * a way relevant for future computations/analyses''' involving this value.
         * In other words, when two values are joined it has to be ensured that no
         * fall back to a previous value occurs. E.g., if you join the existing integer
         * value 0 and the given value 1 and the result would be 1, then it must be
         * ensured that a subsequent join with the value 0 will not result in the value
         * 0 again.
         *
         * Conceptually, the join of an object with itself has to return the object
         * itself. Note, that this is a conceptual requirement as such a call
         * (`this.doJoin(..,this)`) will not be done by the abstract interpreter.
         *
         * ==Performance==
         * In general, the domain should try to minimize the number of objects that it
         * uses to represent values. That is, '''two values that are conceptually equal
         * should – whenever possible – use only one object'''. This has a significant
         * impact on functions such as `join`.
         *
         * @param pc The program counter of the instruction where the paths converge.
         * @param value The "new" domain value with which this domain value should be
         *      joined.
         *      '''The given `value` and this value are guaranteed to have
         *      the same computational type, but are not reference equal.'''
         */
        protected def doJoin(pc: PC, value: DomainValue): Update[DomainValue]

        /**
         * Checks that the given value and this value are compatible and – if so –
         * calls `doJoin(PC,DomainValue)`.
         *
         * See `doJoin(PC,DomainValue)` for details.
         *
         * @note It is in general not recommended/needed to override this method.
         *
         * @param pc The program counter of the instruction where the paths converge.
         * @param value The "new" domain value with which this domain value should be
         *      joined. The caller has to ensure that the given value and `this` value
         *      are guaranteed to be two different objects.
         * @return [[MetaInformationUpdateIllegalValue]]
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
        // ANALYZING PROJECTS, BUT WHICH ARE NOT REQUIRED BY THIS FRAMEWORK. 
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
         * @note The framework (the classes directly in org.opalj.ai) does not
         *      use/call this method.
         *      This method is solely predefined to facilitate the development of
         *      project-wide analyses.
         */
        def summarize(pc: PC): DomainValue

        /**
         * Adapts this value to the given domain (default: throws a domain exception
         * that adaptation is not supported). '''This method needs to be overridden
         * by concrete `Value` classes to support the adaptation for a specific domain.'''
         *
         * Supporting the `adapt` method is primarily necessary when you want to
         * analyze a method that is called by the currently analyzed method
         * and you need to adapt this domain's values (the actual parameters of the method)
         * to the domain used for analyzing the called method.
         *
         * Additionally, the `adapt` method is OPAL's main mechanism to enable dynamic
         * domain-adaptation. I.e., to make it possible to change the abstract domain at
         * runtime if the analysis time takes too long using a (more) precise domain.
         *
         * @note The framework does not use/call this method. This method
         *      is solely predefined to facilitate the development of project-wide
         *      analyses.
         */
        @throws[DomainException]("Adaptation of this value is not supported.")
        def adapt(target: Domain, vo: ValueOrigin): target.DomainValue =
            throw new DomainException("adaptation of "+this+" to "+target+" is unsupported")

        /**
         * Returns `true` iff the abstract state represented by this value
         * is at least as abstract as the abstract state of the given value. I.e.,
         * this method is '''not reflexive'''.
         *
         * The abstract state generally encompasses every information that would
         * be considered during a [[join]] of `this` value and the `other` value and that
         * could lead to a [[StructuralUpdate]].
         *
         * ==Implementation==
         * The default implementation relies on this domain value's [[join]] method.
         *
         * Overriding this method is, hence, primarily meaningful for performance reasons.
         */
        def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            val result = this.join(Int.MinValue /*Irrelevant*/ , other)
            result.isNoUpdate ||
                (result.isMetaInformationUpdate &&
                    (result ne MetaInformationUpdateIllegalValue)
                )
        }
    }

    /**
     * Abstracts over the concrete type of `Value`. Needs to be refined by traits that
     * inherit from `Domain` and which extend `Domain`'s `Value` trait.
     */
    type DomainValue >: Null <: Value with AnyRef

    private[ai] val Null: DomainValue = null

    /**
     * An instruction's operands are represented using a list where the first
     * element of the list represents the top level operand stack value.
     */
    type Operands = org.opalj.ai.Operands[DomainValue] // the full package name is required by unidoc

    type OperandsArray = org.opalj.ai.TheOperandsArray[Operands] // the full package name is required by unidoc

    /**
     * An instruction's current register values/locals are represented using an array.
     */
    type Locals = org.opalj.ai.Locals[DomainValue] // the full package name is required by unidoc

    type LocalsArray = org.opalj.ai.TheLocalsArray[Locals] // the full package name is required by unidoc

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
     * Required by OPAL-AI to generate instances of arrays in which values of type
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
     * @see [[org.opalj.ai.Domain.Value]] for further details.
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

        override def adapt(target: Domain, vo: ValueOrigin): target.DomainValue =
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
     * to be reported as a `MetaInformationUpdate[DomainIllegalValue]`.
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
     *      the point-of-view of OPAL-AI - just throw an `OperationNotSupportedException`
     *      as these additional methods will never be called by OPAL-AI.
     */
    class ReturnAddressValue(
        val address: PC)
            extends Value { this: DomainReturnAddressValue ⇒

        private[ai] final override def asReturnAddressValue: Int = address

        final override def computationalType: ComputationalType =
            ComputationalTypeReturnAddress

        @throws[DomainException]("Return address values cannot be joined.")
        override protected def doJoin(pc: PC, other: DomainValue): Update[DomainValue] = {
            // Note that "Value" already handles the case where this 
            // value is joined with itself. Furthermore, a join of this value with a 
            // different return address value either indicates a serious bug
            // in the framework/the analysis or that the byte code is invalid!
            throw DomainException(
                "return address values (this="+this.toString+"#"+System.identityHashCode(this)+") "+
                    "cannot be joined (other="+other.toString+"#"+System.identityHashCode(other)+") "+
                    "(If this exception occurs make sure that the analyzed bytecode is valid; "+
                    "in particular check that a subroutine never invokes itself "+
                    "(recursive subroutine calls are not allowed).)"
            )
        }

        @throws[DomainException]("Summarizing return address values is meaningless.")
        override def summarize(pc: PC): DomainValue =
            throw DomainException("summarizing return address values is meaningless")

        // Adaptation is supported to support on-the-fly domain up-/downcasts.
        override def adapt(target: Domain, vo: ValueOrigin): target.DomainValue =
            target.ReturnAddressValue(address)

        override def toString = "ReturnAddress("+address+")"

        override def hashCode = address
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

    /**
     * Called by the AI framework for each load constant method handle instruction to
     * get a representation of/a DomainValue that represents the handle.
     *
     * @param handle A valid method handle.
     * @return An `InitializedObjectValue(ObjectType.MethodHandle)`.
     * 		Hence, this method needs to be overridden
     * 		if resolution of MethodHandle based method calls should be performed.
     */
    def MethodHandle(pc: PC, handle: MethodHandle): DomainValue =
        InitializedObjectValue(pc, ObjectType.MethodHandle)

    /**
     * Called by the AI framework for each load constant method type instruction to
     * get a domain-specific representation of the method descriptor as a `MethodType`.
     *
     * @param handle A valid method descriptor.
     * @return An `InitializedObjectValue(ObjectType.MethodType)`.
     * 		Hence, this method needs to be overridden
     * 		if resolution of MethodType based method calls should be performed.
     */
    def MethodType(pc: PC, descriptor: MethodDescriptor): DomainValue =
        InitializedObjectValue(pc, ObjectType.MethodType)

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

    def ArithmeticException(pc: PC): ExceptionValue =
        InitializedObjectValue(pc, ObjectType.ArithmeticException)

    /**
     * Factory method to create domain values with a specific type. I.e., values for
     * which we have some type information but no value or source information.
     * However, the value is guaranteed to be proper initialized (if non-null).
     *
     * For example, if `valueType` is a reference type it may be possible
     * that the actual value is `null`, but such knowledge is not available.
     *
     * The framework uses this method when a method is to be analyzed, but no parameter
     * values are given and initial values need to be generated. This method is not
     * used elsewhere by the framework.
     */
    def TypedValue(vo: ValueOrigin, valueType: Type): DomainValue = valueType match {
        case BooleanType       ⇒ BooleanValue(vo)
        case ByteType          ⇒ ByteValue(vo)
        case ShortType         ⇒ ShortValue(vo)
        case CharType          ⇒ CharValue(vo)
        case IntegerType       ⇒ IntegerValue(vo)
        case FloatType         ⇒ FloatValue(vo)
        case LongType          ⇒ LongValue(vo)
        case DoubleType        ⇒ DoubleValue(vo)
        case rt: ReferenceType ⇒ ReferenceValue(vo, rt)
        case VoidType ⇒
            throw DomainException("a domain value cannot have the type void")
    }

    /**
     * Factory method to create a representation of a boolean value if we know the
     * origin of the value.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def BooleanValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a representation of a boolean value with the given
     * initial value and origin.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def BooleanValue(vo: ValueOrigin, value: Boolean): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def ByteValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given byte value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def ByteValue(vo: ValueOrigin, value: Byte): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def ShortValue(vo: ValueOrigin): DomainValue

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
     * The domain may ignore the information about the origin (`vo`).
     */
    def CharValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given char value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def CharValue(vo: ValueOrigin, value: Char): DomainValue

    /**
     * Factory method to create a representation of the integer constant value 0.
     *
     * OPAL in particular uses this special value for performing subsequent
     * computations against the fixed value 0 (e.g., for if_XX instructions).
     *
     * (The origin ([[ValueOrigin]]) that should be used should be the
     * [[ConstantValueOrigin]] to signify that this value was not created by the program.)
     *
     * The domain may ignore the information about the value.
     */
    def IntegerConstant0: DomainValue = IntegerValue(ConstantValueOrigin, 0)

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def IntegerValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given integer value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def IntegerValue(vo: ValueOrigin, value: Int): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def FloatValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given float value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def FloatValue(vo: ValueOrigin, value: Float): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def LongValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given long value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def LongValue(vo: ValueOrigin, value: Long): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def DoubleValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given double value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def DoubleValue(vo: ValueOrigin, value: Double): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the `null` value and
     * and that was created (explicitly or implicitly) by the instruction (`aconst_null`)
     * with the specified program counter.
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
    def NullValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents ''either a reference
     * value that has the given type and is initialized or the value `null`''. However, the
     * information whether the value is `null` or not is not available. Furthermore, the
     * type may also just be an upper bound.
     *
     * The domain may ignore the information about the value and the origin (`vo`), but
     * it has to remain possible for the domain to identify the component type of an
     * array.
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: '''Yes''' (if non-null the constructor was called)
     *  - Type: '''Upper Bound'''
     *  - Null: '''Unknown'''
     *  - Content: '''Unknown'''
     */
    def ReferenceValue(vo: ValueOrigin, referenceType: ReferenceType): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents ''an array''
     * that was successfully created and which has the given type.
     *
     * The domain may ignore the information about the origin (`pc`) and
     * the precise size of each dimension.
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes'''
     *  - Type: '''Precise'''
     *  - Null: '''No'''
     *  - Content: '''Unknown'''
     *
     * @param vo Information about the origin of the value.
     * @param counts The size of each dimension if available. `counts` may be empty (`Nil`)
     * 		if no corresponding information is available; however, if available the
     *   	following condition always has to hold: `counts.length <= arrayType.dimensions`.
     */
    def InitializedArrayValue(
        vo: ValueOrigin,
        counts: List[Int],
        arrayType: ArrayType): DomainValue

    /**
     * Represents ''a non-null reference value with the given type as an upper type bound''.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes''' (the constructor was called)
     *  - Type: '''Upper Bound'''
     *  - Null: '''No''' (This value is not `null`.)
     */
    def NonNullObjectValue(vo: ValueOrigin, objectType: ObjectType): DomainValue

    /**
     * Creates a new `DomainValue` that represents ''a new,
     * uninitialized instance of an object of the given type''. The object was
     * created by the (`NEW`) instruction with the specified program counter.
     *
     * OPAL calls this method when it evaluates `newobject` instructions.
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
    def NewObject(vo: ValueOrigin, objectType: ObjectType): DomainValue

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
     * This method is used by the OPAL framework to create reference values that are normally
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
    def InitializedObjectValue(vo: ValueOrigin, objectType: ObjectType): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given string value
     * and that was created by the instruction with the specified program counter.
     *
     * This function is called by OPAL-AI when a string constant (`LDC(_W)` instruction) is
     * put on the stack.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
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
    def StringValue(vo: ValueOrigin, value: String): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents a runtime value of
     * type "`Class&lt;T&gt;`" and that was created by the instruction with the
     * specified program counter.
     *
     * This function is called by OPAL when a class constant (`LDC(_W)` instruction) is
     * put on the stack.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes''' and the type represented by the class is the given type.
     *  - Type: '''java.lang.Class<t:Type>'''
     *  - Null: '''No'''
     */
    def ClassValue(vo: ValueOrigin, t: Type): DomainValue

    // -----------------------------------------------------------------------------------
    //
    // QUESTION'S ABOUT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Returns the type(type bounds) of the given value.
     *
     * In general a single value can have multiple type bounds which depend on the
     * control flow.
     * However, all types that the value represents must belong to the same
     * computational type category. I.e., it is possible that the value either has the
     * type "`NullPointerException` or `IllegalArgumentException`", but it will never have
     * – at the same time – the (Java) types `int` and `long`. Furthermore,
     * it is possible that the returned type(s) is(are) only an upper bound of the
     * real type unless the type is a primitive type.
     *
     * This default implementation always returns
     * [[org.opalj.ai.TypeUnknown]].
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
     * value is not precisely known, then all subtypes of the `value`'s type are also
     * taken into consideration when analyzing the subtype relation and only if we
     * can guarantee that none is a subtype of the given `supertype` the answer will be
     * `No`.
     *
     * @note The returned value is only meaningful if `value` does not represent
     *      the runtime value `null`.
     */
    /*ABSTRACT*/ def isValueSubtypeOf(
        value: DomainValue,
        supertype: ReferenceType): Answer

    /**
     * Determines whether the given value is `null` (`Yes`), maybe `null` (`Unknown`) or
     * is not `null` (`No`).
     *
     * @param value A value of computational type reference.
     */
    /*ABSTRACT*/ def refIsNull(value: DomainValue): Answer

    final private[ai] def refIsNonNull(value: DomainValue): Answer =
        refIsNull(value).negate

    /**
     * Compares the given values for reference equality. Returns `Yes` if both values
     * point to the '''same instance''' and returns `No` if both objects are known not to
     * point to the same instance. The latter is, e.g., trivially the case when both
     * values have a different concrete type. Otherwise `Unknown` is returned.
     *
     * @param value1 A value of computational type reference.
     * @param value2 A value of computational type reference.
     */
    /*ABSTRACT*/ def refAreEqual(value1: DomainValue, value2: DomainValue): Answer

    final private[ai] def refAreNotEqual(
        value1: DomainValue,
        value2: DomainValue): Answer =
        refAreEqual(value1, value2).negate

    /**
     * Returns `Yes` or `Unknown` iff at least one possible extension of the given
     * `value` is in the specified range; that is, if the intersection of the range of
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
    /*ABSTRACT*/ def intIsSomeValueInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer

    /**
     * Returns `Yes` or Unknown` iff at least one (possible) extension of given value is
     * not in the specified range; that is, if the set difference of the range of
     * values captured by the given `value` and  the specified range is non-empty.
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
    /*ABSTRACT*/ def intIsSomeValueNotInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer

    /**
     * Tests if the two given integer values are equal.
     *
     * @param value1 A value with computational type integer.
     * @param value2 A value with computational type integer.
     */
    /*ABSTRACT*/ def intAreEqual(value1: DomainValue, value2: DomainValue): Answer

    /**
     * Tests if the two given integer values are not equal.
     *
     * @param value1 A value with computational type integer.
     * @param value2 A value with computational type integer.
     */
    final private[ai] def intAreNotEqual(
        value1: DomainValue,
        value2: DomainValue): Answer =
        intAreEqual(value1, value2).negate

    /**
     * Tests if the first integer value is smaller than the second value.
     *
     * @param smallerValue A value with computational type integer.
     * @param largerValue A value with computational type integer.
     */
    /*ABSTRACT*/ def intIsLessThan(
        smallerValue: DomainValue,
        largerValue: DomainValue): Answer

    /**
     * Tests if the first integer value is less than or equal to the second value.
     *
     * @param smallerOrEqualValue A value with computational type integer.
     * @param equalOrLargerValue A value with computational type integer.
     */
    /*ABSTRACT*/ def intIsLessThanOrEqualTo(
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue: DomainValue): Answer

    /**
     * Tests if the first integer value is larger than the second value.
     *
     * @param largerValue A value with computational type integer.
     * @param smallerValue A value with computational type integer.
     */
    final private[ai] def intIsGreaterThan(
        largerValue: DomainValue,
        smallerValue: DomainValue): Answer =
        intIsLessThan(smallerValue, largerValue)

    /**
     * Tests if the first integer value is larger than or equal to the second value.
     *
     * @param largerOrEqualValue A value with computational type integer.
     * @param smallerOrEqualValue A value with computational type integer.
     */
    final private[ai] def intIsGreaterThanOrEqualTo(
        largerOrEqualValue: DomainValue,
        smallerOrEqualValue: DomainValue): Answer =
        intIsLessThanOrEqualTo(smallerOrEqualValue, largerOrEqualValue)

    /**
     * Tests if the given integer value is 0 or maybe 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def intIs0(value: DomainValue): Answer =
        intAreEqual(value, IntegerConstant0)

    /**
     * Tests if the given integer value is not 0 or maybe not 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def intIsNot0(value: DomainValue): Answer =
        intAreNotEqual(value, IntegerConstant0)

    /**
     * Tests if the given integer value is &lt; 0 or maybe &lt; 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def intIsLessThan0(value: DomainValue): Answer =
        intIsLessThan(value, IntegerConstant0)

    /**
     * Tests if the given integer value is less than or equal to 0 or maybe
     * less than or equal to 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def intIsLessThanOrEqualTo0(value: DomainValue): Answer =
        intIsLessThanOrEqualTo(value, IntegerConstant0)

    /**
     * Tests if the given integer value is &gt; 0 or maybe &gt; 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def intIsGreaterThan0(value: DomainValue): Answer =
        intIsGreaterThan(value, IntegerConstant0)

    /**
     * Tests if the given value is greater than or equal to 0 or maybe greater
     * than or equal to 0.
     *
     * @param value A value with computational type integer.
     */
    final private[ai] def intIsGreaterThanOrEqualTo0(value: DomainValue): Answer =
        intIsGreaterThanOrEqualTo(value, IntegerConstant0)

    // -----------------------------------------------------------------------------------
    //
    // HANDLING CONSTRAINTS RELATED TO VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // W.r.t Reference Values
    /**
     * Called by the framework when the value is known to be `null`/has to be `null`.
     * E.g., after a comparison with `null` (IFNULL/IFNONNULL) OPAL-AI knows that the
     * value has to be `null` on one branch and that the value is not `null` on the
     * other branch.
     */
    def refEstablishIsNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def RefIsNull = refEstablishIsNull _

    /**
     * Called by OPAL-AI when it establishes that the value is guaranteed not to be `null`.
     * E.g., after a comparison with `null` OPAL-AI can establish that the
     * value has to be `null` on one branch and that the value is not `null` on the
     * other branch.
     */
    def refEstablishIsNonNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def RefIsNonNull = refEstablishIsNonNull _

    /**
     * Called by OPAL-AI when two values were compared for reference equality and
     * we are currently analyzing the branch where the comparison succeeded.
     */
    def refEstablishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def RefAreEqual = refEstablishAreEqual _

    def refEstablishAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def RefAreNotEqual = refEstablishAreNotEqual _

    /**
     * Called by OPAL-AI to inform the domain that the given type is a new additional upper
     * bound of the given value that the value is guaranteed to satisfy from here on.
     *
     * This method is called iff a subtype query (typeOf(value) <: bound) returned
     * `Unknown`.
     */
    def refEstablishUpperBound(
        pc: PC,
        bound: ReferenceType,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)

    //
    // W.r.t. Integer values

    def intEstablishValue(
        pc: PC,
        theValue: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)

    def intEstablishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def IntAreEqual = intEstablishAreEqual _

    def intEstablishAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def IntAreNotEqual = intEstablishAreNotEqual _

    def intEstablishIsLessThan(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def IntIsLessThan = intEstablishIsLessThan _

    def intEstablishIsLessThanOrEqualTo(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def IntIsLessThanOrEqualTo = intEstablishIsLessThanOrEqualTo _

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

    private[ai] final def IntIsGreaterThan: TwoValuesConstraint =
        (pc: PC, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThan(pc, value2, value1, operands, locals)

    private[ai] final def IntIsGreaterThanOrEqualTo: TwoValuesConstraint =
        (pc: PC, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThanOrEqualTo(pc, value2, value1, operands, locals)

    private[ai] final def IntIs0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishAreEqual(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IntIsNot0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishAreNotEqual(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IntIsLessThan0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThan(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IntIsLessThanOrEqualTo0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThanOrEqualTo(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IntIsGreaterThan0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThan(pc, IntegerConstant0, value, operands, locals)

    private[ai] final def IntIsGreaterThanOrEqualTo0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThanOrEqualTo(pc, IntegerConstant0, value, operands, locals)

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
     * Called by OPAL-AI when a return instruction with the given `pc` is reached.
     * In other words, when the method returns normally.
     */
    def returnVoid(pc: PC): Unit

    /**
     * Called by the abstract interpreter when an exception is thrown that is not
     * (guaranteed to be) handled within the same method.
     *
     * @note If the original exception value is `null` (`/*E.g.*/throw null;`), then
     *      the exception that is actually thrown is a new `NullPointerException`. This
     *      situation is, however, completely handled by OPAL and the exception value
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
     */
    def getstatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, Nothing]

    /**
     * Sets the field's value if the given `objectref` is not `null`(in the [[Domain]]).
     * In the latter case a `NullPointerException` is thrown.
     */
    def putfield(
        pc: PC,
        objectref: DomainValue,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, ExceptionValue]

    /**
     * Sets the field's value.
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

    type MethodCallResult = Computation[DomainValue, ExceptionValues]

    def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType, // e.g., Array[] x = ...; x.clone()
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult

    def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult

    def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult

    def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult

    //
    // INVOKEDYNAMIC
    //

    def invokedynamic(
        pc: PC,
        bootstrapMethod: BootstrapMethod,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): Computation[DomainValue, ExceptionValues]

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
     * Handles a `monitorexit` instruction.
     */
    def monitorexit(pc: PC, value: DomainValue): Computation[Nothing, ExceptionValue]

    //
    //
    // GENERAL METHODS
    //
    //

    /**
     * Merges the given value `v1` with the value `v2` and returns the merged value
     * which is `v1` if `v1` is an abstraction of `v2`, `v2` if `v2` is an abstraction
     * of `v1` or some other value if a new value is computed that abstracts over
     * both values.
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
     * This method heavily relies on reference comparison to speed up the overall
     * process of performing an abstract interpretation of a method. Hence, a should
     * whenever possible return the original object if a join of that original value with
     * some other original value result in a value that has the same abstract state.
     *
     * ''In general there should be no need to override this method.''
     *
     * @return The joined operand stack and registers.
     *      Returns `NoUpdate` if ''this'' memory layout already subsumes the ''other'' memory
     *      layout.
     * @note The size of the operands stacks that are to be joined and the number of
     *      registers/locals that are to be joined can be expected to be identical
     *      under the assumption that the bytecode is valid and framework contains no
     *      bugs.
     * @note The operand stacks are guaranteed to contain compatible values w.r.t. the
     *      computational type (unless the bytecode is not valid or OPAL contains
     *      an error). I.e., if the result of joining two operand stack values is an
     *      `IllegalValue` we assume that the domain implementation is incorrect.
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
        val newOperands: Operands =
            if (thisOperands eq otherOperands) {
                thisOperands
            } else {
                var thisRemainingOperands = thisOperands
                var otherRemainingOperands = otherOperands
                var newOperands: Operands = List.empty // during the update we build the operands stack in reverse order

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
        val newLocals: Locals =
            if (thisLocals eq otherLocals) {
                thisLocals
            } else {
                val newLocals =
                    thisLocals.merge(
                        otherLocals,
                        (thisLocal, otherLocal) ⇒ {
                            if ((thisLocal eq null) || (otherLocal eq null)) {
                                localsUpdated = localsUpdated &: MetaInformationUpdateType
                                TheIllegalValue
                            } else {
                                val updatedLocal = thisLocal.join(pc, otherLocal)
                                if (updatedLocal eq NoUpdate) {
                                    thisLocal
                                } else {
                                    localsUpdated = localsUpdated &: updatedLocal
                                    updatedLocal.value
                                }
                            }
                        }
                    )
                if (localsUpdated.noUpdate)
                    thisLocals
                else
                    newLocals

            }

        (operandsUpdated &: localsUpdated)((newOperands, newLocals))
    }

    /**
     * ''Called by the framework after performing a computation''. That is, after
     * evaluating the effect of the instruction with `currentPC` on the current stack and
     * register and joining the updated stack and registers with the stack and registers
     * associated with the instruction `successorPC`.
     * This function basically informs the domain about the instruction that
     * may be evaluated next. The flow function is called for ''every possible
     * successor'' of the instruction with `currentPC`. This includes all branch
     * targets as well as those instructions that handle exceptions.
     *
     * In some cases it will even be the case that `flow` is called multiple times with
     * the same pair of program counters: (`currentPC`, `successorPC`). This may happen,
     * e.g., in case of a switch instruction where multiple values have the same
     * body/target instruction and we do not have precise information about the switch value.
     * E.g., as in the following snippet:
     * {{{
     * switch (i) {  // pc: X => Y (for "1"), Y (for "2"), Y (for "3")
     * case 1:
     * case 2:
     * case 3: System.out.println("Great.");            // pc: Y
     * default: System.out.println("Not So Great.");    // pc: Z
     * }
     * }}}
     * The flow function is also called after instructions that are domain independent
     * such as `dup` and `load` instructions which ''just'' manipulate the registers
     * and stack in a generic way.
     * This enables the domain to precisely follow the evaluation
     * progress and in particular to perform control-flow dependent analyses.
     *
     * @param currentPC The program counter of the instruction that is currently evaluated
     *      by the abstract interpreter.
     *
     * @param successorPC The program counter of an instruction that is a potential
     *      successor of the instruction with `currentPC`. If the head of the
     *      given `worklist` is not `successorPC` the abstract interpreter did
     *      not (again) schedule the evaluation of the instruction with `successorPC`.
     *      This means that the instruction was evaluated in the past and that
     *      the abstract state did not change in a way that a reevaluation is –
     *      from the point of view of the AI framework – necessary.
     *
     * @param isExceptionalControlFlow `True` if an and only if the evaluation of
     *      the instruction with the program counter `currentPC` threw an exception;
     *      `false` otherwise. Hence, if `true` the instruction with `successorPC` is the
     *      first instruction of the handler.
     *
     * @param operandsArray The array that associates '''every instruction''' with its
     *      operand stack that is in effect.  Note, that only those elements of the
     *      array contain values that are related to instructions that were
     *      evaluated in the past. The other elements are `null`.
     *
     * @param localsArray The array that associates every instruction with its current
     *      register values. Note, that only those elements of the
     *      array contain values that are related to instructions that were evaluated in
     *      the past. The other elements are `null`.
     *
     * @param worklist The current list of instructions that will be evaluated next.
     *      If you want to force the evaluation of the instruction
     *      with the program counter `successorPC` it is sufficient to test whether
     *      the list already contains `successorPC` and – if not – to prepend it.
     *      If the worklist already contains `successorPC` then the domain is allowed to move
     *      the PC to the beginning of the worklist. However, if the PC does not belong
     *      to the same (current) (sub)routine, it is not allowed to be moved to the beginning
     *      of the worklist. (Subroutines can only be found in code generated by old
     *      Java compilers; before Java 6. Subroutines are identified by jsr/ret
     *      instructions. A subroutine can be identified by going back in the worklist
     *      and by looking for negative "program counters".
     *      These negative program counters mark the beginning of a subroutine. In other
     *      words, an instruction can be freely moved around unless a negative value is
     *      found.) Additionally, neither the negative values nor the positive values between
     *      two negative values should be changed. Furthermore, no value (PC) should be put
     *      between negative values that capture subroutine information.
     *      If the domain updates the worklist, it is the responsibility of the domain
     *      to call the tracer and to inform it about the changes.
     *      Note that the worklist is not allowed to contain duplicates related to the
     *      evaluation of the current (sub-)routine.
     *
     * @return The updated worklist. In most cases this is simply the given `worklist`.
     *      The default case is also to return the given `worklist`.
     *
     * @note The domain is allowed to modify the `worklist`, `operandsArray` and
     *      `localsArray`. However, the AI will not perform any checks. In case of
     *      updates of the `operandsArray` or `localsArray` it is necessary to first
     *      create a shallow copy before  updating it.
     *      If this is not done, it may happen that the locals associated
     *      with other instructions are also updated.
     */
    def flow(
        currentPC: PC,
        successorPC: PC,
        isExceptionalControlFlow: Boolean,
        worklist: List[PC],
        operandsArray: OperandsArray,
        localsArray: LocalsArray,
        tracer: Option[AITracer]): List[PC] = worklist

    /**
     * Called by the framework after evaluating the instruction with the given pc. I.e.,
     * the state of all potential successor instructions was updated and the
     * flow method was called – potentially multiple times – accordingly.
     *
     * By default this method does nothing.
     */
    def evaluationCompleted(
        pc: PC,
        worklist: List[PC],
        evaluated: List[PC],
        operandsArray: OperandsArray,
        localsArray: LocalsArray,
        tracer: Option[AITracer]): Unit = { /*Nothing*/ }

    /**
     * Called by (OPAL)AI when the abstract interpretation of a method has ended. The
     * abstract interpretation of a method ends if either the fixpoint is reached or
     * the interpretation was aborted.
     *
     * By default this method does nothing.
     */
    def abstractInterpretationEnded(
        aiResult: AIResult { val domain: Domain.this.type }): Unit = { /* Nothing */ }

    /**
     * Creates a summary of the given domain values by summarizing and
     * joining the given `values`. For the precise details
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
     * and is not used by the abstract interpretation framework.
     *
     * `DomainValue`s that define (additional) properties should (`abstract`) `override`
     * this method and should return a textual representation of the property.
     */
    def properties(pc: PC): Option[String] = None
}
