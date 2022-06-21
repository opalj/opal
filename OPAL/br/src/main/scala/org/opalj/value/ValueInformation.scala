/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package value

import scala.annotation.switch

import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.br.ArrayType
import org.opalj.br.BaseType
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ClassHierarchy
import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.ComputationalTypeReturnAddress
import org.opalj.br.DoubleType
import org.opalj.br.DoubleVariableInfo
import org.opalj.br.FloatType
import org.opalj.br.FloatVariableInfo
import org.opalj.br.IntegerType
import org.opalj.br.IntegerVariableInfo
import org.opalj.br.LongType
import org.opalj.br.LongVariableInfo
import org.opalj.br.NullVariableInfo
import org.opalj.br.ObjectType
import org.opalj.br.ObjectVariableInfo
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.br.TopVariableInfo
import org.opalj.br.Type
import org.opalj.br.VerificationTypeInfo
import org.opalj.br.VoidType

/**
 * Encapsulates the available type information about a `DomainValue`.
 *
 * @author Michael Eichberg
 */
trait ValueInformation {

    /**
     * Returns `true` iff this value is not a legal value according to the JVM specification.
     * Such values cannot be used to perform any computations and will generally not occur
     * in static analyses unless the analysis or the bytecode is buggy.
     *
     * @note An [[IsIllegalValue]] can always be distinguished from a void value.
     */
    def isIllegalValue: Boolean

    /**
     * Returns `true` if this value represents void.
     */
    def isVoid: Boolean

    /**
     * Returns `true` in case of a value with primitive type.
     *
     * @throws IllegalStateException if this value is illegal.
     */
    def isPrimitiveValue: Boolean
    def asPrimitiveValue: IsPrimitiveValue[_ <: BaseType] = throw new ClassCastException();

    /**
     * Returns `true` if the value has a reference type.
     *
     * @throws IllegalStateException if this value is illegal.
     */
    def isReferenceValue: Boolean
    def asReferenceValue: IsReferenceValue = throw new ClassCastException();

    /**
     * Returns `Yes` if the value is _not null_ and the least upper type bound is an `ArrayType`;
     * the value is `Unknown` if the least upper type bound is `ArrayType` but the value may be null;
     * in all other cases `No` is returned; in particular if the value is known to be null. `No`
     * is also returned if the value's type is `Object` or `Seriablizable` or `Cloneable`.
     */
    def isArrayValue: Answer

    /**
     * The computational type of the value if this object represents a legal value.
     *
     * The precise computational type is, e.g., needed to calculate the effect
     * of generic stack manipulation instructions (e.g., `DUP_...` and `SWAP`)
     * on the stack as well as to calculate the jump targets of `RET` instructions
     * and to determine which values are actually copied by, e.g., the `dup_XX`
     * instructions.
     *
     * @note The computational type has to be precise/correct.
     *
     * @throws IllegalStateException if this value is illegal or void.
     */
    def computationalType: ComputationalType

    /**
     * Returns `true` if and only if the value has the computational type 2; `false` in all
     * other cases (including the case where this value is illegal!).
     */
    def hasCategory2ComputationalType: Boolean

    /**
     * The type of this value as used by the [[org.opalj.br.StackMapTable]] attribute.
     *
     * @throws IllegalStateException if this ''value represents void'' or a return address value.
     */
    def verificationTypeInfo: VerificationTypeInfo

    /**
     * Returns a `ValueInformation` object that just captures the basic information as defined
     * by this `value` framework. The returned value information object will be independent of
     * the underlying representation from which it was derived.
     */
    def toCanonicalForm: ValueInformation
}

object ValueInformation {

