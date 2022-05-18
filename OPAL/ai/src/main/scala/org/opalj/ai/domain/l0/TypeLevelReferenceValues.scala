/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.collection.immutable.UIDSet
import org.opalj.value.ASArrayValue
import org.opalj.value.IsNullValue
import org.opalj.value.IsReferenceValue
import org.opalj.value.IsSArrayValue
import org.opalj.value.IsSReferenceValue
import org.opalj.value.ValueInformation
import org.opalj.br.ArrayType
import org.opalj.br.ClassHierarchy
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.Type

/**
 * Implements the foundations for performing computations related to reference values.
 *
 * ==Extending/Implementing This Domain==
 * The following implementation decisions need to be taken into account when
 * inheriting from this trait:
 *  - By default equality of `DomainValue`s that represent reference values is
 *    reference based. I.e., two instances of `DomainValue`s that represent
 *    reference values are never equal. However, subclasses may implement their
 *    own strategy.
 *  - Instances of `DomainValue`s are always immutable or are at least considered and
 *    treated as immutable.
 *    Every update of a value's properties creates a new value. This is a general design
 *    decision underlying OPAL and should not be changed.
 *  - A new instance of a `DomainValue` is always exclusively created by one of the
 *    factory methods. (The factory methods generally start with a capital letter
 *    and are correspondingly documented.) This greatly facilitates domain adaptability
 *    and selective customizations.
 *
 * @author Michael Eichberg
 */
trait TypeLevelReferenceValues extends GeneralizedArrayHandling with AsJavaObject {
    domain: IntegerValuesDomain with Configuration =>

    /**
     * Merges those exceptions that have the same upper type bound. This ensures
     * that per upper type bound only one [[ValuesDomain.DomainValue]] (which may be a
     * `MultipleReferenceValues`) is used. For those values that are merged, the
     * given `pc` is used.
     */
    def mergeMultipleExceptionValues(
        pc:  Int,
        v1s: ExceptionValues,
        v2s: ExceptionValues
    ): ExceptionValues = {

        var v: List[ExceptionValue] = Nil
        var remainingv2s = v2s
        v1s foreach { v1 =>
            val v1UTB = domain.asObjectValue(v1).upperTypeBound
            remainingv2s find (domain.asObjectValue(_).upperTypeBound == v1UTB) match {
                case Some(v2) =>
                    remainingv2s = remainingv2s filterNot (_ == v2)
                    v = mergeDomainValues(pc, v1, v2).asInstanceOf[ExceptionValue] :: v
                case None =>
                    v = v1 :: v
            }
        }
        v ++ remainingv2s
    }

    /**
     * Merges two computations that both return some `DomainValue` and some
     * `ExceptionValues`. If values are merged the merged value will use the
     * specified `pc`.
     */
    protected[this] def mergeDEsComputations(
        pc: Int,
        c1: Computation[DomainValue, ExceptionValues],
        c2: Computation[DomainValue, ExceptionValues]
    ): Computation[DomainValue, ExceptionValues] = {

        c1 match {
            case ComputationWithResultAndException(r1, e1) =>
                c2 match {
                    case ComputationWithResultAndException(r2, e2) =>
                        ComputedValueOrException(
                            mergeDomainValues(pc, r1, r2),
                            mergeMultipleExceptionValues(pc, e1, e2)
                        )
                    case ComputationWithResult(r2) =>
                        ComputedValueOrException(mergeDomainValues(pc, r1, r2), e1)
                    case ComputationWithException(e2) =>
                        ComputedValueOrException(r1, mergeMultipleExceptionValues(pc, e1, e2))
                    case _ => throw new MatchError(c2)
                }

            case ComputationWithResult(r1) =>
                c2 match {
                    case ComputationWithResultAndException(r2, e2) =>
                        ComputedValueOrException(mergeDomainValues(pc, r1, r2), e2)
                    case ComputationWithResult(r2) =>
                        ComputedValue(mergeDomainValues(pc, r1, r2))
                    case ComputationWithException(e2) =>
                        ComputedValueOrException(r1, e2)
                    case _ => throw new MatchError(c2)

                }

            case ComputationWithException(e1) =>
                c2 match {
                    case ComputationWithResultAndException(r2, e2) =>
                        ComputedValueOrException(r2, mergeMultipleExceptionValues(pc, e1, e2))
                    case ComputationWithResult(r2) =>
                        ComputedValueOrException(r2, e1)
                    case ComputationWithException(e2) =>
                        ThrowsException(mergeMultipleExceptionValues(pc, e1, e2))
                    case _ => throw new MatchError(c2)
                }
            case _ => throw new MatchError(c1)
        }
    }

