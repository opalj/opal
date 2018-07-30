/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.br.ArrayType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.value.IsPrimitiveValue
import org.opalj.value.TypeOfReferenceValue

/**
 * Default implementation for handling reference values.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelReferenceValues
    extends DefaultDomainValueBinding
    with TypeLevelReferenceValues {
    domain: IntegerValuesDomain with TypedValuesFactory with Configuration with TheClassHierarchy ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    type DomainNullValue <: NullValue with AReferenceValue
    type DomainObjectValue <: ObjectValue with AReferenceValue // <= SObject.. and MObject...
    type DomainArrayValue <: ArrayValue with AReferenceValue

    protected[this] class NullValue extends super.NullValue { this: DomainNullValue ⇒

        override protected def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case _: NullValue ⇒ NoUpdate
                case _: ReferenceValue ⇒
                    // THIS domain does not track whether ReferenceValues
                    // are definitively not null!
                    StructuralUpdate(other)
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other.isInstanceOf[NullValue]
        }
    }

    /**
     * @param theUpperTypeBound The upper type bound of this array, which is necessarily
     *      precise if the element type of the array is a base type (primitive type).
     */
    protected[this] class ArrayValue(
            override val theUpperTypeBound: ArrayType
    ) extends super.ArrayValue with SReferenceValue[ArrayType] {
        this: DomainArrayValue ⇒

        override def baseValues: Traversable[DomainArrayValue] = Traversable.empty

        override def isValueASubtypeOf(supertype: ReferenceType): Answer = {
            domain.isASubtypeOf(theUpperTypeBound, supertype) match {
                case Yes ⇒ Yes
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
                    ) ⇒ No
                case _ ⇒ Unknown
            }
        }

        override def isAssignable(value: DomainValue): Answer = {

            // TODO Get rid of "typeOfValue" call; the value is now always typed!
            (typeOfValue(value): @unchecked) match {

                case IsPrimitiveValue(primitiveType) ⇒
                    // The following is an over approximation that makes it theoretically
                    // possible to store an int value in a byte array. However,
                    // such bytecode is illegal
                    Answer(
                        theUpperTypeBound.componentType.computationalType eq
                            primitiveType.computationalType
                    )

                case elementValue @ TypeOfReferenceValue(EmptyUpperTypeBound) ⇒
                    // the elementValue is "null"
                    assert(elementValue.isNull.isYes)
                    // e.g., it is possible to store null in the n-1 dimensions of
                    // a n-dimensional array of primitive values
                    if (theUpperTypeBound.componentType.isReferenceType)
                        Yes
                    else
                        No

                case elementValue @ TypeOfReferenceValue(UIDSet1(elementValueType)) ⇒
                    classHierarchy.canBeStoredIn(
                        elementValueType,
                        elementValue.isPrecise,
                        this.theUpperTypeBound,
                        this.isPrecise
                    )

                case elementValue @ TypeOfReferenceValue(otherUpperTypeBound) ⇒
                    val elementValueIsPrecise = elementValue.isPrecise
                    val thisArrayType = this.theUpperTypeBound
                    val thisIsPrecise = this.isPrecise
                    var finalAnswer: Answer = No
                    otherUpperTypeBound.exists { elementValueType ⇒
                        classHierarchy.canBeStoredIn(
                            elementValueType,
                            elementValueIsPrecise,
                            thisArrayType,
                            thisIsPrecise
                        ) match {
                            case Yes ⇒
                                return Yes;

                            case intermediateAnswer ⇒
                                finalAnswer = finalAnswer join intermediateAnswer
                                false
                        }
                    }
                    finalAnswer
            }
        }

        override protected def doLoad(
            pc:                  Int,
            index:               DomainValue,
            potentialExceptions: ExceptionValues
        ): ArrayLoadResult = {
            val value = TypedValue(pc, theUpperTypeBound.componentType)
            ComputedValueOrException(value, potentialExceptions)
        }

        override protected def doStore(
            pc:               Int,
            value:            DomainValue,
            index:            DomainValue,
            thrownExceptions: ExceptionValues
        ): ArrayStoreResult = {
            ComputationWithSideEffectOrException(thrownExceptions)
        }

        // WIDENING OPERATION
        override protected def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val thisUTB = this.theUpperTypeBound
            other match {

                case SObjectValue(thatUpperTypeBound) ⇒
                    classHierarchy.joinAnyArrayTypeWithObjectType(thatUpperTypeBound) match {
                        case UIDSet1(newUpperTypeBound) ⇒
                            if (newUpperTypeBound eq `thatUpperTypeBound`)
                                StructuralUpdate(other)
                            else
                                StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                        case newUpperTypeBound ⇒
                            StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                    }

                case MObjectValue(thatUpperTypeBound) ⇒
                    classHierarchy.joinAnyArrayTypeWithMultipleTypesBound(thatUpperTypeBound) match {
                        case `thatUpperTypeBound` ⇒
                            StructuralUpdate(other)
                        case UIDSet1(newUpperTypeBound) ⇒
                            StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                        case newUpperTypeBound ⇒
                            // this case should not occur...
                            StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                    }

                case ArrayValue(thatUpperTypeBound) ⇒
                    classHierarchy.joinArrayTypes(thisUTB, thatUpperTypeBound) match {
                        case Left(`thisUTB`) ⇒
                            NoUpdate
                        case Left(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case Left(newUpperTypeBound) ⇒
                            StructuralUpdate(ArrayValue(joinPC, newUpperTypeBound))
                        case Right(newUpperTypeBound) ⇒
                            StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                    }

                case _: NullValue ⇒ NoUpdate
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other match {
                case _: NullValue ⇒ true
                case ArrayValue(thatUpperTypeBound) ⇒
                    domain.isSubtypeOf(thatUpperTypeBound, this.theUpperTypeBound)
                case _ ⇒ false
            }
        }

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue = {
            target.ReferenceValue(origin, theUpperTypeBound)
        }
    }

    /**
     * Enables matching of `DomainValue`s that are array values.
     */
    object ArrayValue {
        def unapply(value: ArrayValue): Some[ArrayType] = Some(value.theUpperTypeBound)
    }

    protected trait ObjectValue extends super.ObjectValue { this: DomainObjectValue ⇒

        override def baseValues: Traversable[DomainObjectValue] = Traversable.empty

        protected def asStructuralUpdate(
            pc:                Int,
            newUpperTypeBound: UIDSet[ObjectType]
        ): Update[DomainValue] = {
            if (newUpperTypeBound.isSingletonSet)
                StructuralUpdate(ObjectValue(pc, newUpperTypeBound.head))
            else
                StructuralUpdate(ObjectValue(pc, newUpperTypeBound))
        }

        final override def length(pc: Int): Computation[DomainValue, ExceptionValue] = {
            throw DomainException("arraylength not possible; this is not an array value: "+this)
        }

        final override def load(pc: Int, index: DomainValue): ArrayLoadResult = {
            throw DomainException("arrayload not possible; this is not an array value: "+this)
        }

        final override def store(
            pc:    Int,
            value: DomainValue,
            index: DomainValue
        ): ArrayStoreResult = {
            throw DomainException("arraystore not possible; this is not an array value: "+this)
        }

    }

    protected class SObjectValue(
            override val theUpperTypeBound: ObjectType
    ) extends ObjectValue with SReferenceValue[ObjectType] { this: DomainObjectValue ⇒

        /**
         * @inheritdoc
         *
         * @note It is often not necessary to override this method as this method already
         *      takes the property whether the upper type bound '''is precise''' into
         *      account.
         */
        override def isValueASubtypeOf(supertype: ReferenceType): Answer = {
            domain.isASubtypeOf(theUpperTypeBound, supertype) match {
                case Yes ⇒
                    Yes
                case No if isPrecise
                    || (
                        supertype.isArrayType &&
                        // and it is impossible that this value is actually an array...
                        (theUpperTypeBound ne ObjectType.Object) &&
                        (theUpperTypeBound ne ObjectType.Serializable) &&
                        (theUpperTypeBound ne ObjectType.Cloneable)
                    ) || (
                            // If both types represent class types and it is not
                            // possible that some value of this type may be a subtype
                            // of the given supertype, the answer "No" is correct.
                            supertype.isObjectType &&
                            classHierarchy.isKnown(supertype.asObjectType) &&
                            classHierarchy.isKnown(theUpperTypeBound) &&
                            classHierarchy.isInterface(supertype.asObjectType).isNo &&
                            classHierarchy.isInterface(theUpperTypeBound).isNo &&
                            domain.isASubtypeOf(supertype, theUpperTypeBound).isNo
                        ) ⇒
                    No
                case _ if isPrecise &&
                    // Note "reflexivity" is already captured by the first isSubtypeOf call
                    domain.isSubtypeOf(supertype, theUpperTypeBound) ⇒
                    No
                case _ ⇒
                    Unknown
            }
        }

        // WIDENING OPERATION
        override protected def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            val thisUpperTypeBound = this.theUpperTypeBound
            other match {

                case SObjectValue(thatUpperTypeBound) ⇒
                    classHierarchy.joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case UIDSet1(newUpperTypeBound) ⇒
                            if (newUpperTypeBound eq `thisUpperTypeBound`)
                                NoUpdate
                            else if (newUpperTypeBound eq `thatUpperTypeBound`)
                                StructuralUpdate(other)
                            else
                                StructuralUpdate(ObjectValue(pc, newUpperTypeBound))
                        case newUpperTypeBound ⇒
                            StructuralUpdate(ObjectValue(pc, newUpperTypeBound))
                    }

                case MObjectValue(thatUpperTypeBound) ⇒
                    classHierarchy.joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case `thatUpperTypeBound` ⇒
                            StructuralUpdate(other)
                        case UIDSet1(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case newUpperTypeBound ⇒
                            asStructuralUpdate(pc, newUpperTypeBound)
                    }

                case _: ArrayValue ⇒
                    classHierarchy.joinAnyArrayTypeWithObjectType(thisUpperTypeBound) match {
                        case UIDSet1(newUpperTypeBound) ⇒
                            if (newUpperTypeBound eq `thisUpperTypeBound`)
                                NoUpdate
                            else
                                StructuralUpdate(ObjectValue(pc, newUpperTypeBound))
                        case newUpperTypeBound ⇒
                            StructuralUpdate(ObjectValue(pc, newUpperTypeBound))
                    }

                case _: NullValue ⇒ NoUpdate
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other match {
                case SObjectValue(thatUpperTypeBound) ⇒
                    domain.isSubtypeOf(thatUpperTypeBound, this.theUpperTypeBound)

                case ArrayValue(thatUpperTypeBound) ⇒
                    domain.isSubtypeOf(thatUpperTypeBound, this.theUpperTypeBound)

                case MObjectValue(thatUpperTypeBound) ⇒
                    classHierarchy.isSubtypeOf(
                        thatUpperTypeBound.asInstanceOf[UIDSet[ReferenceType]],
                        this.theUpperTypeBound
                    )

                case _: NullValue ⇒ true
            }
        }

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue = {
            target.ReferenceValue(origin, theUpperTypeBound)
        }

    }

    object SObjectValue {
        def unapply(that: SObjectValue): Some[ObjectType] = Some(that.theUpperTypeBound)
    }

    /**
     * @param upperTypeBound All types from which the (precise, but unknown) type of the
     *      represented value inherits. I.e., the value represented by this domain value
     *      is known to have a type that (in)directly inherits from all given types at
     *      the same time.
     */
    protected class MObjectValue(
            override val upperTypeBound: UIDSet[ObjectType]
    ) extends ObjectValue { value: DomainObjectValue ⇒

        assert(upperTypeBound.size > 1)

        /**
         * Determines if this value is a subtype of the given supertype by
         * delegating to the `isSubtypeOf(ReferenceType,ReferenceType)` method of the
         * domain.
         *
         * @note This is a very basic implementation that cannot determine that this
         *      value is '''not''' a subtype of the given type as this implementation
         *      does not distinguish between class types and interface types.
         */
        override def isValueASubtypeOf(supertype: ReferenceType): Answer = {
            var isASubtypeOf: Answer = No
            upperTypeBound foreach { anUpperTypeBound ⇒
                domain.isASubtypeOf(anUpperTypeBound, supertype) match {
                    case Yes     ⇒ return Yes; // <= Shortcut evaluation
                    case Unknown ⇒ isASubtypeOf = Unknown
                    case No      ⇒ /*nothing to do*/
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
                ) ⇒
                    // even if the upper bound is not precise we are now 100% sure
                    // that this value is not a subtype of the given supertype
                    No
                case _ ⇒
                    Unknown
            }
        }

        override protected def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            val thisUTB = this.upperTypeBound
            other match {

                case SObjectValue(thatUTB) ⇒
                    classHierarchy.joinObjectTypes(thatUTB, thisUTB, true) match {
                        case `thisUTB`          ⇒ NoUpdate
                        case UIDSet1(`thatUTB`) ⇒ StructuralUpdate(other)
                        case newUTB             ⇒ asStructuralUpdate(pc, newUTB)
                    }

                case MObjectValue(thatUTB) ⇒
                    classHierarchy.joinUpperTypeBounds(thisUTB, thatUTB, true) match {
                        case `thisUTB` ⇒ NoUpdate
                        case `thatUTB` ⇒ StructuralUpdate(other)
                        case newUTB    ⇒ asStructuralUpdate(pc, newUTB)
                    }

                case _: ArrayValue ⇒
                    classHierarchy.joinAnyArrayTypeWithMultipleTypesBound(thisUTB) match {
                        case `thisUTB` ⇒ NoUpdate
                        case newUTB    ⇒ asStructuralUpdate(pc, newUTB)
                    }

                case _: NullValue ⇒ NoUpdate
            }
        }

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue =
            target match {
                case td: TypeLevelReferenceValues ⇒
                    td.ObjectValue(origin, upperTypeBound).asInstanceOf[target.DomainValue]
                case _ ⇒
                    super.adapt(target, origin)
            }

        override def summarize(origin: ValueOrigin): this.type = this

        override def toString: String = {
            upperTypeBound.map(_.toJava).mkString("ReferenceValue(", " with ", ")")
        }
    }

    object MObjectValue {
        def unapply(that: MObjectValue): Option[UIDSet[ObjectType]] = Some(that.upperTypeBound)
    }
}
