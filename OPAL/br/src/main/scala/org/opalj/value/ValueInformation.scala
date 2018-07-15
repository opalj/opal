/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package value

import org.opalj.collection.immutable.UIDSet
import org.opalj.br.Type
import org.opalj.br.BaseType
import org.opalj.br.ReferenceType
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ShortType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.FloatType
import org.opalj.br.DoubleType
import org.opalj.br.VoidType
import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.ComputationalTypeReturnAddress
import org.opalj.collection.immutable.UIDSet1

import scala.annotation.switch

/**
 * Encapsulates the available type information about a `DomainValue`.
 *
 * @author Michael Eichberg
 */
sealed trait ValueInformation {

    /**
     * Returns `true` if no type information is available.
     */
    def isUnknownValue: Boolean

    /** True if the value is "Void"; undefined if the type is unknown. */
    def isVoid: Boolean

    /** True in case of a value with primitive type; undefined if the type is unknown. */
    def isPrimitiveValue: Boolean

    /** True if the value has a reference type; undefined if the type is unknown. */
    def isReferenceValue: Boolean
    def asReferenceValue: IsReferenceValue = throw new ClassCastException

}

object ValueInformation {

    def apply(t: Type): ValueInformation = {
        (t.id: @switch) match {
            case VoidType.id     ⇒ VoidValue
            case BooleanType.id  ⇒ ABooleanValue
            case ByteType.id     ⇒ AByteValue
            case CharType.id     ⇒ ACharValue
            case ShortType.id    ⇒ AShortValue
            case IntegerType.id  ⇒ AnIntegerValue
            case LongType.id     ⇒ ALongValue
            case FloatType.id    ⇒ AFloaValue
            case DoubleType.id   ⇒ ADoubleValue
            case referenceTypeId ⇒ AReferenceValue(t.asReferenceType)

        }
    }

}

/**
 * Specifies that no type information is available.
 *
 * @note Recall that the computational type of a value always has to
 *      be available, but that a
 *      `ValuesDomain.typeOfValue(...)` query does not need to take the computational type
 *      into account. (Whenever the core framework requires the computational type of a
 *      value it uses the respective method.) However, in case that the
 *      underlying value may be an array or exception
 *      value the reported type must not be `TypeUnknown`.
 *
 * @author Michael Eichberg
 */
case object UnknownValue extends ValueInformation {

    override def isUnknownValue: Boolean = true

    override def isVoid: Boolean = throw new IllegalStateException("unknown value")

    override def isPrimitiveValue: Boolean = throw new IllegalStateException("unknown value")

    override def isReferenceValue: Boolean = throw new IllegalStateException("unknown value")

}

sealed trait KnownValue extends ValueInformation {

    final override def isUnknownValue: Boolean = false

}

object VoidValue extends KnownValue {

    override def isVoid: Boolean = true

    override def isPrimitiveValue: Boolean = false

    override def isReferenceValue: Boolean = false

}

trait KnownTypedValue extends KnownValue {

    /**
     * The computational type of the value.
     *
     * The precise computational type is, e.g., needed to calculate the effect
     * of generic stack manipulation instructions (e.g., `DUP_...` and `SWAP`)
     * on the stack as well as to calculate the jump targets of `RET` instructions
     * and to determine which values are actually copied by, e.g., the `dup_XX`
     * instructions.
     *
     * @note The computational type has to be precise/correct.
     */
    def computationalType: ComputationalType

}

/**
 * The value has the primitive type.
 */
trait IsReturnAddressValue extends KnownTypedValue {

    final override def isVoid: Boolean = false

    final override def isReferenceValue: Boolean = false

    final override def isPrimitiveValue: Boolean = false

    final override def computationalType: ComputationalType = ComputationalTypeReturnAddress

}

/**
 * The value has the primitive type.
 */
sealed trait IsPrimitiveValue[T <: BaseType] extends KnownTypedValue {

    final override def isVoid: Boolean = false

    final override def isReferenceValue: Boolean = false

    final override def isPrimitiveValue: Boolean = true

    def primitiveType: T

    final override def computationalType: ComputationalType = primitiveType.computationalType

}

object IsPrimitiveValue {
    def unapply[T <: BaseType](answer: IsPrimitiveValue[T]): Some[T] = Some(answer.primitiveType)
}

trait IsBooleanValue extends IsPrimitiveValue[BooleanType] {
    final def primitiveType: BooleanType = BooleanType
}
case object ABooleanValue extends IsBooleanValue

trait IsByteValue extends IsPrimitiveValue[ByteType] {
    final def primitiveType: ByteType = ByteType
}
case object AByteValue extends IsByteValue

trait IsCharValue extends IsPrimitiveValue[CharType] {

    final def primitiveType: CharType = CharType
}
case object ACharValue extends IsCharValue