    /**
     * Merges two computations that both resulted in at most one `ExceptionValue` each.
     *
     * If values are merged the merged value will use the specified `pc`.
     */
    protected[this] def mergeEsComputations(
        pc: Int,
        c1: Computation[Nothing, ExceptionValues],
        c2: Computation[Nothing, ExceptionValues]
    ): Computation[Nothing, ExceptionValues] = {

        (c1, c2) match {
            case (ComputationWithException(e1), ComputationWithException(e2)) =>
                ComputationWithSideEffectOrException(mergeMultipleExceptionValues(pc, e1, e2))
            case (ComputationWithException(_), _ /*ComputationWithoutException*/ ) =>
                c1
            case (_ /*ComputationWithoutException*/ , ComputationWithException(_)) =>
                c2
            case _ =>
                ComputationWithSideEffectOnly
        }
    }

    /**
     * Merges two computations that both resulted in at most one `DomainValue` or
     * at most one `ExceptionValue`.
     *
     * If values are merged the merged value will use the specified `pc`.
     */
    protected[this] def mergeDEComputations(
        pc: Int,
        c1: Computation[DomainValue, ExceptionValue],
        c2: Computation[DomainValue, ExceptionValue]
    ): Computation[DomainValue, ExceptionValue] = {

        c1 match {
            case ComputationWithResultAndException(r1, e1) =>
                c2 match {
                    case ComputationWithResultAndException(r2, e2) =>
                        ComputedValueOrException(
                            mergeDomainValues(pc, r1, r2) /*Value*/ ,
                            mergeDomainValues(pc, e1, e2).asInstanceOf[ExceptionValue]
                        )
                    case ComputationWithResult(r2) =>
                        ComputedValueOrException(mergeDomainValues(pc, r1, r2), e1)
                    case ComputationWithException(e2) =>
                        ComputedValueOrException(
                            r1,
                            mergeDomainValues(pc, e1, e2).asInstanceOf[ExceptionValue]
                        )
                    case _ => throw new MatchError(c2)
                }

            case ComputationWithResult(r1) =>
                c2 match {
                    case ComputationWithResultAndException(r2, e2) =>
                        ComputedValueOrException(mergeDomainValues(pc, r1, r2), e2)
                    case ComputationWithResult(r2) =>
                        ComputedValue(mergeDomainValues(pc, r1, r2))
                    case ComputationWithException(e2) =>
                        ComputedValueOrException(r1, e2)
                    case _ => throw new MatchError(c2)
                }

            case ComputationWithException(e1) =>
                c2 match {
                    case ComputationWithResultAndException(r2, e2) =>
                        ComputedValueOrException(
                            r2,
                            mergeDomainValues(pc, e1, e2).asInstanceOf[ExceptionValue]
                        )
                    case ComputationWithResult(r2) =>
                        ComputedValueOrException(r2, e1)
                    case ComputationWithException(e2) =>
                        ThrowsException(
                            mergeDomainValues(pc, e1, e2).asInstanceOf[ExceptionValue]
                        )
                    case _ => throw new MatchError(c2)
                }
            case _ => throw new MatchError(c1)
        }
    }

    // ---------------------------------------------------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // ---------------------------------------------------------------------------------------------

    type AReferenceValue <: DomainReferenceValue with ReferenceValueLike

    type DomainObjectValue <: ObjectValueLike with AReferenceValue

    type DomainArrayValue <: ArrayValueLike with AReferenceValue

    type DomainNullValue <: NullValueLike with AReferenceValue

    trait ArrayAbstraction {

        def load(pc: Int, index: DomainValue): ArrayLoadResult

        def store(pc: Int, value: DomainValue, index: DomainValue): ArrayStoreResult