    /**
     * Creates a new [[ValueInformation]] object for a proper value. In case of a reference value
     * the value is either null or a value that is properly initialized; i.e., it cannot be an
     * uninitialized this/value.
     */
    def forProperValue(t: Type)(implicit classHierarchy: ClassHierarchy): ValueInformation = {
        (t.id: @switch) match {
            case VoidType.id    => TheVoidValue
            case BooleanType.id => ABooleanValue
            case ByteType.id    => AByteValue
            case CharType.id    => ACharValue
            case ShortType.id   => AShortValue
            case IntegerType.id => AnIntegerValue
            case LongType.id    => ALongValue
            case FloatType.id   => AFloatValue
            case DoubleType.id  => ADoubleValue
            case _ /*referenceTypeId*/ =>
                if (t.isObjectType) {
                    val objectType = t.asObjectType
                    AProperSObjectValue(
                        Unknown,
                        isPrecise = classHierarchy.isKnownToBeFinal(objectType),
                        objectType
                    )
                } else {
                    val arrayType = t.asArrayType
                    ASArrayValue(
                        Unknown,
                        isPrecise = classHierarchy.isKnownToBeFinal(arrayType),
                        arrayType
                    )
                }
        }
    }
}

trait IsIllegalValue extends ValueInformation {

    final override def isIllegalValue: Boolean = true

    final override def isVoid: Boolean = false

    final override def isPrimitiveValue: Boolean = throw new IllegalStateException("illegal value")

    final override def isReferenceValue: Boolean = throw new IllegalStateException("illegal value")

    final override def isArrayValue: Answer = No

    final override def computationalType: ComputationalType = {
        throw new IllegalStateException("illegal value")
    }

    final override def hasCategory2ComputationalType: Boolean = false

    final override def verificationTypeInfo: VerificationTypeInfo = TopVariableInfo

    final override def toCanonicalForm: ValueInformation = IsIllegalValue

}

case object IsIllegalValue extends IsIllegalValue

sealed trait KnownValue extends ValueInformation {

    final override def isIllegalValue: Boolean = false

}

object TheVoidValue extends KnownValue {

    override def isVoid: Boolean = true

    override def isPrimitiveValue: Boolean = false

    override def isReferenceValue: Boolean = false

    override def isArrayValue: Answer = No

    override def computationalType: ComputationalType = throw new IllegalStateException("void")

    override def hasCategory2ComputationalType: Boolean = false

    override def verificationTypeInfo: VerificationTypeInfo = {
        throw new IllegalStateException("void")
    }

    override def toCanonicalForm: ValueInformation = this

}

/**
 * A value with a well-defined computational type.
 */
trait KnownTypedValue extends KnownValue {

    final override def isVoid: Boolean = false

}

trait IsReturnAddressValue extends KnownTypedValue {

    final override def isPrimitiveValue: Boolean = false

    final override def isReferenceValue: Boolean = false

    final override def isArrayValue: Answer = No

    final override def computationalType: ComputationalType = ComputationalTypeReturnAddress

    final override def hasCategory2ComputationalType: Boolean = false

    final override def verificationTypeInfo: VerificationTypeInfo = {
        throw new IllegalStateException("return address value - cf. JVM spec.: StackMapTableAttribute")
    }

    override def toCanonicalForm: IsReturnAddressValue = AReturnAddressValue

}

case object AReturnAddressValue extends IsReturnAddressValue

/**
 * A value for which the information is available if it is a constant – and if so which – value.
 */
trait ConstantValueInformationProvider[T] {

    /**
     * The constant value that this variable takes - if it take a single constant value!
     * I.e., if the variable may take multiple different values at runtime `constantValue` will
     * return `None`.
     */
    def constantValue: Option[T]

    def asConstantBoolean: Boolean = throw new UnsupportedOperationException("not a boolean const")
    def asConstantByte: Byte = throw new UnsupportedOperationException("not a byte const")
    def asConstantShort: Short = throw new UnsupportedOperationException("not a short const")
    def asConstantChar: Char = throw new UnsupportedOperationException("not a char const")
    def asConstantInteger: Integer = throw new UnsupportedOperationException("not a integer const")
    def asConstantLong: Long = throw new UnsupportedOperationException("not a long const")
    def asConstantFloat: Float = throw new UnsupportedOperationException("not a float const")
    def asConstantDouble: Double = throw new UnsupportedOperationException("not a double const")

}

/**
 * The value has the primitive type.
 */
