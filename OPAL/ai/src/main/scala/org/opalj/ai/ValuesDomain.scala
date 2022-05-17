/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import scala.reflect.ClassTag

import org.opalj.value.IsIllegalValue
import org.opalj.value.IsReferenceValue
import org.opalj.value.IsReturnAddressValue
import org.opalj.value.KnownTypedValue
import org.opalj.value.ValueInformation
import org.opalj.br.ClassHierarchy
import org.opalj.br.PC
import org.opalj.br.ReferenceType
import org.opalj.br.Type

/**
 * Defines the concept of a value in a `Domain`.
 *
 * @see [[Domain]] For an explanation of the underlying concepts and ideas.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait ValuesDomain { domain =>

    /**
     * This project's class hierarchy.
     *
     * Usually, just a redirect to the `Project`'s class hierarchy or the default class hierarchy.
     */
    implicit def classHierarchy: ClassHierarchy

    /**
     * Tests if `subtype` is known to be subtype of `supertype`.
     * See [[org.opalj.br.ClassHierarchy]]'s `isSubtypeOf` method for details.
     */
    final def isASubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer = {
        classHierarchy.isASubtypeOf(subtype, supertype)
    }

    /**
     * Tests if `subtype` is known to be subtype of `supertype`.
     * See [[org.opalj.br.ClassHierarchy]]'s `isSubtypeOf` method for details.
     */
    final def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Boolean = {
        classHierarchy.isSubtypeOf(subtype, supertype)
    }

    /**
     * Creates a domain value from the given value information that represents a
     * properly domain value.
     * A representation of a proper value is created even if the value information is provided
     * for an '''uninitialized''' value.
     *
     * @note This function is only defined for proper values, i.e., it is not defined for '''void'''
     *       values or illegal values.
     *
     * @note This method is intended to be overwritten by concrete domains which can represent more
     *       information.
     */
    def InitializedDomainValue(origin: ValueOrigin, vi: ValueInformation): DomainValue

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over the concrete type of `Value`. Needs to be refined by traits that
     * inherit from `Domain` and which extend `Domain`'s `Value` trait.
     */
    type DomainValue >: Null <: Value with AnyRef

    /**
     * The class tag for the type `DomainValue`.
     *
     * Required to generate instances of arrays in which values of type
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
     * Abstracts over a concrete operand stack value or a value stored in one of the local
     * variables/registers.
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
     * Standard inheritance from this trait is always supported and is the primary
     * mechanism to model an abstract domain's lattice w.r.t. some special type of value.
     * In general, the implementation should try to avoid creating new instances of
     * values unless strictly required to model the domain's semantics. This will greatly
     * improve the overall performance as this framework heavily uses reference-based
     * equality checks to speed up the evaluation.
     *
     * @note OPAL does not rely on any special equality semantics w.r.t. values and
     *      never directly or indirectly calls a `Value`'s `equals` method. Hence,
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
     *      In general, `equals` is only defined for values belonging to the same domain.
     *      If values need to be compared across domains, they need to be adapted
     *      to a target domain first.
     */
    trait Value extends ValueInformation { this: DomainValue =>

        @inline final def PCIndependent: Int = Int.MinValue

        /**
         * @return  The concrete return address stored by this value; this method
         *          is defined if and only if the value is guaranteed to encapsulate a
         *          `ReturnAddressValue`.
         */
        private[ai] def asReturnAddressValue: Int = {
            throw new ClassCastException(this.getClass.getSimpleName+" is no return address value");
        }

        /**
         * Returns the represented reference value iff this value represents a reference value.
         */
        def asDomainReferenceValue: DomainReferenceValue = {
            throw new ClassCastException(this.getClass.getSimpleName+" is no reference value");
        }

        /**
         * Joins this value and the given value.
         *
         * Join is called whenever an instruction is evaluated more than once and, hence,
         * the values found on the paths need to be joined. This method is, however,
         * only called if the two values are two different objects (`(this ne value) === true`),
         * but both values have the ''same computational type''.
         *
         * This basically implements the join operator of complete lattices.
         *
         * ==Example==
         * For example, joining a `DomainValue` that represents the integer value 0
         * with a `DomainValue` that represents the integer value 1 may return a new
         * `DomainValue` that precisely captures the range '''[0..1]''' or that captures
         * '''all positive''' integer values or just '''some integer value'''.
         *
         * ==Contract==
         * '''`this` value''' is always the value that was previously used to perform
         * subsequent computations/analyses. Hence, if `this` value subsumes the given
         * value, the result has to be either `NoUpdate` or a `MetaInformationUpdate`.
         * In case that the given value subsumes `this` value, the result has to be
         * a `StructuralUpdate` with the given value as the new value. Hence,
         * ''this `join` operation is not commutative''. If a new (more abstract)
         * abstract value is created that represents both values the result always has to
         * be a `StructuralUpdate`.
         * If the result is a `StructuralUpdate` the framework will continue with the
         * interpretation.
         *
         * The termination of the abstract interpretation directly depends on the fact
         * that at some point all (abstract) values are fixed and don't change anymore.
         * Hence, it is important that '''the type of the update is only a
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
         * (`this.doJoin(..,this)`) will not be performed by the abstract interpretation
         * framework; this case is handled by the [[join]] method.
         * However, if the join object is also used by the implementation of the domain
         * itself, it may be necessary to explicitly handle self-joins.
         *
         * ==Performance==
         * In general, the domain should try to minimize the number of objects that it
         * uses to represent values. That is, '''two values that are conceptually equal
         * should – whenever possible – use only one object'''. This has a significant
         * impact on functions such as `join`.
         *
         * @param   pc The program counter of the instruction where the paths converge.
         * @param   value The "new" domain value with which this domain value should be
         *          joined.
         *          '''The given `value` and this value are guaranteed to have
         *          the same computational type, but are not reference equal.'''
         */
        protected[this] def doJoin(pc: Int, value: DomainValue): Update[DomainValue]

        /**
         * Checks that the given value and this value are compatible with regard to
         * its computational type and – if so – calls [[doJoin]].
         *
         * See `doJoin(PC,DomainValue)` for details.
         *
         * @note    It is in general not recommended/needed to override this method.
         *
         * @param   pc The program counter of the instruction where the paths converge or
         *          `Int.MinValue` if the join is done independently of an instruction.
         * @param   that The "new" domain value with which this domain value should be
         *          joined. The caller has to ensure that the given value and this value
         *          are guaranteed to be two different objects.
         * @return  [[MetaInformationUpdateIllegalValue]] or the result of calling
         *          [[doJoin]].
         */
        def join(pc: Int, that: DomainValue): Update[DomainValue] = {
            assert(that ne this, "join is only defined for objects that are different")

            if ((that eq TheIllegalValue) || (this.computationalType ne that.computationalType))
                MetaInformationUpdateIllegalValue
            else
                doJoin(pc, that)
        }

        //
        // METHODS THAT ARE PREDEFINED BECAUSE THEY ARE GENERALLY USEFUL WHEN
        // ANALYZING PROJECTS, BUT WHICH ARE NOT REQUIRED BY THE AI CORE FRAMEWORK.
        // I.E. THESE METHODS ARE USED - IF AT ALL - BY THE DOMAIN.
        //

        /**
         * Returns `true` iff the abstract state represented by this value abstracts over
         * the state of the given value. In other words if every possible runtime value
         * represented by the given value is also represented by this value.
         *
         * The abstract state generally encompasses every information that would
         * be considered during a [[join]] of `this` value and the `other` value and that
         * could lead to a true [[Update]].
         *
         * This method is '''reflexive''', I.e., every value abstracts over itself.
         *
         * [[TheIllegalValue]] only abstracts over itself.
         *
         * @note abstractsOver is only defined for comparable values where both values have the
         *       same computational type.
         *
         * @note The default implementation uses the [[join]]  method of this '''domain value'''.
         *       Overriding this method is, hence, primarily meaningful for performance reasons.
         *
         * @see `isMorePreciseThan`
         */
        def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            val result = this.join(PCIndependent, other)
            result.isNoUpdate ||
                (result.isMetaInformationUpdate && (result ne MetaInformationUpdateIllegalValue))
        }

        /**
         * Returns `true` iff the abstract state represented by this value is strictly more
         * precise than the state of the given value. In other words if every possible
         * runtime value represented by this value is also represented by the given value,
         * but both '''are not equal'''; in other words, this method is '''irreflexive'''.
         *
         * The considered abstract state generally encompasses every information that would be
         * considered during a [[join]] of `this` value and the `other` value and that could
         * lead to a [[StructuralUpdate]].
         *
         * @note    It is recommended to overwrite this method for performance
         *          reasons, as the default implementation relies on [[join]].
         *
         * @param   other Another `DomainValue` with the same computational type as this value.
         *          (The `IllegalValue` has no computational type and, hence, a comparison with
         *          an IllegalValue is not well defined.)
         *
         * @see `abstractsOver`
         */
        def isMorePreciseThan(other: DomainValue): Boolean = {
            assert(this.computationalType eq other.computationalType)

            if (this eq other)
                return false;

            other.join(PCIndependent, this).updateType match {
                case StructuralUpdateType =>
                    // ... i.e., either this value abstracts over the other value or
                    // this and the other value are not in a more/less precise relation.
                    false
                case NoUpdateType
                    // ... if the other value abstracts over this value or equals this value.
                    | MetaInformationUpdateType // ... if the other value is equal to this value or is un-comparable.
                    =>
                    // We have to check that the other is NOT more precise than this!
                    this.join(PCIndependent, other).isStructuralUpdate
            }
        }

        /**
         * Creates a summary of this value.
         *
         * In general, creating a summary of a value may be useful/required for values that
         * are potentially returned by a called method and which will then be used by the
         * calling method. For example, it may be useful to precisely track the flow of
         * values within a method to be able to distinguish between all sources of a value
         * (E.g., to be able to distinguish between a `NullPointerException` created by
         * instruction A and another one created by instruction B (`A != B`).)
         *
         * However, from the caller perspective it may be absolutely irrelevant where/how the
         * value was created in the called method and, hence, keeping all information would
         * just waste memory and a summary may be sufficient.
         *
         * @note   This method is predefined to facilitate the development of project-wide analyses.
         */
        def summarize(pc: Int): DomainValue

        /**
         * Adapts this value to the given domain (default: throws a domain exception
         * that adaptation is not supported). '''This method needs to be overridden
         * by concrete `Value` classes to support the adaptation for a specific domain.'''
         *
         * Supporting the `adapt` method is primarily necessary when you want to analyze a
         * method that is called by the currently analyzed method and you need to adapt this
         * domain's values (the actual parameters of the method) to the domain used for
         * analyzing the called method.
         *
         * Additionally, the `adapt` method is OPAL's main mechanism to enable dynamic
         * domain-adaptation. I.e., to make it possible to change the abstract domain at
         * runtime if the analysis time takes too long using a (more) precise domain.
         *
         * @note   The abstract interpretation framework does not use/call this method.
         *         This method is solely predefined to facilitate the development of
         *         project-wide analyses.
         */
        @throws[DomainException]("Adaptation of this value is not supported.")
        def adapt(target: TargetDomain, valueOrigin: Int): target.DomainValue = {
            throw DomainException(s"adaptation of $this to $target is unsupported");
        }
    }

    type DomainTypedValue[+T <: Type] >: Null <: DomainValue

    trait TypedValue[+T <: Type] extends Value with KnownTypedValue {
        this: DomainTypedValue[T] =>

        /**
         * The type kind of the values, if the value has a specific type kind; `None` if and
         * only if the underlying value is `null`.
         *
         * @return The type/the least upper type bound of the value.
         *         If the type is a base type, then the type is necessarily precise.
         *         In case of a reference type the type may be an upper type bound or may be
         *         precise.
         *         In the latter case, it may be possible to get further information using
         *         the concrete domain. If the underlying value is `null`, `None` is returned.
         */
        def leastUpperType: Option[T]

    }

    type DomainReferenceValue >: Null <: ReferenceValue with DomainTypedValue[ReferenceType]

    /**
     * The class tag can be used to create type safe arrays or to extract the concrete type
     * of the domain value.
     * {{{
     *     val DomainReferenceValue(v) = value // of type "DomainValue"
     *     // v is now of the type DomainReferenceValue
     * }}}
     */
    val DomainReferenceValueTag: ClassTag[DomainReferenceValue]

    trait ReferenceValue extends TypedValue[ReferenceType] with IsReferenceValue {
        this: domain.DomainReferenceValue =>

        override def isPrecise: Boolean = {
            leastUpperType match {
                case None    => true // the value is null
                case Some(t) => classHierarchy.isKnownToBeFinal(t)
            }
        }

        override def baseValues: Iterable[domain.DomainReferenceValue]

        override def allValues: Iterable[domain.DomainReferenceValue]

        /*
        /**
         * Provides the correct verification type for non-locally initialized object values if
         * null-ness is tracked.
         *
         * For the computation of uninitialized this/object value infos,
         * this method needs to be refined/overridden.
         */
        override def verificationTypeInfo: VerificationTypeInfo = {
            if (isNull.isYes) {
                NullVariableInfo
            } else {
                ObjectVariableInfo(leastUpperType.get.asReferenceType)
            }
        }
        */
    }

    /**
     * A simple type alias of the type `DomainValue`; used to facilitate comprehension.
     */
    type ExceptionValue = DomainReferenceValue

    /**
     * A type alias for `Iterable`s of `ExceptionValue`s; used to facilitate comprehension.
     */
    type ExceptionValues = Iterable[ExceptionValue]

    /**
     * The typed `null` value.
     */
    private[ai] val Null: DomainValue = null

    /**
     * An instruction's operands are represented using a list where the first
     * element of the list represents the top level operand stack value.
     */
    type Operands = org.opalj.ai.Operands[DomainValue] // the package name is required by unidoc

    type OperandsArray = org.opalj.ai.TheOperandsArray[Operands] // the package name is required by unidoc

    /**
     * An instruction's current register values/locals are represented using an array.
     */
    type Locals = org.opalj.ai.Locals[DomainValue] // the package name is required by unidoc

    type LocalsArray = org.opalj.ai.TheLocalsArray[Locals] // the package name is required by unidoc

    /**
     * Represents a value that has no well defined state/type. Such values are either
     * the result of a join of two incompatible values or if the variable was identified
     * as being dead. `IllegalValue`'s are only found in registers (in the locals).
     *
     * @see [[org.opalj.ai.Domain.Value]] for further details.
     */
    protected class IllegalValue extends Value with IsIllegalValue {
        this: DomainIllegalValue =>

        @throws[DomainException]("doJoin(...) is not supported by IllegalValue")
        override protected def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            throw DomainException("doJoin(...) is not supported by IllegalValue")
        }

        @throws[DomainException]("summarize(...) is not supported by IllegalValue")
        override def summarize(pc: Int): DomainValue = {
            throw DomainException("summarize(...) is not supported by IllegalValue")
        }

        override def join(pc: Int, other: DomainValue): Update[DomainValue] = {
            if (other eq TheIllegalValue)
                NoUpdate
            else
                MetaInformationUpdateIllegalValue
        }

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.TheIllegalValue
        }

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
     * The result of the merge of two incompatible values has to be reported as a
     * `MetaInformationUpdate[DomainIllegalValue]`.
     */
    def MetaInformationUpdateIllegalValue: MetaInformationUpdate[DomainIllegalValue]

    /**
     * The result of merging two values should never be reported as a `StructuralUpdate` if the
     * computed value is an `IllegalValue`. The JVM semantics guarantee that the value will not
     * be used and, hence, continuing the interpretation is meaningless.
     *
     * @note   This method is solely defined for documentation purposes and to catch
     *         implementation errors early on.
     */
    final def StructuralUpdateIllegalValue: StructuralUpdate[Nothing] = {
        throw DomainException("internal error (see ValuesDomain.StructuralUpdateIllegalValue())");
    }

    // an implementation trait for return addresses
    trait RETValue extends Value with IsReturnAddressValue { this: DomainValue =>

        @throws[DomainException]("summarize(...) is not supported by RETValue")
        override def summarize(pc: Int): DomainValue = {
            throw DomainException("summarize(...) is not supported by RETValue");
        }
    }

    /**
     * A collection of (not further stored) return address values. Primarily used when we
     * join the executions of subroutines.
     */
    class ReturnAddressValues extends RETValue {
        this: DomainReturnAddressValues =>

        override protected def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case _: RETValue => NoUpdate
                case _           => MetaInformationUpdateIllegalValue
                // The superclass "Value" handles the case where this value is joined with itself.
            }
        }

        // Adaptation is supported to support on-the-fly domain up-/downcasts.
        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.TheReturnAddressValues
        }

        override def toString: String = "ReturnAddresses"

    }
    type DomainReturnAddressValues <: ReturnAddressValues with DomainValue

    /**
     *  The singleton instance of `ReturnAddressValues`
     */
    val TheReturnAddressValues: DomainReturnAddressValues

    /**
     * Stores a single return address (i.e., a program counter/index into the code array).
     *
     * @note Though the framework completely handles all aspects related to return address
     *      values, it is nevertheless necessary that this class inherits from `Value`
     *      as return addresses are stored on the stack/in the registers. However,
     *      if the `Value` trait should be refined, all additional methods may – from
     *      the point-of-view of OPAL-AI – just throw an `UnsupportedOperationException`
     *      as these additional methods will never be called by the OPAL-AI.
     */
    class ReturnAddressValue(val address: Int) extends RETValue { this: DomainReturnAddressValue =>

        private[ai] final override def asReturnAddressValue: Int = address

        override protected def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case _: RETValue => StructuralUpdate(TheReturnAddressValues)
                case _           => MetaInformationUpdateIllegalValue
                // The super class "Value" handles the case where this value is joined with itself.

            }
        }

        // Adaptation is supported to support on-the-fly domain up-/downcasts.
        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.ReturnAddressValue(address)
        }

        override def toString: String = "ReturnAddress("+address+")"

        override def hashCode: Int = address
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
    def ReturnAddressValue(address: Int): DomainReturnAddressValue

    // -----------------------------------------------------------------------------------
    //
    // QUESTION'S ABOUT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Merges the given domain value `v1` with the domain value `v2` and returns
     * the merged value which is `v1` if `v1` is an abstraction of `v2`, `v2` if `v2`
     * is an abstraction of `v1` or some other value if a new value is computed that
     * abstracts over both values.
     *
     * This operation is commutative.
     */
    def mergeDomainValues(pc: Int, v1: DomainValue, v2: DomainValue): DomainValue = {
        if (v1 eq v2)
            return v1;

        v1.join(pc, v2) match {
            case NoUpdate      => v1
            case SomeUpdate(v) => v
        }
    }

    /**
     * Creates a summary of the given domain values by summarizing and
     * joining the given `values`. For the precise details
     * regarding the calculation of a summary see `Value.summarize(...)`.
     *
     * @param pc The program counter that will be used for the summary value if
     *        a new value is returned that abstracts over/summarizes the given values.
     * @param values An `Iterable` over one or more values.
     *
     * @note The current algorithm is generic and should satisfy most needs, but
     *      it is not very efficient. However, it should be easy to tailor it for a
     *      specific domain/domain values, if need be.
     */
    def summarize(pc: Int, values: Iterable[DomainValue]): DomainValue = {
        val valuesIterator = values.iterator
        var summary = valuesIterator.next().summarize(pc)
        valuesIterator foreach { value =>
            if (summary ne value) {
                summary.join(pc, value.summarize(pc)) match {
                    case NoUpdate               => /*nothing to do*/
                    case SomeUpdate(newSummary) => summary = newSummary.summarize(pc)
                }
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
     * `Domain`s that define (additional) properties should (`abstract`) `override`
     * this method and should return a textual representation of the property.
     */
    def properties(
        pc:               PC,
        propertyToString: AnyRef => String = p => p.toString
    ): Option[String] = {
        None
    }
}