        def length(pc: Int): Computation[DomainValue, ExceptionValue]
    }

    /**
     * Abstracts over all values with computational type `reference`. I.e.,
     * abstracts over class and array values and also the `null` value.
     */
    trait ReferenceValueLike extends super.ReferenceValue with ArrayAbstraction {
        this: AReferenceValue =>

        final override def asDomainReferenceValue: DomainReferenceValue = this

    }

    /**
     * A reference value with a single (upper) type (bound).
     */
    protected[this] trait SReferenceValue[T <: ReferenceType]
        extends ReferenceValueLike
        with IsSReferenceValue[T] {
        this: AReferenceValue =>

        def theUpperTypeBound: T

        final def classHierarchy: ClassHierarchy = domain.classHierarchy

        final override def summarize(pc: Int): this.type = this

        override def toString: String = theUpperTypeBound.toJava

    }

    /**
     * Represents the runtime value `null`. Null values are basically found in the
     * following two cases:
     *  1. The value `null` was pushed onto the stack using `aconst_null`.
     *  1. A reference value that is not guaranteed to be non-null is tested against
     *    `null` using `ifnull` or `ifnonnull` and we are now on the branch where
     *    the value has to be `null`.
     *
     * Depending on the precision of the domain `null` values may also be returned by
     * method calls or field reads.
     */
    protected trait NullValueLike extends ReferenceValueLike with IsNullValue {
        value: DomainNullValue =>

        // IMPLEMENTATION OF THE ARRAY RELATED METHODS
        //

        final override def load(pc: Int, index: DomainValue): ArrayLoadResult =
            justThrows(VMNullPointerException(pc))

        final override def store(
            pc:    Int,
            value: DomainValue,
            index: DomainValue
        ): ArrayStoreResult = {
            justThrows(VMNullPointerException(pc))
        }

        final override def length(pc: Int): Computation[DomainValue, ExceptionValue] = {
            throws(VMNullPointerException(pc))
        }

        override def summarize(pc: Int): this.type = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = target.NullValue(pc)

        override def toString: String = "ReferenceValue(null)"
    }

    /**
     * Represents a class/interface value which may have a single class and/or
     * multiple interfaces as its upper type bound.
     */
    protected[this] trait ObjectValueLike extends ReferenceValueLike {
        value: DomainObjectValue =>

    }

