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

import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.util.Answer
import org.opalj.util.No
import org.opalj.util.Unknown
import org.opalj.util.Yes
import org.opalj.br.ArrayType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType

/**
 * Default implementation for handling reference values.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelReferenceValues
        extends DefaultDomainValueBinding
        with TypeLevelReferenceValues {
    domain: IntegerValuesDomain with TypedValuesFactory with Configuration with ClassHierarchy ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    type DomainNullValue <: NullValue with DomainReferenceValue
    type DomainObjectValue <: ObjectValue with DomainReferenceValue // <= SObject.. and MObject...
    type DomainArrayValue <: ArrayValue with DomainReferenceValue

    protected[this] class NullValue extends super.NullValue { this: DomainNullValue ⇒

        override protected def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
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

    protected[this] class ArrayValue(
        override val theUpperTypeBound: ArrayType)
            extends super.ArrayValue with SReferenceValue[ArrayType] {
        this: DomainArrayValue ⇒

        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            val isSubtypeOf = domain.isSubtypeOf(theUpperTypeBound, supertype)
            isSubtypeOf match {
                case Yes ⇒ Yes
                case No if isPrecise ||
                    supertype.isObjectType /* the array's supertypes: Object, Serializable and Cloneable are handled by domain.isSubtypeOf*/ ||
                    theUpperTypeBound.elementType.isBaseType ||
                    (supertype.isArrayType && supertype.asArrayType.elementType.isBaseType) ⇒ No
                case _ ⇒ Unknown
            }
        }

        override def isAssignable(value: DomainValue): Answer = {
            typeOfValue(value) match {

                case IsPrimitiveValue(primitiveType) ⇒
                    // The following is an overapproximation that makes it theoretically 
                    // possible to store an int value in a byte array. However, 
                    // such bytecode is illegal
                    Answer(
                        theUpperTypeBound.componentType.computationalType eq
                            primitiveType.computationalType
                    )

                case IsAReferenceValue(UIDSet1(valueType: ArrayType)) if valueType.elementType.isBaseType ⇒
                    // supports arrays of arrays of primitive values
                    if (theUpperTypeBound.componentType eq valueType)
                        Yes
                    else
                        No

                case _ /* ReferenceValue */ ⇒
                    if (theUpperTypeBound.componentType.isBaseType)
                        No
                    else
                        // IMPROVE We could check if this array's type and the given value's type are in no inheritance hierarchy
                        // IMPROVE We could check if the type of the other value is precise and if so if this type is a supertype of it
                        Unknown
            }
        }

        override protected def doLoad(
            pc: PC,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayLoadResult = {
            ComputedValueOrException(
                TypedValue(pc, theUpperTypeBound.componentType),
                potentialExceptions)
        }

        override protected def doStore(
            pc: PC,
            value: DomainValue,
            index: DomainValue,
            thrownExceptions: ExceptionValues): ArrayStoreResult =
            ComputationWithSideEffectOrException(thrownExceptions)

        // WIDENING OPERATION
        override protected def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            require(this ne other)

            val thisUpperTypeBound = this.theUpperTypeBound
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
                    classHierarchy.joinArrayTypes(this.theUpperTypeBound, thatUpperTypeBound) match {
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

        override def abstractsOver(other: DomainValue): Boolean = {
            other match {
                case NullValue() ⇒ true
                case ArrayValue(thatUpperTypeBound) ⇒
                    domain.isSubtypeOf(thatUpperTypeBound, this.theUpperTypeBound).isYes
                case _ ⇒ false
            }
        }

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue =
            target.ReferenceValue(pc, theUpperTypeBound)
    }

    /**
     * Enables matching of `DomainValue`s that are array values.
     */
    object ArrayValue {
        def unapply(value: ArrayValue): Some[ArrayType] = Some(value.theUpperTypeBound)
    }

    protected trait ObjectValue extends super.ObjectValue {
        this: DomainObjectValue ⇒

        protected def asStructuralUpdate(
            joinPC: PC,
            newUpperTypeBound: UIDSet[ObjectType]): Update[DomainValue] = {
            if (newUpperTypeBound.size == 1)
                StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound.first))
            else
                StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
        }

        override def load(pc: PC, index: DomainValue): ArrayLoadResult =
            throw DomainException("arrayload not possible; this is not an array value: "+this)

        override def store(pc: PC, value: DomainValue, index: DomainValue): ArrayStoreResult =
            throw DomainException("arraystore not possible; this is not an array value: "+this)

        override def length(pc: PC): Computation[DomainValue, ExceptionValue] =
            throw DomainException("arraylength not possible; this is not an array value: "+this)
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
                            !classHierarchy.isInterface(supertype.asObjectType) &&
                            !classHierarchy.isInterface(theUpperTypeBound) &&
                            domain.isSubtypeOf(supertype, theUpperTypeBound).isNo
                        ) ⇒ No
                case _ ⇒ Unknown
            }
        }

        // WIDENING OPERATION
        override protected def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
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
                                StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                        case newUpperTypeBound ⇒
                            StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                    }

                case MObjectValue(thatUpperTypeBound) ⇒
                    classHierarchy.joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case `thatUpperTypeBound` ⇒
                            StructuralUpdate(other)
                        case UIDSet1(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case newUpperTypeBound ⇒
                            asStructuralUpdate(joinPC, newUpperTypeBound)
                    }

                case that: ArrayValue ⇒
                    classHierarchy.joinAnyArrayTypeWithObjectType(thisUpperTypeBound) match {
                        case UIDSet1(newUpperTypeBound) ⇒
                            if (newUpperTypeBound eq `thisUpperTypeBound`)
                                NoUpdate
                            else
                                StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                        case newUpperTypeBound ⇒
                            StructuralUpdate(ObjectValue(joinPC, newUpperTypeBound))
                    }

                case NullValue() ⇒
                    NoUpdate
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other match {
                case SObjectValue(thatUpperTypeBound) ⇒
                    domain.isSubtypeOf(thatUpperTypeBound, this.theUpperTypeBound).isYes
                case NullValue() ⇒
                    true
                case ArrayValue(thatUpperTypeBound) ⇒
                    domain.isSubtypeOf(thatUpperTypeBound, this.theUpperTypeBound).isYes
                case MObjectValue(thatUpperTypeBound) ⇒
                    val lutb =
                        classHierarchy.joinObjectTypes(
                            this.theUpperTypeBound, thatUpperTypeBound, true)
                    lutb.containsOneElement && (lutb.first() eq this.theUpperTypeBound)
            }
        }

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue =
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

        override def referenceValues: Iterable[IsAReferenceValue] = Iterable(this)

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
                    upperTypeBound != ObjectType.SerializableAndCloneable
                ) ⇒
                    // even if the upper bound is not precise we are now 100% sure 
                    // that this value is not a subtype of the given supertype
                    No
                case _ ⇒
                    Unknown
            }
        }

        override protected def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val thisUpperTypeBound = this.upperTypeBound
            other match {
                case SObjectValue(thatUpperTypeBound) ⇒
                    classHierarchy.joinObjectTypes(thatUpperTypeBound, thisUpperTypeBound, true) match {
                        case `thisUpperTypeBound` ⇒
                            NoUpdate
                        case UIDSet1(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case newUpperTypeBound ⇒
                            asStructuralUpdate(joinPC, newUpperTypeBound)
                    }

                case MObjectValue(thatUpperTypeBound) ⇒
                    classHierarchy.joinUpperTypeBounds(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case `thisUpperTypeBound` ⇒
                            NoUpdate
                        case `thatUpperTypeBound` ⇒
                            StructuralUpdate(other)
                        case newUpperTypeBound ⇒
                            asStructuralUpdate(joinPC, newUpperTypeBound)
                    }

                case ArrayValue(thatUpperTypeBound) ⇒
                    classHierarchy.joinAnyArrayTypeWithMultipleTypesBound(thisUpperTypeBound) match {
                        case `thisUpperTypeBound` ⇒
                            NoUpdate
                        case newUpperTypeBound ⇒
                            asStructuralUpdate(joinPC, newUpperTypeBound)
                    }

                case NullValue() ⇒
                    NoUpdate
            }
        }

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue =
            target match {
                case td: TypeLevelReferenceValues ⇒
                    td.ObjectValue(pc, upperTypeBound).asInstanceOf[target.DomainValue]
                case _ ⇒
                    super.adapt(target, pc)
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