trait IsShortValue extends IsPrimitiveValue[ShortType] {
    final def primitiveType: ShortType = ShortType
}
case object AShortValue extends IsShortValue

trait IsIntegerValue extends IsPrimitiveValue[IntegerType] {
    final def primitiveType: IntegerType = IntegerType
    def lowerBound: Int
    def upperBound: Int
}
case object AnIntegerValue extends IsIntegerValue {
    def lowerBound: Int = Int.MinValue
    def upperBound: Int = Int.MaxValue
}

trait IsFloatValue extends IsPrimitiveValue[FloatType] {
    final def primitiveType: FloatType = FloatType
}
case object AFloaValue extends IsFloatValue

trait IsLongValue extends IsPrimitiveValue[LongType] {
    final def primitiveType: LongType = LongType
}
case object ALongValue extends IsLongValue

trait IsDoubleValue extends IsPrimitiveValue[DoubleType] {
    final def primitiveType: DoubleType = DoubleType
}
case object ADoubleValue extends IsDoubleValue

/**
 * Describes the essential properties of a reference value in a program.
 *
 * For example, in the following:
 * {{{
 * val o = If(...) new Object() else "STRING"
 * }}}
 * o is a reference value (`IsReferenceValue`) that (may) refers to two "simple" base values:
 * `new Object()` and `"STRING"`; however, it is a decision of the underlying domain whether
 * the information about the base values is made available or not. Furthermore, if the base values
 * are actually used, the constraints in effect for the overall abstraction should be considered
 * to get the most precise result.
 *
 * @author Michael Eichberg
 */
trait IsReferenceValue extends KnownTypedValue {

    final override def isVoid: Boolean = false

    final override def isReferenceValue: Boolean = true
    final override def asReferenceValue: IsReferenceValue = this

    final override def isPrimitiveValue: Boolean = false

    final override def computationalType: ComputationalType = ComputationalTypeReference

    /**
     * The upper bound of the value's type. The upper bound is empty if this
     * value is `null` (i.e., `isNull == Yes`). The upper bound is only guaranteed to contain
     * exactly one type if the type is precise. (i.e., `isPrecise == true`). Otherwise,
     * the upper type bound may contain one or more types that are not known to be
     * in an inheritance relation, but which will ''correctly'' approximate the runtime
     * type.
     *
     * @note If only a part of a project is analyzed, the class hierarchy may be
     *      fragmented and it may happen that two classes that are indeed in an
     *      inheritance relation – if we would analyze the complete project – are part
     *      of the upper type bound.
     */
    def upperTypeBound: UIDSet[_ <: ReferenceType]

    /**
     * If `Yes` the value is known to always be `null` at runtime. In this
     * case the upper bound  is (has to be) empty. If the answer is `Unknown` then the
     * analysis was not able to statically determine whether the value is `null` or
     * is not `null`. In this case the upper bound is expected to be non-empty.
     * If the answer is `No` then the value is statically known not to be `null`. In this
     * case, the upper bound may precisely identify the runtime type or still just identify
     * an upper bound.
     *
     * This default implementation always returns `Unknown`; this is a sound
     * over-approximation.
     *
     * @note '''This method is expected to be overridden by subtypes.'''
     *
     * @return `Unknown` (default)
     */
    def isNull: Answer = Unknown

    /**
     * Returns `true` if the type information is precise. I.e., the type returned by
     * `upperTypeBound` precisely models the runtime type of the value.
     *  If, `isPrecise` returns true, the type of this value can
     * generally be assumed to represent a class type (not an interface type) or
     * an array type. However, this domain also supports the case that `isPrecise`
     * returns `true` even though the associated type identifies an interface type
     * or an abstract class type. The later case may be interesting in the context
     * of classes that are generated at run time.
     *
     * This default implementation always returns `false`.
     *
     * @note `isPrecise` is always `true` if this value is known to be `null`.
     *
     * @note '''This method is expected to be overridden by subtypes.'''
     *
     * @return `false` (default)
     */
    def isPrecise: Boolean = false

    /**
     * Returns '''the type of the upper type bound''' if the upper type bound contains
     * exactly one element. That is, the function is only always defined iff the type
     * is precise.
     */
    final def theType: ReferenceType = {
        if (!upperTypeBound.isSingletonSet) {
            throw new AssertionError(s"$upperTypeBound.size >= 1")
        }

        upperTypeBound.head
    }

    /**
     * The least upper unique type bound of the upper type value. `None` if and only if the underlying value is `null`.
     */
    def valueType: Option[ReferenceType]