    /**
     * Represents an array value.
     */
    protected[this] trait ArrayValueLike extends ReferenceValueLike with IsSArrayValue {
        value: DomainArrayValue =>

        /**
         * Returns `Yes` if we can statically determine that the given value can
         * be stored in the array represented by this `ArrayValue`.
         */
        /*ABSTRACT*/ def isAssignable(value: DomainValue): Answer

        override def toCanonicalForm: IsSArrayValue = {
            ASArrayValue(isNull, isPrecise, theUpperTypeBound)
        }

        /**
         * Called by the load method if the index is potentially valid.
         */
        /*ABSTRACT*/ protected def doLoad(
            pc:                  Int,
            index:               DomainValue,
            potentialExceptions: ExceptionValues
        ): ArrayLoadResult

        def isIndexValid(pc: Int, index: DomainValue): Answer =
            length.map { l: Int =>
                intIsSomeValueNotInRange(pc, index, 0, l - 1) match {
                    case No => Yes
                    case Yes =>
                        intIsSomeValueInRange(pc, index, 0, l - 1) match {
                            case No                  => No
                            case _ /*Yes | Unknown*/ => Unknown
                        }
                    case Unknown => Unknown
                }
            }.getOrElse(if (intIsLessThan0(pc, index).isYes) No else Unknown)

        /**
         * @note It is in general not necessary to override this method. If you need some
         *      special handling if a value is loaded from an array, override the method
         *      [[doLoad]].
         */
        override def load(pc: Int, index: DomainValue): ArrayLoadResult = {
            // The case "this.isNull == Yes" will not occur as the value "null" is always
            // represented by an instance of the respective class.
            val isIndexValid = this.isIndexValid(pc, index)
            if (isIndexValid.isNo)
                return justThrows(VMArrayIndexOutOfBoundsException(pc));

            var thrownExceptions: List[ExceptionValue] = Nil
            if (isNull.isUnknown && throwNullPointerExceptionOnArrayAccess)
                thrownExceptions = VMNullPointerException(pc) :: thrownExceptions
            if (isIndexValid.isUnknown && throwArrayIndexOutOfBoundsException)
                thrownExceptions = VMArrayIndexOutOfBoundsException(pc) :: thrownExceptions
            doLoad(pc, index, thrownExceptions)
        }

        /**
         * Called by the store method if the value is potentially assignable and if
         * the index is potentially valid.
         */
        /*ABSTRACT*/ protected def doStore(
            pc:               Int,
            value:            DomainValue,
            index:            DomainValue,
            thrownExceptions: ExceptionValues
        ): ArrayStoreResult

        /**
         * @note It is in general not necessary to override this method. If you need some
         *      special handling if a value is stored in an array, override the method
         *      [[doStore]].
         */
        override def store(
            pc:    Int,
            value: DomainValue,
            index: DomainValue
        ): ArrayStoreResult = {
            // @note
            // The case "this.isNull == Yes" will not occur as the value "null" is always
            // represented by an instance of the respective class
            val isIndexValid = this.isIndexValid(pc, index)
            if (isIndexValid.isNo)
                return justThrows(VMArrayIndexOutOfBoundsException(pc))

            val isAssignable = this.isAssignable(value)
            if (isAssignable.isNo)
                return justThrows(VMArrayStoreException(pc))

            var thrownExceptions: List[ExceptionValue] = Nil
            if (isIndexValid.isUnknown && throwArrayIndexOutOfBoundsException)
                thrownExceptions = VMArrayIndexOutOfBoundsException(pc) :: thrownExceptions
            if (isAssignable.isUnknown && throwArrayStoreException)
                thrownExceptions = VMArrayStoreException(pc) :: thrownExceptions
            if (isNull.isUnknown && throwNullPointerExceptionOnArrayAccess)
                thrownExceptions = VMNullPointerException(pc) :: thrownExceptions

            doStore(pc, value, index, thrownExceptions)
        }

        /**
         * Returns the length of this array, if this information is available.
         */
        def length: Option[Int] = None // IMPROVE Define and use IntOption

        final def doGetLength(pc: Int): DomainValue = {
            length.map(IntegerValue(pc, _)).getOrElse(IntegerValue(pc))
        }

        override def length(pc: Int): Computation[DomainValue, ExceptionValue] = {
            if (isNull == Unknown && throwNullPointerExceptionOnArrayAccess)
                ComputedValueOrException(doGetLength(pc), VMNullPointerException(pc))
            else
                ComputedValue(doGetLength(pc))
        }
    }

    /**
     * Returns the given value as a [[DomainReferenceValue]]. Basically just performs
     * a type cast and is intended to be used to communicate that the value has
     * to be a reference value (if the underlying byte code is valid.)
     */
    def asReferenceValue(value: DomainValue): AReferenceValue = value.asInstanceOf[AReferenceValue]

    def asObjectValue(value: DomainValue): DomainObjectValue = value.asInstanceOf[DomainObjectValue]

    def asArrayAbstraction(value: DomainValue): ArrayAbstraction = {
        value.asInstanceOf[ArrayAbstraction]
    }