sealed trait IsPrimitiveValue[T <: BaseType]
    extends KnownTypedValue
    with ConstantValueInformationProvider[T#JType] {

    final override def isReferenceValue: Boolean = false

    final override def isPrimitiveValue: Boolean = true

    final override def asPrimitiveValue: IsPrimitiveValue[T] = this

    final override def isArrayValue: Answer = No

    def primitiveType: T

    final override def computationalType: ComputationalType = primitiveType.computationalType

}

object IsPrimitiveValue {

    def unapply[T <: BaseType](underlying: IsPrimitiveValue[T]): Some[T] = {
        Some(underlying.primitiveType)
    }

}

sealed trait IsIntegerLikeValue[T <: BaseType] extends IsPrimitiveValue[T] {
    final override def verificationTypeInfo: VerificationTypeInfo = IntegerVariableInfo
}

trait IsBooleanValue extends IsIntegerLikeValue[BooleanType] {
    final override def primitiveType: BooleanType = BooleanType
    final override def hasCategory2ComputationalType: Boolean = false
    override def toCanonicalForm: ValueInformation = ABooleanValue
    override def asConstantBoolean: Boolean = constantValue.get // Expected to be overridden!
}
case object ABooleanValue extends IsBooleanValue {
    override def constantValue: Option[Boolean] = None
}
case object BooleanValueTrue extends IsBooleanValue {
    override def constantValue: Option[Boolean] = Some(true)
    override def asConstantBoolean: Boolean = true
    override def toCanonicalForm: ValueInformation = this
}
case object BooleanValueFalse extends IsBooleanValue {
    override def constantValue: Option[Boolean] = Some(false)
    override def asConstantBoolean: Boolean = false
    override def toCanonicalForm: ValueInformation = this
}

trait IsByteValue extends IsIntegerLikeValue[ByteType] {
    final override def primitiveType: ByteType = ByteType
    final override def hasCategory2ComputationalType: Boolean = false
    override def toCanonicalForm: ValueInformation = AByteValue
    override def asConstantByte: Byte = constantValue.get // Expected to be overridden!
}
case object AByteValue extends IsByteValue {
    override def constantValue: Option[Byte] = None
}
case class TheByteValue(value: Byte) extends IsByteValue {
    override def constantValue: Option[Byte] = Some(value)
    override def asConstantByte: Byte = value
    override def toCanonicalForm: ValueInformation = this
}

trait IsCharValue extends IsIntegerLikeValue[CharType] {
    final override def primitiveType: CharType = CharType
    final override def hasCategory2ComputationalType: Boolean = false
    override def toCanonicalForm: ValueInformation = ACharValue
    override def asConstantChar: Char = constantValue.get // Expected to be overridden!
}
case object ACharValue extends IsCharValue {
    override def constantValue: Option[Char] = None
}
case class TheCharValue(value: Char) extends IsCharValue {
    override def constantValue: Option[Char] = Some(value)
    override def asConstantChar: Char = value
    override def toCanonicalForm: ValueInformation = this
}

trait IsShortValue extends IsIntegerLikeValue[ShortType] {
    final override def primitiveType: ShortType = ShortType
    final override def hasCategory2ComputationalType: Boolean = false
    override def toCanonicalForm: ValueInformation = AShortValue
    override def asConstantShort: Short = constantValue.get // Expected to be overridden!
}
case object AShortValue extends IsShortValue {
    override def constantValue: Option[Short] = None
}
case class TheShortValue(value: Short) extends IsShortValue {
    override def constantValue: Option[Short] = Some(value)
    override def asConstantShort: Short = value
    override def toCanonicalForm: ValueInformation = this
}

trait IsIntegerValue extends IsIntegerLikeValue[IntegerType] {
    final override def primitiveType: IntegerType = IntegerType
    final override def hasCategory2ComputationalType: Boolean = false
    override def toCanonicalForm: ValueInformation = AnIntegerValue
    override def asConstantInteger: Integer = constantValue.get // Expected to be overridden!
    def lowerBound: Int
    def upperBound: Int
}
case object AnIntegerValue extends IsIntegerValue {
    final override def constantValue: Option[Int] = None
    final override def lowerBound: Int = Int.MinValue
    final override def upperBound: Int = Int.MaxValue
}
case class TheIntegerValue(value: Int) extends IsIntegerValue {
    final override def constantValue: Option[Int] = Some(value)
    override def asConstantInteger: Integer = value
    final override def lowerBound: Int = value
    final override def upperBound: Int = value
    override def toCanonicalForm: ValueInformation = this
}

