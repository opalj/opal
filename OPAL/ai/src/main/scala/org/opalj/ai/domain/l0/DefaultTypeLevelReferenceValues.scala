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
package domain
package l0

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.collection.immutable.UIDSet

import br._

/**
 * Default implementation for handling reference values.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelReferenceValues
        extends DefaultDomainValueBinding
        with TypeLevelReferenceValues {
    domain: Configuration with IntegerValuesComparison with ClassHierarchy ⇒

    // ---------------------------------1-------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    type DomainNullValue <: NullValue with DomainReferenceValue
    type DomainObjectValue <: ObjectValue with DomainReferenceValue // <= SObject.. and MObject...
    type DomainArrayValue <: ArrayValue with DomainReferenceValue

    protected class NullValue
            extends super.NullValue {
        this: DomainNullValue ⇒

        override protected def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case that: NullValue ⇒ NoUpdate
                case that: ReferenceValue ⇒
                    assume(that.isNull.isYesOrUnknown)
                    StructuralUpdate(other)
            }
        }
    }

    protected class ArrayValue(
        override val theUpperTypeBound: ArrayType)
            extends super.ArrayValue
            with SReferenceValue[ArrayType] {
        this: DomainArrayValue ⇒

        override def refineIsNull(pc: PC, isNull: Answer): DomainReferenceValue = {
            if (isNull.isYes)
                NullValue(pc)
            else
                this
        }

        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            val isSubtypeOf = domain.isSubtypeOf(theUpperTypeBound, supertype)
            isSubtypeOf match {
                case Yes ⇒ Yes
                case No if isPrecise ||
                    theUpperTypeBound.elementType.isBaseType ||
                    (supertype.isArrayType && supertype.asArrayType.elementType.isBaseType) ⇒ No
                case _ ⇒ Unknown
            }
        }

        override def isAssignable(value: DomainValue): Answer = {
            typeOfValue(value) match {
                case IsPrimitiveValue(primitiveType) ⇒
                    // The following is an overapproximation that makes it theoretically 
                    // possible to store an int value in a byte array.
                    Answer(
                        theUpperTypeBound.componentType.computationalType eq primitiveType.computationalType
                    )
                case _ ⇒
                    // IMPROVE We could check if this array's type and the given value's type are in no inheritance hierarchy
                    // IMPROVE We could check if the type of the other value is precise and if so if this type is a supertype of it
                    Unknown
            }
        }

        override protected def doLoad(
            pc: PC,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayLoadResult = {
            ComputedValueAndException(
                TypedValue(pc, theUpperTypeBound.componentType),
                potentialExceptions)
        }

        override protected def doStore(
            pc: PC,
            value: DomainValue,
            index: DomainValue,
            thrownExceptions: ExceptionValues): ArrayStoreResult =
            ComputationWithSideEffectOrException(thrownExceptions)

        // NARROWING OPERATION
        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): DomainReferenceValue = {
            // OPAL-AI calls this method only if a previous "subtype of" test 
            // (this.isValueSubtypeOf <: supertype ?) 
            // returned unknown and we are now on the branch where this relation
            // has to hold. Hence, we only need to handle the case where 
            // supertype is more strict than this type's upper type bound.
            if (supertype.isArrayType)
                ArrayValue(pc, supertype.asArrayType)
            else
                throw ImpossibleRefinement(this, "cast to a non-array value: "+supertype)
        }

        // WIDENING OPERATION
        override protected def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val thisUpperTypeBound = this.theUpperTypeBound
            other match {
                case SObjectValue(thatUpperTypeBound) ⇒
                    joinAnyArrayTypeWithObjectType(thatUpperTypeBound) match {
                        case Left(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case Left(newUpperTypeBound) ⇒
                            StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                        case Right(newUpperTypeBound) ⇒
                            StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                    }

                case MObjectValue(thatUpperTypeBound) ⇒
                    joinAnyArrayTypeWithMultipleTypesBound(thatUpperTypeBound) match {
                        case Right(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case Left(newUpperTypeBound) ⇒
                            StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                        case Right(newUpperTypeBound) ⇒
                            // this case should not occur...
                            StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                    }

                case ArrayValue(thatUpperTypeBound) ⇒
                    joinArrayTypes(this.theUpperTypeBound, thatUpperTypeBound) match {
                        case Left(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case Left(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case Left(newUpperTypeBound) ⇒
                            StructuralUpdate(ArrayValue(joinPC, newUpperTypeBound))
                        case Right(newUpperTypeBound) ⇒
                            StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                    }

                case NullValue() ⇒
                    NoUpdate
            }
        }
    }

    /**
     * Enables matching of `DomainValue`s that are array values.
     */
    object ArrayValue {
        def unapply(value: ArrayValue): Some[ArrayType] = Some(value.theUpperTypeBound)
    }

    protected trait ObjectValue extends super.ObjectValue {
        this: DomainObjectValue ⇒

        override def refineIsNull(pc: PC, isNull: Answer): DomainReferenceValue = {
            if (isNull.isYes)
                NullValue(pc)
            else
                this
        }

        protected def asStructuralUpdate(
            joinPC: PC,
            newUpperTypeBound: Either[ObjectType, UIDSet[ObjectType]]): Update[DomainValue] = {
            newUpperTypeBound match {
                case Left(newUpperTypeBound) ⇒
                    StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                case Right(newUpperTypeBound) ⇒
                    StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
            }
        }

        override def load(pc: PC, index: DomainValue): ArrayLoadResult =
            throw DomainException("this is not an array value: "+this)

        override def store(pc: PC, value: DomainValue, index: DomainValue): ArrayStoreResult =
            throw DomainException("this is not an array value: "+this)

        override def length(pc: PC): Computation[DomainValue, ExceptionValue] =
            throw DomainException("this not an array value: "+this)
    }

    protected class SObjectValue(
        override val theUpperTypeBound: ObjectType)
            extends ObjectValue
            with SReferenceValue[ObjectType] {
        this: DomainObjectValue ⇒

        /**
         * @inheritdoc
         *
         * @note It is often not necessary to override this method as this method already
         *      takes the property whether the upper type bound '''is precise''' into
         *      account.
         */
        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            val isSubtypeOf = domain.isSubtypeOf(theUpperTypeBound, supertype)
            isSubtypeOf match {
                case Yes ⇒ Yes
                case No if isPrecise ||
                    (
                        supertype.isArrayType &&
                        // and it is impossible that this value is actually an array...
                        (theUpperTypeBound ne ObjectType.Object) &&
                        (theUpperTypeBound ne ObjectType.Serializable) &&
                        (theUpperTypeBound ne ObjectType.Cloneable)
                    ) ⇒ No
                case _ ⇒ Unknown
            }
        }

        // NARROWING OPERATION
        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): DomainReferenceValue = {
            if (supertype eq theUpperTypeBound)
                return this

            // OPAL-AI calls this method only if a previous "subtype of" test 
            // (this.typeOfvalue <: supertype ?) 
            // returned unknown and we are now on the branch where this relation
            // has to hold. Hence, we only need to handle the case where 
            // supertype is more strict than this type's upper type bound.
            val isSubtypeOf = domain.isSubtypeOf(supertype, theUpperTypeBound)
            if (isSubtypeOf.isYes)
                if (supertype.isArrayType)
                    ArrayValue(pc, supertype.asArrayType)
                else
                    ObjectValue(pc, supertype.asObjectType)
            else {
                if (supertype.isArrayType)
                    throw ImpossibleRefinement(this, "cast to array type: "+supertype.toJava)

                // probably some (abstract) class and an interface or two
                // unrelated interfaces
                ObjectValue(pc, UIDSet(theUpperTypeBound, supertype.asObjectType))
            }
        }

        // WIDENING OPERATION
        override protected def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val thisUpperTypeBound = this.theUpperTypeBound
            other match {
                case SObjectValue(thatUpperTypeBound) ⇒
                    joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case Left(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case Left(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case newUpperTypeBound ⇒
                            asStructuralUpdate(joinPC, newUpperTypeBound)
                    }

                case MObjectValue(thatUpperTypeBound) ⇒
                    joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case Left(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case Right(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case newUpperTypeBound ⇒
                            asStructuralUpdate(joinPC, newUpperTypeBound)
                    }

                case that: ArrayValue ⇒
                    joinAnyArrayTypeWithObjectType(thisUpperTypeBound) match {
                        case Left(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case Left(newUpperTypeBound) ⇒
                            StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                        case Right(newUpperTypeBound) ⇒
                            StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                    }

                case NullValue() ⇒
                    NoUpdate
            }
        }

        override def adapt(target: Domain, pc: PC): target.DomainValue =
            target.ReferenceValue(pc, theUpperTypeBound)

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
        override val upperTypeBound: UIDSet[ObjectType])
            extends ObjectValue {
        value: DomainObjectValue ⇒

        override def referenceValues: Iterator[IsAReferenceValue] = Iterator(this)

        /**
         * Determines if this value is a subtype of the given supertype by
         * delegating to the `isSubtypeOf(ReferenceType,ReferenceType)` method of the
         * domain.
         *
         * @note This is a very basic implementation that cannot determine that this
         *      value is '''not''' a subtype of the given type as this implementation
         *      does not distinguish between class types and interface types.
         */
        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            var isSubtypeOf: Answer = No
            upperTypeBound foreach { anUpperTypeBound ⇒
                domain.isSubtypeOf(anUpperTypeBound, supertype) match {
                    case Yes     ⇒ return Yes // <= Shortcut evaluation
                    case Unknown ⇒ isSubtypeOf = Unknown
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

            isSubtypeOf match {
                // Yes is not possible here!

                case No if (
                    supertype.isArrayType &&
                    upperTypeBound != TypeLevelReferenceValues.SerializableAndCloneable
                ) ⇒
                    // even if the upper bound is not precise we are now a 100% sure 
                    // this value is not a subtype of the given supertype
                    No
                case _ ⇒
                    Unknown
            }
        }

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): DomainReferenceValue = {
            // OPAL-AI calls this method only if a previous "subtype of" test 
            // (typeOf(this.value) <: additionalUpperBound ?) 
            // returned unknown. Hence, we only handle the case where the new bound
            // is more strict than the previous bound.

            var newUpperTypeBound: UIDSet[ObjectType] = UIDSet.empty[ObjectType]
            upperTypeBound foreach { (anUpperTypeBound: ObjectType) ⇒
                // ATTENTION: "!..yes" is not the same as "no" (there is also unknown)
                if (!domain.isSubtypeOf(supertype, anUpperTypeBound).isYes)
                    newUpperTypeBound = newUpperTypeBound + anUpperTypeBound
            }
            if (newUpperTypeBound.size == 0)
                ReferenceValue(pc, supertype)
            else if (supertype.isObjectType)
                ObjectValue(pc, newUpperTypeBound + supertype.asObjectType)
            else
                throw ImpossibleRefinement(this, "incompatible bounds: "+supertype.toJava)
        }

        override protected def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val thisUpperTypeBound = this.upperTypeBound
            other match {
                case SObjectValue(thatUpperTypeBound) ⇒
                    joinObjectTypes(thatUpperTypeBound, thisUpperTypeBound, true) match {
                        case Right(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case Left(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case newUpperTypeBound ⇒
                            asStructuralUpdate(joinPC, newUpperTypeBound)
                    }

                case MObjectValue(thatUpperTypeBound) ⇒
                    joinUpperTypeBounds(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case Right(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case Right(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case newUpperTypeBound ⇒
                            asStructuralUpdate(joinPC, newUpperTypeBound)
                    }

                case ArrayValue(thatUpperTypeBound) ⇒
                    joinAnyArrayTypeWithMultipleTypesBound(thisUpperTypeBound) match {
                        case Right(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case newUpperTypeBound ⇒
                            asStructuralUpdate(joinPC, newUpperTypeBound)
                    }

                case NullValue() ⇒
                    NoUpdate
            }
        }

        override def adapt(target: Domain, pc: PC): target.DomainValue =
            target match {
                case td: DefaultTypeLevelReferenceValues ⇒
                    td.ObjectValue(pc, this.upperTypeBound).
                        asInstanceOf[target.DomainValue]
                case _ ⇒ super.adapt(target, pc)
            }

        override def summarize(pc: PC): DomainValue = this

        override def toString() =
            "ReferenceValue("+upperTypeBound.map(_.toJava).mkString(" with ")+")"
    }

    object MObjectValue {
        def unapply(that: MObjectValue): Option[UIDSet[ObjectType]] =
            Some(that.upperTypeBound)
    }
}