    // -----------------------------------------------------------------------------------
    //
    // QUESTION'S ABOUT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Tests if both values refer to the same object instance.
     *
     * Though this is in general intractable, there are some cases where a definitive
     * answer is possible.
     *
     * This implementation completely handles the case where at least one value
     * definitively represents the `null` value.
     * Additionally, if we have precise type information and the types are different,
     * `No` is returned. Otherwise, `Unknown` is returned.
     *
     * @note This method is intended to be overridden by subclasses and may be the first
     *      one that is called (by means of `super`) by the overriding method to handle checks
     *      related to null. E.g.
     *      {{{
     *      super.areEqualReferences(value1,value2).ifUnknown {
     *          ...
     *      }
     *      }}}
     *
     * @param value1 A value of type `ReferenceValue`.
     * @param value2 A value of type `ReferenceValue`.
     */
    override def refAreEqual(pc: Int, value1: DomainValue, value2: DomainValue): Answer = {
        val v1 = asReferenceValue(value1)
        val v2 = asReferenceValue(value2)
        val value1IsNull = v1.isNull
        val value2IsNull = v2.isNull
        if (value1IsNull.isYes)
            // the answer is unknown if the second value is unknown, no if the second
            // value is no and yes if the second value is also yes
            value2IsNull
        else if (value2IsNull.isYes)
            // value1IsNull is either No or unknown, both represents the correct answer
            value1IsNull
        else {
            val v1UTB = v1.upperTypeBound
            val v2UTB = v2.upperTypeBound
            if (v1.isPrecise && v2.isPrecise)
                if (v1UTB != v2UTB)
                    // two objects with different runtime types are never equal
                    No
                else
                    // though both values have the same runtime type, we don't know
                    // if they refer to the same object
                    Unknown
            else {
                val ch = classHierarchy
                // - both values may not be null
                // - at least one value is not precise
                if (ch.isASubtypeOf(v1UTB, v2UTB).isNo &&
                    ch.isASubtypeOf(v2UTB, v1UTB).isNo &&
                    // two interfaces that are not in an inheritance relation can
                    // still be implemented by the same class and, hence, the references
                    // can still be equal
                    v1UTB.exists(t => t.isObjectType && ch.isInterface(t.asObjectType).isNo) &&
                    v2UTB.exists(t => t.isObjectType && ch.isInterface(t.asObjectType).isNo))
                    No
                else
                    Unknown
            }
        }
    }

    final override def isValueASubtypeOf(value: DomainValue, supertype: ReferenceType): Answer = {
        asReferenceValue(value).isValueASubtypeOf(supertype)
    }

    /**
     * Determines the nullness-property of the given value.
     *
     * @param value A value of type `ReferenceValue`.
     */
    final override def refIsNull(pc: Int, value: DomainValue): Answer = {
        asReferenceValue(value).isNull
    }

    // -----------------------------------------------------------------------------------
    //
    // ARRAY RELATED OPERATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // CREATE ARRAY
    //

    /**
     * Creates a new array.
     *
     * @note It is generally not necessary to override this method as it handles all
     *      cases in a generic manner.
     */
    override def newarray(
        pc:            Int,
        count:         DomainValue,
        componentType: FieldType
    ): Computation[DomainValue, ExceptionValue] = {
        val validCount = intIsSomeValueInRange(pc, count, 0, Int.MaxValue)
        if (validCount.isNo)
            return throws(VMNegativeArraySizeException(pc))

        val newarray = NewArray(pc, count, ArrayType(componentType))
        if (validCount.isUnknown && throwNegativeArraySizeException)
            ComputedValueOrException(newarray, VMNegativeArraySizeException(pc))
        else
            ComputedValue(newarray)
    }

    /**
     * Creates a multi-dimensional array.
     *
     * @note The componentType may be (again) an array type.
     * @note It is generally not necessary to override this method as it handles all
     *      cases in a generic manner.
     */
    override def multianewarray(
        pc:        Int,
        counts:    Operands,
        arrayType: ArrayType
    ): Computation[DomainArrayValue, ExceptionValue] = {
        var validCounts: Answer = Yes
        counts foreach { (count) =>
            val validCount = intIsSomeValueInRange(pc, count, 0, Int.MaxValue)
            if (validCount.isNo)
                return throws(VMNegativeArraySizeException(pc))
            else if (validCount.isUnknown)
                validCounts = Unknown
        }

        val newarray =
            if (counts.tail.isEmpty)
                NewArray(pc, counts.head, arrayType)
            else
                NewArray(pc, counts, arrayType)
        if (validCounts.isUnknown && throwNegativeArraySizeException)
            ComputedValueOrException(newarray, VMNegativeArraySizeException(pc))
        else
            ComputedValue(newarray)
    }

    //
    // OPERATIONS ON ARRAYS
    //

    /**
     * Loads the value stored in the array at the given index or throws an
     * exception (`NullPointerException` or `IndexOutOfBoundsException`).
     *
     * @note It is in general not necessary to override this method. If you need
     *      some special handling refine the `load` method defined by the trait
     *      `ArrayValue`.
     */
    override def arrayload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult = {
        // if the bytecode is valid, the type cast (asArrayValue) is safe
        asArrayAbstraction(arrayref).load(pc, index)
    }