trait IsFloatValue extends IsPrimitiveValue[FloatType] {
    final override def primitiveType: FloatType = FloatType
    final override def hasCategory2ComputationalType: Boolean = false
    final override def verificationTypeInfo: VerificationTypeInfo = FloatVariableInfo
    override def toCanonicalForm: ValueInformation = AFloatValue
    override def asConstantFloat: Float = constantValue.get // Expected to be overridden!
}
case object AFloatValue extends IsFloatValue {
    override def constantValue: Option[Float] = None
}
case class TheFloatValue(value: Float) extends IsFloatValue {
    override def constantValue: Option[Float] = Some(value)
    override def asConstantFloat: Float = value
    override def toCanonicalForm: ValueInformation = this
}

trait IsLongValue extends IsPrimitiveValue[LongType] {
    final override def primitiveType: LongType = LongType
    final override def hasCategory2ComputationalType: Boolean = true
    final override def verificationTypeInfo: VerificationTypeInfo = LongVariableInfo
    override def toCanonicalForm: ValueInformation = ALongValue
    override def asConstantLong: Long = constantValue.get // Expected to be overridden!
}
case object ALongValue extends IsLongValue {
    override def constantValue: Option[Long] = None
}
case class TheLongValue(value: Long) extends IsLongValue {
    override def constantValue: Option[Long] = Some(value)
    override def asConstantLong: Long = value
    override def toCanonicalForm: ValueInformation = this
}

trait IsDoubleValue extends IsPrimitiveValue[DoubleType] {
    final override def primitiveType: DoubleType = DoubleType
    final override def hasCategory2ComputationalType: Boolean = true
    final override def verificationTypeInfo: VerificationTypeInfo = DoubleVariableInfo
    override def toCanonicalForm: ValueInformation = ADoubleValue
    override def asConstantDouble: Double = constantValue.get // Expected to be overridden!
}
case object ADoubleValue extends IsDoubleValue {
    override def constantValue: Option[Double] = None
}
case class TheDoubleValue(value: Double) extends IsDoubleValue {
    override def constantValue: Option[Double] = Some(value)
    override def asConstantDouble: Double = value
    override def toCanonicalForm: ValueInformation = this
}

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

    final override def isPrimitiveValue: Boolean = false

    final override def isReferenceValue: Boolean = true
    final override def asReferenceValue: IsReferenceValue = this

    final override def computationalType: ComputationalType = ComputationalTypeReference

    final override def hasCategory2ComputationalType: Boolean = false

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
     */
    def isNull: Answer

    /**
     * Returns `true` if the type information is precise. I.e., the type returned by
     * `upperTypeBound` precisely models the runtime type of the value.
     *  If, `isPrecise` returns true, the type of this value can
     * generally be assumed to represent a class type (not an interface type) or
     * an array type. However, this domain also supports the case that `isPrecise`
     * returns `true` even though the associated type identifies an interface type
     * or an abstract class type. The later case may be interesting in the context
     * of classes that are generated at run time.
     */
    def isPrecise: Boolean

    /**
     * Returns '''the type of the upper type bound''' if the upper type bound contains
     * exactly one element. That is, the function is only always defined iff the type
     * is precise.
     */
    final def asReferenceType: ReferenceType = {
        if (!upperTypeBound.isSingletonSet) {
            throw new ClassCastException(s"$upperTypeBound.size >= 1");
        }

        upperTypeBound.head
    }

    /**
     * The least upper type bound of the value.
     *
     * `None` if and only if the underlying value is `null`.
     */
    def leastUpperType: Option[ReferenceType]

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
     * @note The function `isValueASubtypeOf` is not defined if `isNull` returns `Yes`;
     *      if `isNull` is `Unknown` then the result is given under the
     *      assumption that the value is not `null` at runtime.
     *      In other words, if this value represents `null` this method is not supported.
     *      If the value is null, the effect/interpretation of a subtype of query is
     *      context dependent (isInstanceOf/checkCast).
     */
    def isValueASubtypeOf(
        referenceType: ReferenceType
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Answer

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
    def baseValues: Iterable[IsReferenceValue] // ... technically a set of IsBaseReferenceValue

    /**
     * The set of base values (`IsReferenceValue`) this value abstracts over.
     * This set is never empty and contains this value if this value does not (further) abstract
     * over other reference values; otherwise it only contains the base values,
     * but not `this` value.
     *
     * @note Primarily defined as a convenience interface.
     */
    def allValues: Iterable[IsReferenceValue]

    override def toCanonicalForm: IsReferenceValue
}