    /**
     * Tests if the type of this value is potentially a subtype of the specified
     * reference type under the assumption that this value is not `null`.
     * This test takes the precision of the type information into account.
     * That is, if the currently available type information is not precise and
     * the given type has a subtype that is always a subtype of the current
     * upper type bound, then `Unknown` is returned. Given that it may be
     * computationally intensive to determine whether two types have a common subtype
     * it may be better to just return `Unknown` in case that this type and the
     * given type are not in a direct inheritance relationship.
     *
     *
     * Basically, this method implements the same semantics as the `ClassHierarchy`'s
     * `isSubtypeOf` method, but it additionally checks if the type of this value
     * ''could be a subtype'' of the given supertype. I.e., if this value's type
     * identifies a supertype of the given `supertype` and that type is not known
     * to be precise, the answer is `Unknown`.
     *
     * For example, assume that the type of this reference value is
     * `java.util.Collection` and we know/have to assume that this is only an
     * upper bound. In this case an answer is `No` if and only if it is impossible
     * that the runtime type is a subtype of the given supertype. This
     * condition holds, for example, for `java.io.File` which is not a subclass
     * of `java.util.Collection` and which does not have any further subclasses (in
     * the JDK). I.e., the classes `java.io.File` and `java.util.Collection` are
     * not in an inheritance relationship. However, if the specified supertype would
     * be `java.util.List` the answer would be unknown.
     *
     * @note The function `isValueSubtypeOf` is not defined if `isNull` returns `Yes`;
     *      if `isNull` is `Unknown` then the result is given under the
     *      assumption that the value is not `null` at runtime.
     *      In other words, if this value represents `null` this method is not supported.
     * @note This method is expected to be overridden by subtypes.
     *
     * @return This default implementation always returns `Unknown`.
     */
    def isValueSubtypeOf(referenceType: ReferenceType): Answer = Unknown

    type BaseReferenceValue <: IsReferenceValue
    def asBaseReferenceValue: BaseReferenceValue

    /**
     * In general an `IsReferenceValue` abstracts over all potential values and this information is
     * sufficient for subsequent analyses; but in some cases, analyzing the set of underlying values
     * may increase the overall precision and this set is returned by this function. In other
     * words: if `baseValues` is nonEmpty, then the properties returned by `this` value are derived
     * from the base values, but still maybe more specific. For example,
     * {{{
     *     Object o = _;
     *     if(...) o = f() else o = g();
     *     // when we reach this point, we generally don't know if the values returned by f and g
     *     // are non-null; hence, o is potentially null.
     *     if(o != null)
     *      // Now, we know that o is not null, but we still don't know if the values returned
     *      // by f OR g were null and we cannot establish that when we don't know to which value
     *      // o is actually referring to.
     *      u(o);
     * }}}
     *
     * @note A reference value which belongs to the base values by some other reference value
     *       '''never''' has itself as a '''direct''' base value.
     *
     * @return The set of values this reference value abstracts over. The set is empty if this
     *         value is already a base value and it does not abstract over other values.
     */
    def baseValues: Traversable[BaseReferenceValue]

    /**
     * The set of base values this value abstracts over. This set is never empty and contains
     * this value if this value does not (further) abstract over other reference values; otherwise
     * it only contains the base values, but not `this` value.
     *
     * @note Primarily defined as a convenience interface.
     */
    final def allValues: Traversable[BaseReferenceValue] = {
        val baseValues = this.baseValues
        if (baseValues.isEmpty) Set(asBaseReferenceValue) else baseValues
    }

}

case class AReferenceValue(referenceType: ReferenceType) extends IsReferenceValue {
    override type BaseReferenceValue = AReferenceValue
    override def asBaseReferenceValue: AReferenceValue = this

    override def upperTypeBound: UIDSet[_ <: ReferenceType] = UIDSet1(referenceType)
    override def valueType: Option[ReferenceType] = Some(referenceType)
    override def baseValues: Traversable[AReferenceValue] = Traversable(this)
}

/**
 * Extractor for instances of `IsReferenceValue` objects.
 *
 * @author Michael Eichberg
 */
object TypeOfReferenceValue {
    def unapply(rv: IsReferenceValue): Some[UIDSet[_ <: ReferenceType]] = Some(rv.upperTypeBound)
}

/**
 * Defines an extractor method for instances of `IsReferenceValue` objects.
 *
 * @note To ensure that the generic type can be matched, it may be necessary to first cast
 *       a ''generic'' `org.opalj.ai.ValuesDomain.DomainValue` to a
 *       `org.opalj.ai.ValuesDomain.DomainReferenceValue`.
 *       {{{
 *           val d : Domain = ...
 *           val d.DomainReferenceValue(v) = /*some domain value; e.g., operands.head*/
 *           val BaseReferenceValues(values) = v
 *           values...
 *       }}}
 *
 * @author Michael Eichberg
 */
object BaseReferenceValues {
    def unapply(rv: IsReferenceValue): Some[Traversable[IsReferenceValue]] = Some(rv.allValues)
}

/**
 * Defines and extractor for the null-property of reference values.
 *
 * @author Michael Eichberg
 */
object IsNullValue {
    def unapply(rv: IsReferenceValue): Boolean = rv.isNull == Yes
}