    /**
     * Stores the given value in the array at the given index or throws an exception
     * (`NullPointerException`, `ArrayStoreException` or `IndexOutOfBoundsException`).
     *
     * @note It is in general not necessary to override this method. If you need
     *      some special handling refine the `store` method defined by the trait
     *      `ArrayValue`.
     */
    override def arraystore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult = {
        // if the bytecode is valid, the type cast (asArrayValue) is safe
        asArrayAbstraction(arrayref).store(pc, value, index)
    }

    /**
     * Returns the array's length or throws a `NullPointerException` if the given
     * reference is `null`.
     *
     * @note It is in general not necessary to override this method. If you need
     *      some special handling refine the trait `ArrayValue`.
     */
    override def arraylength(
        pc:       Int,
        arrayref: DomainValue
    ): Computation[DomainValue, ExceptionValue] = {
        asArrayAbstraction(arrayref).length(pc)
    }

    // -----------------------------------------------------------------------------------
    //
    // EXTRACTORS
    //
    // -----------------------------------------------------------------------------------

    object IsNull {
        def unapply(value: AReferenceValue): Some[Answer] = Some(value.isNull)
    }

    object IsPrecise {
        def unapply(value: AReferenceValue): Some[Boolean] = Some(value.isPrecise)
    }

    object UpperTypeBound {
        def unapply(value: AReferenceValue): Some[UIDSet[_ <: ReferenceType]] = {
            Some(value.upperTypeBound)
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // FACTORY METHODS
    //
    // -----------------------------------------------------------------------------------

    //
    // REFINEMENT OF EXISTING DOMAIN VALUE FACTORY METHODS
    //

    override def NullValue(pc: Int): DomainNullValue

    override def NewObject(pc: Int, objectType: ObjectType): DomainObjectValue = {
        ObjectValue(pc, objectType)
    }

    override def UninitializedThis(objectType: ObjectType): DomainObjectValue = {
        ObjectValue(-1, objectType)
    }

    override def InitializedObjectValue(pc: Int, objectType: ObjectType): DomainObjectValue = {
        ObjectValue(pc, objectType)
    }

    final override def ReferenceValue(
        pc:             Int,
        upperTypeBound: ReferenceType
    ): AReferenceValue = {
        if (upperTypeBound.isArrayType)
            ArrayValue(pc, upperTypeBound.asArrayType)
        else
            ObjectValue(pc, upperTypeBound.asObjectType)
    }

    override def NonNullObjectValue(pc: Int, objectType: ObjectType): DomainObjectValue = {
        InitializedObjectValue(pc, objectType)
    }

    override def StringValue(pc: Int, value: String): DomainObjectValue = {
        InitializedObjectValue(pc, ObjectType.String)
    }

    override def ClassValue(pc: Int, t: Type): DomainObjectValue = {
        InitializedObjectValue(pc, ObjectType.Class)
    }

    //
    // DECLARATION OF ADDITIONAL DOMAIN VALUE FACTORY METHODS
    //

    /**
     * Creates a new `DomainValue` that represents an array value with unknown
     * values and where the specified type may also just be an upper type bound
     * (unless the component type is a primitive type or an array of primitives.)
     *
     * ==Typical Usage==
     * This factory method is (typically) used to create a domain value that represents
     * an array if we know nothing specific about the array. E.g., if you want to
     * analyze a method that takes an array as a parameter.
     *
     * ==Summary==
     * The properties of the value are:
     *  - Type: '''Upper Bound''' (unless the elementType is a base type)
     *  - Null: '''Unknown'''
     *  - Size: '''Unknown'''
     *  - Content: '''Unknown'''
     *
     * @note Java's arrays are co-variant. I.e., `Object[] a = new Serializable[100];` is valid.
     */
    def ArrayValue(pc: Int, arrayType: ArrayType): DomainArrayValue

    /**
     * Factory method to create a `DomainValue` that represents ''either an class-/interface
     * value that has the given type or the value `null`''. However, the
     * information whether the value is `null` or not is not available. Furthermore, the
     * type may also just be an upper bound and it is not known if the value is
     * properly initialized.
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: '''Unknown'''
     *    (I.e., it is not guaranteed that the constructor was called; unless `NewObject` was
     *    overridden and returns DomainValues that are distinguishable!)
     *  - Type: '''Upper Bound'''
     *  - Null: '''Unknown'''
     *  - Content: '''Unknown'''
     */
    def ObjectValue(pc: Int, objectType: ObjectType): DomainObjectValue

    /**
     * Factory method to create a `DomainValue` that represents ''either an class-/interface
     * value that has the given types as an upper bound or the value `null`''. However, the
     * information whether the value is `null` or not is not available. Furthermore, the
     * type may also just be an upper bound and it is not known if the value is
     * properly initialized.
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: '''Yes'''
     *    (An Object with multiple bounds can only exist due to a merge, in this case, the objects
     *    must have been initialized beforehand or the value is not used at all and actually
     *    represents a dead variable.)
     *  - Type: '''Upper Bound'''
     *  - Null: '''Unknown'''
     *  - Content: '''Unknown'''
     */
    def ObjectValue(pc: Int, upperTypeBound: UIDSet[ObjectType]): DomainObjectValue

    abstract override def InitializedDomainValue(
        origin: ValueOrigin,
        vi:     ValueInformation
    ): DomainValue = {
        vi match {
            case _: IsNullValue =>
                NullValue(origin)

            case v: IsReferenceValue =>
                if (v.upperTypeBound.size > 1)
                    // it is definitively not an array
                    ObjectValue(origin, v.upperTypeBound.asInstanceOf[UIDSet[ObjectType]])
                else
                    // it is definitively not guaranteed to be null
                    ReferenceValue(origin, v.leastUpperType.get)

            case vi => super.InitializedDomainValue(origin, vi)
        }
    }

    /**
     * Factory method to create a new domain value that represents a newly created
     * array (non-null) with the size determined by count that is empty.
     *
     * ==Typical Usage==
     * This factory method is (implicitly) used, e.g., by OPAL when a `newarray`
     * instruction is found.
     *
     * ==Summary==
     * The properties of the value are:
     *
     *  - Type: '''Precise'''
     *  - Null: '''No'''
     *  - Size: '''Count'''
     *  - Content: ''Symbol("Empty")''' (i.e., default values w.r.t. to the array's component type)
     */
    def NewArray(pc: Int, count: DomainValue, arrayType: ArrayType): DomainArrayValue = {
        ArrayValue(pc, arrayType)
    }

    /**
     * Factory method to create a new domain value that represents a newly created
     * array (non-null) with the size determined by count that is empty.
     *
     * ==Typical Usage==
     * This factory method is (implicitly) used, e.g., by OPAL when a
     * `multianewarray` instruction is found.
     *
     * ==Summary==
     * The properties of the value are:
     *
     *  - Type: '''Precise'''
     *  - Null: '''No'''
     *  - Size: '''Depending on the values in `counts`'''
     *  - Content: ''Symbol("Empty")''' (i.e., default values w.r.t. to the array's component type)
     */
    def NewArray(pc: Int, counts: Operands, arrayType: ArrayType): DomainArrayValue = {
        ArrayValue(pc, arrayType)
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    abstract override def toJavaObject(pc: Int, value: DomainValue): Option[Object] = {
        value match {
            case _: NullValueLike => Some(null)
            case _                => super.toJavaObject(pc, value)
        }
    }

    // This domain does not support the propagation of constraints, since
    // the join operator reuses the current domain value (the same instance)
    // if its properties are correctly abstracting over the current state. Hence,
    // the same domain value is used to potentially represent different objects at
    // runtime/this domain does not support the identification of aliases.

    def refSetUpperTypeBoundOfTopOperand(
        pc:             Int,
        upperTypeBound: ReferenceType,
        operands:       Operands,
        locals:         Locals
    ): (Operands, Locals) = {
        (ReferenceValue(pc, upperTypeBound) :: operands.tail, locals)
    }

    override def refTopOperandIsNull(
        pc:       Int,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        (NullValue(pc /*Irrelevant - at least here*/ ) :: operands.tail, locals)
    }

}