trait IsBaseReferenceValue extends IsReferenceValue {
    final override def baseValues: Iterable[this.type] = Nil
    final override def allValues: Iterable[this.type] = List(this)
    override def toCanonicalForm: IsBaseReferenceValue
}

trait IsNullValue extends IsBaseReferenceValue {

    final override def isArrayValue: Answer = No

    final override def isNull: Answer = Yes
    final override def isPrecise: Boolean = true
    final override def upperTypeBound: UIDSet[_ <: ReferenceType] = UIDSet.empty
    final override def leastUpperType: None.type = None
    final override def verificationTypeInfo: VerificationTypeInfo = NullVariableInfo

    final override def isValueASubtypeOf(
        referenceType: ReferenceType
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Answer = {
        throw new IllegalStateException("null value")
    }

    override def toCanonicalForm: IsNullValue = IsNullValue
}
case object IsNullValue extends IsNullValue {
    def unapply(rv: IsReferenceValue): Boolean = rv.isNull == Yes
    override def toString: String = "NullValue"
}

trait IsMObjectValue extends IsBaseReferenceValue {

    /**
     * All types from which the (precise, but unknown) type of the represented value inherits.
     * I.e., the value represented by this domain value is known to have a type that
     * (in)directly inherits from all given types at the same time. Hence, the upperTypeBound
     * may contain at most one class type.
     */
    override def upperTypeBound: UIDSet[ObjectType]

    final override def isArrayValue: Answer = No

    assert(upperTypeBound.size > 1)

    final override def verificationTypeInfo: VerificationTypeInfo = {
        ObjectVariableInfo(leastUpperType.get)
    }

    // Non-final to enable
    override def isPrecise: Boolean = false

    /**
     * Determines if this value is a subtype of the given supertype by
     * delegating to the `isSubtypeOf(ReferenceType,ReferenceType)` method of the
     * domain.
     *
     * @note This is a very basic implementation that cannot determine that this
     *      value is '''not''' a subtype of the given type as this implementation
     *      does not distinguish between class types and interface types.
     */
    override def isValueASubtypeOf(
        supertype: ReferenceType
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Answer = {
        var isASubtypeOf: Answer = No
        upperTypeBound foreach { anUpperTypeBound =>
            classHierarchy.isASubtypeOf(anUpperTypeBound, supertype) match {
                case Yes     => return Yes; // <= Shortcut evaluation
                case Unknown => isASubtypeOf = Unknown
                case No      => /*nothing to do*/
            }
        }
        /* No | Unknown*/
        // In general, we could check whether a type exists that is a
        // proper subtype of the type identified by this value's type bounds
        // and that is also a subtype of the given `supertype`.
        //
        // If such a type does not exist the answer is truly `no` (if we
        // assume that we know the complete type hierarchy);
        // if we don't know the complete hierarchy or if we currently
        // analyze a library the answer generally has to be `Unknown`
        // unless we also consider the classes that are final or ....

        isASubtypeOf match {
            // Yes is not possible here!

            case No if (
                supertype.isArrayType && upperTypeBound != ObjectType.SerializableAndCloneable
            ) =>
                // even if the upper bound is not precise we are now 100% sure
                // that this value is not a subtype of the given supertype
                No
            case _ =>
                Unknown
        }
    }

    override def toCanonicalForm: IsBaseReferenceValue = {
        AProperMObjectValue(isNull, isPrecise, upperTypeBound, leastUpperType)
    }
}

/**
 * Represents an object value which is either null or properly initialized.
 */
case class AProperMObjectValue(
        override val isNull:    Answer,
        override val isPrecise: Boolean,
        upperTypeBound:         UIDSet[ObjectType],
        leastUpperType:         Option[ReferenceType] // actually always Some[ObjectType]
) extends IsMObjectValue {
    override def toCanonicalForm: IsMObjectValue = this
    override def toString: String = {
        "ProperMObjectValue("+
            s"type=${upperTypeBound.map(_.toJava).toList.sorted.mkString(" with ")},"+
            s"isNull=$isNull,isPrecise=$isPrecise)"
    }

}

trait IsSReferenceValue[T <: ReferenceType] extends IsBaseReferenceValue {

    def theUpperTypeBound: T

    final override def leastUpperType: Option[ReferenceType] = Some(theUpperTypeBound)

    final override def upperTypeBound: UIDSet[T] = new UIDSet1(theUpperTypeBound)

}

trait IsSObjectValue extends IsSReferenceValue[ObjectType] {

    final override def isArrayValue: Answer = No

    override def isValueASubtypeOf(
        supertype: ReferenceType
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Answer = {
        val subtype = theUpperTypeBound
        classHierarchy.isASubtypeOf(subtype, supertype) match {
            case Yes =>
                Yes
            case No if isPrecise
                || (
                    supertype.isArrayType &&
                    // and it is impossible that this value is actually an array...
                    (subtype ne ObjectType.Object) &&
                    (subtype ne ObjectType.Serializable) &&
                    (subtype ne ObjectType.Cloneable)
                ) || (
                        // If both types represent class types and it is not
                        // possible that some value of this type may be a subtype
                        // of the given supertype, the answer "No" is correct.
                        supertype.isObjectType &&
                        classHierarchy.isKnown(supertype.asObjectType) &&
                        classHierarchy.isKnown(subtype) &&
                        classHierarchy.isInterface(supertype.asObjectType).isNo &&
                        classHierarchy.isInterface(subtype).isNo &&
                        classHierarchy.isASubtypeOf(supertype, subtype).isNo
                    ) =>
                No
            case _ if isPrecise &&
                // Note "reflexivity" is already captured by the first isSubtypeOf call
                classHierarchy.isSubtypeOf(supertype, subtype) =>
                No
            case _ =>
                Unknown
        }
    }

    // Non-final to enable subclasses to identify cases of uninitializedThis/Variable cases.
    override def verificationTypeInfo: VerificationTypeInfo = {
        ObjectVariableInfo(theUpperTypeBound)
    }

    // Non-final to enable subclasses to provide more detailed information.
    override def toCanonicalForm: IsBaseReferenceValue = {
        ASObjectValue(isNull, isPrecise, theUpperTypeBound)
    }
}

/**
 * Represents some object value which may be null and may even not be properly initialized yet.
 */
case class ASObjectValue(
        isNull:                 Answer,
        override val isPrecise: Boolean,
        theUpperTypeBound:      ObjectType
) extends IsSObjectValue {
    override def toCanonicalForm: IsSObjectValue = this
    override def toString: String = {
        s"SObjectValue(type=${theUpperTypeBound.toJava},isNull=$isNull,isPrecise=$isPrecise)"
    }

}

/**
 * Represents an object value which is either null or properly initialized.
 */
case class AProperSObjectValue(
        isNull:                 Answer,
        override val isPrecise: Boolean,
        theUpperTypeBound:      ObjectType
) extends IsSObjectValue {
    override def toCanonicalForm: IsSObjectValue = this
    override def toString: String = {
        s"ProperSObjectValue(type=${theUpperTypeBound.toJava},isNull=$isNull,isPrecise=$isPrecise)"
    }
}

trait IsSArrayValue extends IsSReferenceValue[ArrayType] {

    final override def isArrayValue: Answer = isNull.negate // isNull is either "No" or "Unknown"

    override def isValueASubtypeOf(
        supertype: ReferenceType
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Answer = {
        classHierarchy.isASubtypeOf(theUpperTypeBound, supertype) match {
            case Yes => Yes
            case No if isPrecise ||
                // the array's supertypes: Object, Serializable and Cloneable
                // are handled by domain.isASubtypeOf
                supertype.isObjectType ||
                theUpperTypeBound.elementType.isBaseType ||
                (
                    supertype.isArrayType &&
                    supertype.asArrayType.elementType.isBaseType &&
                    (
                        theUpperTypeBound.dimensions >= supertype.asArrayType.dimensions ||
                        (theUpperTypeBound.componentType ne ObjectType.Object)
                    )
                ) => No
            case _ => Unknown
        }
    }

    // Non-final to enable subclasses to identify cases of uninitializedThis/Variable cases.
    final override def verificationTypeInfo: VerificationTypeInfo = {
        ObjectVariableInfo(theUpperTypeBound)
    }
}

case class ASArrayValue(
        override val isNull:    Answer,
        override val isPrecise: Boolean,
        theUpperTypeBound:      ArrayType
) extends IsSArrayValue {
    override def toCanonicalForm: IsSArrayValue = this
    override def toString: String = {
        "SArrayValue("+
            s"type=${theUpperTypeBound.toJava},"+
            s"isNull=$isNull,isPrecise=$isPrecise,length=<N/A>)"
    }
}

case class ASArrayWithLengthValue(
        length:            Int,
        theUpperTypeBound: ArrayType
) extends IsSArrayValue {
    override def isNull: Answer = No
    override def isPrecise: Boolean = true
    override def toCanonicalForm: IsSArrayValue = this
    override def toString: String = {
        s"SArrayValue("+
            s"type=${theUpperTypeBound.toJava},"+
            s"isNull=$isNull,isPrecise=$isPrecise,length=$length)"
    }

}

private[value] trait IsPreciseNonNullReferenceValue extends IsSObjectValue {
    override def isNull: No.type = No
    override def isPrecise: Boolean = true
}

trait IsStringValue
    extends IsPreciseNonNullReferenceValue
    with ConstantValueInformationProvider[String] {

    def value: String

    final override def theUpperTypeBound: ObjectType = ObjectType.String

    final override def verificationTypeInfo: VerificationTypeInfo = {
        ObjectVariableInfo(ObjectType.String)
    }

    final override def isValueASubtypeOf(
        supertype: ReferenceType
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Answer = {
        supertype.id match {
            case ObjectType.ObjectId
                | ObjectType.SerializableId
                | ObjectType.CloneableId
                | ObjectType.ComparableId
                | ObjectType.StringId =>
                Yes
            case _ => No
        }
    }
    override def constantValue: Option[String] = Some(value)
}

/**
 * Represents a constant string value. If the string is not yet completely initialized `value`
 * is `null`. This can never be the case for the parameters and return values of methods and
 * also for the values stored in fields.
 */
case class TheStringValue(value: String) extends IsStringValue {
    override def toCanonicalForm: TheStringValue = this
}

trait IsClassValue
    extends IsPreciseNonNullReferenceValue
    with ConstantValueInformationProvider[Type] {

    // We hard-code the type hierarchy related to "java.lang.Class".
    val AnnotatedElement = ObjectType("java/lang/reflect/AnnotatedElement")
    val GenericDeclaration = ObjectType("java/lang/reflect/GenericDeclaration")
    val Type = ObjectType("java/lang/reflect/Type")

    def value: Type

    final override def theUpperTypeBound: ObjectType = ObjectType.Class
    final override def verificationTypeInfo: VerificationTypeInfo = {
        ObjectVariableInfo(ObjectType.Class)
    }

    final override def isValueASubtypeOf(
        supertype: ReferenceType
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Answer = {
        supertype.id match {
            case ObjectType.ObjectId
                | ObjectType.ClassId
                | ObjectType.SerializableId
                | AnnotatedElement.id
                | Type.id
                | GenericDeclaration.id =>
                Yes
            case _ => No
        }
    }

    override def constantValue: Option[Type] = Some(value)
}

/**
 * Represents a constant class value. If the class is not yet completely initialized `value`
 * is `null`. This can never be the case for the parameters and return values of methods and
 * also for the values stored in fields.
 */
case class TheClassValue(value: Type) extends IsClassValue {
    override def toCanonicalForm: TheClassValue = this
}

/**
 * Extractor for instances of `IsReferenceValue` objects.
 *
 * @author Michael Eichberg
 */
object TypeOfReferenceValue {

    def unapply(rv: IsReferenceValue): Some[UIDSet[_ <: ReferenceType]] = Some(rv.upperTypeBound)

}

trait IsMultipleReferenceValue extends IsReferenceValue {

    assert(baseValues.nonEmpty)

    override def allValues: Iterable[IsReferenceValue] = this.baseValues

    override def verificationTypeInfo: VerificationTypeInfo = {
        if (isNull.isYes) {
            NullVariableInfo
        } else {
            ObjectVariableInfo(leastUpperType.get)
        }
    }

    override def isValueASubtypeOf(
        supertype: ReferenceType
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Answer = {
        // Recall that the client has to make an "isNull" check before calling
        // isValueASubtypeOf. Hence, at least one of the possible reference values
        // has to be non null and this value's upper type bound has to be non-empty.

        // It may the case that the subtype relation of each individual value – 
        // when compared with supertype - is Unknown, but that the type of the
        // value as a whole is still known to be a subtype
        val isASubtypeOf = classHierarchy.isASubtypeOf(this.upperTypeBound, supertype)
        if (isASubtypeOf eq Yes)
            return Yes;
        if ((isASubtypeOf eq No) && isPrecise)
            return No;

        // Recall that the runtime type of this value can still be a subtype of supertype
        // even if this upperTypeBound is not a subtype of supertype.
        val values = baseValues.iterator.filter(_.isNull.isNoOrUnknown)
        var answer: Answer = values.next().isValueASubtypeOf(supertype)
        values foreach { value => /* the first value is already removed */
            if (answer eq Unknown)
                return answer; //isSubtype

            answer = answer join value.isValueASubtypeOf(supertype)
        }

        answer
    }

    override def toCanonicalForm: IsReferenceValue = {
        var uniqueBaseValues = Set.empty[IsReferenceValue]
        uniqueBaseValues = baseValues.foldLeft(uniqueBaseValues)(_ + _.toCanonicalForm)
        // ...toSet is required because we potentially drop domain specific information
        // and afterwards the values are identical.
        if (uniqueBaseValues.size == 1 &&
            uniqueBaseValues.head.isNull == this.isNull &&
            uniqueBaseValues.head.isPrecise == this.isPrecise &&
            uniqueBaseValues.head.upperTypeBound == this.upperTypeBound) {
            uniqueBaseValues.head
        } else {
            AMultipleReferenceValue(
                uniqueBaseValues,
                isNull,
                isPrecise,
                upperTypeBound,
                leastUpperType
            )
        }
    }
}

case class AMultipleReferenceValue(
        baseValues:     Iterable[IsReferenceValue],
        isNull:         Answer,
        isPrecise:      Boolean,
        upperTypeBound: UIDSet[_ <: ReferenceType],
        leastUpperType: Option[ReferenceType] // None in case of "null"
) extends IsMultipleReferenceValue {

    assert((isNull.isYes && leastUpperType.isEmpty) || (isNull.isNoOrUnknown && leastUpperType.isDefined))
    assert(baseValues.forall(_.getClass.getPackage.getName == ("org.opalj.value")))

    final override def isArrayValue: Answer = {
        leastUpperType match {
            case Some(_: ArrayType) => isNull.negate // isNull is either No or unknown
            case _                  => No
        }
    }

    override def toCanonicalForm: IsReferenceValue = this
    override def toString: String = {
        "MultipleReferenceValue("+
            s"type=${upperTypeBound.map(_.toJava).toList.sorted.mkString(" with ")},"+
            s"isNull=$isNull,isPrecise=$isPrecise,"+
            s"baseValues=${baseValues.map(_.toString).mkString("{ ", ", ", " }")})"
    }
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
    def unapply(rv: IsReferenceValue): Some[Iterable[IsReferenceValue]] = Some(rv.allValues)
}
