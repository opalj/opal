///* License (BSD Style License):
// * Copyright (c) 2009 - 2013
// * Software Technology Group
// * Department of Computer Science
// * Technische Universität Darmstadt
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *  - Redistributions of source code must retain the above copyright notice,
// *    this list of conditions and the following disclaimer.
// *  - Redistributions in binary form must reproduce the above copyright notice,
// *    this list of conditions and the following disclaimer in the documentation
// *    and/or other materials provided with the distribution.
// *  - Neither the name of the Software Technology Group or Technische
// *    Universität Darmstadt nor the names of its contributors may be used to
// *    endorse or promote products derived from this software without specific
// *    prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
// * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// * POSSIBILITY OF SUCH DAMAGE.
// */
//package de.tud.cs.st
//package bat
//package resolved
//package ai
//package domain
//package l1
//
//import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }
//
///**
// * @author Michael Eichberg
// */
//trait TaggableReferenceValues[+I]
//        extends DefaultValueBinding[I]
//        with TypeLevelReferenceValues[I] { domain ⇒
//
//    // -----------------------------------------------------------------------------------
//    //
//    // REPRESENTATION OF REFERENCE VALUES
//    //
//    // -----------------------------------------------------------------------------------
//
//    trait ReferenceValue
//            extends super.ReferenceValue
//            with IsReferenceType { this: DomainValue ⇒
//
//        /**
//         * A type bound represents the available information about a reference
//         * value's type.
//         *
//         * In case of reference types, a type bound may, e.g., be a set of interface types
//         * which are known to be implemented by the current object. Even if the type
//         * contains a class type it may just be a super class of the concrete type and,
//         * hence, just represent an abstraction.
//         *
//         * How type bounds related to reference types are handled and whether the
//         * domain makes it possible to distinguish between precise types and
//         * type bounds is at the sole discretion of the domain.
//         */
//        type TypeBound = Set[ReferenceType]
//
//        /**
//         * Returns the set of types of the represented value.
//         *
//         * Basically, we have to distinguish two situations:
//         * 1. a value that may have (depending on the control flow) different
//         *    independent types (different values with corresponding types).
//         * 2. a type for which we have multiple bounds; i.e., we don't know the precise
//         *    type, but we know (e.g., due to typechecks) that it (has to) implements
//         *    multiple interfaces.
//         */
//        def valueType: TypeBound
//
//        /**
//         * Returns if `true` if the type information about this value is complete and
//         * precise. I.e., if `isPrecise` returns `true` and the value's type is
//         * reported to be `java.lang.Object` then the current value is known to be an
//         * instance of the class `java.lang.Object` and of no other (sub)class.
//         */
//        def isPrecise: Boolean
//    }
//
//    object ReferenceValue {
//        def unapply(rv: ReferenceValue): Some[(TypeBound, Answer, Boolean)] =
//            Some((rv.valueType, rv.isNull, rv.isPrecise))
//    }
//
//    //
//    // REPRESENTATIONS OF CONCRETE REFERENCE VALUES
//    //    
//
//
//    class AReferenceValue protected[TaggableReferenceValues] (
//        val pc: Int,
//        val valueType: TypeBound,
//        val properties: Int)
//            extends ReferenceValue { this: DomainValue ⇒
//
//        // START - GENERAL PROPERTIES HANDLING
//        final val AnswerMask = 3
//        final val BooleanMask = 1
//        final val YesP = 1 // now if we merge YesP & NoP we get UnknownP
//        final val NoP = 2
//        final val UnknownP = 0
//        final val TrueP = 1
//        final val FalseP = 0
//        private[this] var lastPropertyId = -1
//        def nextBinaryPropertyId(): Int = { lastPropertyId += 1; lastPropertyId }
//        def nextTernaryPropertyId(): Int = { lastPropertyId += 2; lastPropertyId }
//        def asAnswer(propertyId: Int): Answer =
//            properties >>> propertyId & AnswerMask match {
//                case YesP     ⇒ Yes
//                case NoP      ⇒ No
//                case UnknownP ⇒ Unknown
//            }
//        def asBoolean(propertyId: Int): Boolean =
//            (properties >>> propertyId & BooleanMask) == 1
//        // END - GENERAL PROPERTIES HANDLING
//            
//        final val isNullPId = nextTernaryPropertyId();
//        final val isPrecisePId = nextBinaryPropertyId();
//
//        def isNull: Answer = asAnswer(isNullPId)
//        def isPrecise: Boolean = asBoolean(isPrecisePId)
//
//        override def adapt[ThatI >: I](
//            targetDomain: Domain[ThatI],
//            pc: Int): targetDomain.DomainValue =
//            targetDomain match {
//                case thatDomain: TaggableReferenceValues[ThatI] ⇒
//                    adaptAReferenceValue(thatDomain, pc).asInstanceOf[targetDomain.DomainValue]
//                case _ ⇒ super.adapt(targetDomain, pc)
//            }
//
//        def adaptAReferenceValue[ThatI >: I](
//            targetDomain: TaggableReferenceValues[ThatI],
//            pc: Int): targetDomain.AReferenceValue =
//            new targetDomain.AReferenceValue(pc, this.valueType, this.properties)
//
//        override def nonEmpty = valueType.nonEmpty
//
//        override def size = valueType.size
//
//        def foreach[U](f: TypeBound ⇒ U): Unit = f(valueType)
//
//        def headType = valueType.head
//
//        def foreachType[U](f: ReferenceType ⇒ U): Unit = valueType.foreach(f)
//
//        def forallTypes(f: ReferenceType ⇒ Boolean): Boolean = valueType.forall(f)
//
//        /**
//         * Determines if this reference value is a subtype of the given supertype by
//         * delegating to the `isSubtypeOf(ReferenceType,ReferenceType)` method of the
//         * domain.
//         *
//         * Additionally, the `isPrecise` property is taken into consideration to ensure
//         * that a `No` answer means that it is impossible that any runtime value is
//         * actually a subtype of the given supertype.
//         */
//        def isSubtypeOf(supertype: ReferenceType, onNull: ⇒ Answer): Answer = {
//            if (isNull.yes)
//                return onNull
//
//            var answer = domain.isSubtypeOf(valueType.head, supertype)
//            for (
//                referenceType ← valueType.tail;
//                if answer.isDefined // when the answer is Unknown we do not need to continue
//            ) {
//                answer = answer.merge(domain.isSubtypeOf(referenceType, supertype))
//            }
//            answer match {
//                case Yes             ⇒ Yes
//                case No if isPrecise ⇒ No
//                case No ⇒
//                    // .. is it conceivable that this value is still a subtype of the
//                    // given reference type?
//                    if (forallTypes { subtype ⇒ domain.isSubtypeOf(supertype, subtype).maybeYes })
//                        // Well it is conceivable that the value at runtime is a subtype
//                        Unknown
//                    else
//                        No
//                case Unknown ⇒ Unknown
//            }
//        }
//
//        def updateIsNull(pc: Int, isNull: Answer): AReferenceValue = {
//            if (this.isNull.isUndefined)
//                if (isNull.yes)
//                    AReferenceValue(this.pc, Set.empty[ReferenceType], Yes, true)
//                else
//                    AReferenceValue(this.pc, valueType, isNull, isPrecise)
//            else
//                // this update of the value's isNull property doesn't make sense
//                // hence, we swallow it to facilitate the implementation of 
//                // MultipleReferenceValues
//                this
//        }
//
//        def addUpperBound(pc: Int, theUpperBound: ReferenceType): AReferenceValue = {
//            val isSubtypeOfAnswer = isSubtypeOf(theUpperBound, Yes)
//            isSubtypeOfAnswer match {
//                case Yes ⇒ this
//                case No if isPrecise ⇒
//                    // Actually, it does not make sense to establish a new bound for a 
//                    // precise object type. However, we have to handle this case 
//                    // gracefully since it is possible that the value represented by
//                    // this "AReferenceValue" is just one of many instances represented 
//                    // by a domain value on the stack/in a register 
//                    // and in this case it may make sense to establish a more stringent
//                    // bound for the others.
//                    this
//                case No if forallTypes(domain.isSubtypeOf(theUpperBound, _).yes) ⇒
//                    AReferenceValue(this.pc, Set(theUpperBound), isNull, isPrecise)
//                case _ /* (No && !isPrecise || Unknown) */ ⇒
//                    var newValueTypes = Set.empty[ReferenceType]
//                    var addTheUpperBound = true
//                    valueType.foreach { anUpperBound ⇒
//                        if (theUpperBound == anUpperBound) {
//                            /* do nothing */
//                        } else if (domain.isSubtypeOf(theUpperBound, anUpperBound).yes) {
//                            /* do nothing (we may add theUpperBound later) */
//                        } else if (domain.isSubtypeOf(anUpperBound, theUpperBound).yes) {
//                            addTheUpperBound = false
//                            newValueTypes = newValueTypes + anUpperBound
//                        } else {
//                            newValueTypes = newValueTypes + anUpperBound
//                        }
//                    }
//                    if (addTheUpperBound)
//                        newValueTypes = newValueTypes + theUpperBound
//
//                    AReferenceValue(this.pc, newValueTypes, isNull, isPrecise)
//            }
//        }
//
//        override def merge(mergePC: Int, value: DomainValue): Update[DomainValue] = {
//            if (value eq this)
//                return NoUpdate
//
//            value match {
//                case v @ AReferenceValue(otherPC, otherValueType, otherIsNull, otherIsPrecise) ⇒
//                    if (otherPC != this.pc)
//                        return StructuralUpdate(MultipleReferenceValues(Set[AReferenceValue](this, v)))
//
//                    if (this.isNull == otherIsNull &&
//                        this.valueType == otherValueType &&
//                        (this.isPrecise == false || this.isPrecise == otherIsPrecise))
//                        return NoUpdate
//
//                    var newValueType = this.valueType
//                    otherValueType.foreach { otherValueType ⇒
//                        var addOtherValueType = true
//                        newValueType = newValueType.filterNot { vt ⇒
//                            domain.isSubtypeOf(otherValueType, vt) match {
//                                case Yes ⇒
//                                    true
//                                case _ ⇒
//                                    if (domain.isSubtypeOf(vt, otherValueType).yes)
//                                        addOtherValueType = false
//                                    false
//                            }
//                        }
//                        if (addOtherValueType)
//                            newValueType = newValueType + otherValueType
//                    }
//                    StructuralUpdate(AReferenceValue(
//                        this.pc,
//                        newValueType,
//                        this.isNull merge otherIsNull,
//                        this.isPrecise && otherIsPrecise
//                    ))
//
//                case mrv: MultipleReferenceValues ⇒
//                    mrv.merge(mergePC, this) match {
//                        case NoUpdate                 ⇒ StructuralUpdate(mrv)
//                        case SomeUpdate(updatedValue) ⇒ StructuralUpdate(updatedValue)
//                    }
//                case _ ⇒ MetaInformationUpdateIllegalValue
//            }
//        }
//
//        override def equals(other: Any): Boolean = {
//            other match {
//                case that: AReferenceValue ⇒
//                    (that eq this) || (
//                        (that canEqual this) &&
//                        this.pc == that.pc &&
//                        this.properties == that.properties &&
//                        this.valueType == that.valueType)
//                case _ ⇒ false
//            }
//        }
//
//        protected def canEqual(other: AReferenceValue): Boolean = true
//
//        override def hashCode: Int =
//            ((41 + pc) * 41 + properties) * 41 + valueType.hashCode()
//
//        override def toString() =
//            isNull match {
//                case Yes ⇒ "Null(pc="+pc+")"
//                case _ ⇒
//                    valueType.map(_.toJava).mkString(" with ")+
//                        "(pc="+pc+
//                        ", isNull="+isNull+
//                        ", isPrecise="+isPrecise+")"
//            }
//
//    }

//    /**
//     * Extractor for `AReferenceValue`s.
//     */
//    object AReferenceValue {
//        def unapply(arv: AReferenceValue): Option[(Int, TypeBound, Answer, Boolean)] =
//            Some((arv.pc, arv.valueType, arv.isNull, arv.isPrecise))
//    }
//
////    def AReferenceValue(
////        pc: Int,
////        valueType: TypeBound,
////        isNull: Answer,
////        isPrecise: Boolean): AReferenceValue =
////        new AReferenceValue(pc, valueType, isNull, isPrecise)
//
//    final def AReferenceValue(
//        pc: Int,
//        referenceType: ReferenceType,
//        isNull: Answer = Unknown,
//        isPrecise: Boolean = false): AReferenceValue =
//        AReferenceValue(pc, Set(referenceType), isNull, isPrecise)
//
//    case class MultipleReferenceValues(
//        val values: Set[AReferenceValue])
//            extends ReferenceValue { this: DomainValue ⇒
//
//        override def adapt[TDI >: I](targetDomain: Domain[TDI], pc: Int): targetDomain.DomainValue =
//            if (targetDomain.isInstanceOf[TaggableReferenceValues[TDI]]) {
//                val thatDomain = targetDomain.asInstanceOf[TaggableReferenceValues[TDI]]
//                val newValues = this.values.map { value: AReferenceValue ⇒
//                    value.adaptAReferenceValue(thatDomain, pc)
//                }
//                if (newValues.size == 1)
//                    newValues.head.asInstanceOf[targetDomain.DomainValue]
//                else
//                    thatDomain.MultipleReferenceValues(newValues).asInstanceOf[targetDomain.DomainValue]
//            } else
//                super.adapt(targetDomain, pc)
//
//        def isSubtypeOf(supertype: ReferenceType, onNull: ⇒ Answer): Answer = {
//            val firstAnswer = values.head.isSubtypeOf(supertype, onNull)
//            (firstAnswer /: values.tail) { (answer, nextReferenceValue) ⇒
//                (answer merge nextReferenceValue.isSubtypeOf(supertype, onNull)).
//                    orElse { return Unknown }
//            }
//        }
//
//        /**
//         * The set of all upper type bounds.
//         */
//        lazy val valueType: Set[ReferenceType] = values.flatMap(_.valueType)
//
//        def foreach[U](f: TypeBound ⇒ U): Unit = values.foreach(_.foreach(f))
//
//        override def nonEmpty = valueType.nonEmpty // values.forall(_.nonEmpty) // FIXME doesn't it have to be "values.exists(_.nonEmpty)"
//
//        override def size = valueType.size
//
//        def foreachType[U](f: ReferenceType ⇒ U): Unit = valueType.foreach(f)
//
//        def forallTypes(f: ReferenceType ⇒ Boolean): Boolean = valueType.forall(f)
//
//        def headType: ReferenceType = valueType.head
//
//        private def updateValues(update: AReferenceValue ⇒ AReferenceValue) = {
//            var createNew = false
//            val updatedValues = values.map { v ⇒
//                val updatedValue = update(v)
//                if (updatedValue ne v)
//                    createNew = true
//                updatedValue
//            }
//            if (createNew)
//                MultipleReferenceValues(updatedValues)
//            else
//                this
//        }
//
//        override def updateIsNull(pc: Int, isNull: Answer): ReferenceValue =
//            updateValues { aReferenceValue: AReferenceValue ⇒
//                aReferenceValue.updateIsNull(pc, isNull)
//            }
//
//        def addUpperBound(pc: Int, upperBound: ReferenceType): DomainValue = {
//            updateValues { aReferenceValue: AReferenceValue ⇒
//                aReferenceValue.addUpperBound(pc, upperBound)
//            }
//        }
//
//        // TODO refactor...
//        lazy val isPrecise: Boolean = values.forall(_.isPrecise)
//
//        // TODO refactor...
//        lazy val isNull: Answer = (values.head.isNull /: values.tail)(_ merge _.isNull)
//
//        override def merge(mergePC: Int, value: DomainValue): Update[DomainValue] = {
//            if (value eq this)
//                return NoUpdate
//
//            value match {
//                case otherArv: AReferenceValue ⇒
//                    values.find(_.pc == otherArv.pc) match {
//                        case None ⇒
//                            StructuralUpdate(MultipleReferenceValues(values + otherArv))
//                        case Some(thisArv) ⇒
//                            thisArv.merge(mergePC, otherArv) match {
//                                case NoUpdate ⇒ NoUpdate
//                                case update @ SomeUpdate(updatedArv: AReferenceValue) ⇒
//                                    update.updateValue(
//                                        MultipleReferenceValues((values - thisArv) + updatedArv)
//                                    )
//                            }
//                    }
//                case otherMrv: MultipleReferenceValues ⇒
//                    var updateType: UpdateType = NoUpdateType
//                    var otherRemainingArvs = otherMrv.values
//                    var newValues: Set[AReferenceValue] = values.map { thisArv ⇒
//                        otherRemainingArvs.find(_.pc == thisArv.pc) match {
//                            case None ⇒ thisArv
//                            case Some(otherArv) ⇒
//                                otherRemainingArvs -= otherArv
//                                thisArv.merge(mergePC, otherArv) match {
//                                    case NoUpdate ⇒
//                                        thisArv
//                                    case update @ SomeUpdate(updatedArv: AReferenceValue) ⇒
//                                        updateType = updateType &: update
//                                        updatedArv
//                                }
//                        }
//                    }
//                    if (otherRemainingArvs.size > 0) {
//                        newValues ++= otherRemainingArvs
//                        updateType = StructuralUpdateType
//                    }
//                    updateType(MultipleReferenceValues(newValues))
//                case _ ⇒ MetaInformationUpdateIllegalValue
//            }
//        }
//
//        override def toString() = {
//            values.mkString("OneOf(\n\t", ",\n\t", ")")
//        }
//    }
//
//    abstract override def types(value: DomainValue): TypesAnswer[_] = {
//        value match {
//            case r: ReferenceValue ⇒ r
//            case _                 ⇒ super.types(value)
//        }
//    }
//
//    //
//    // FACTORY METHODS
//    //
//
//    def newNullValue(pc: Int): DomainValue =
//        AReferenceValue(pc, Set.empty[ReferenceType], Yes, true)
//
//    def newReferenceValue(referenceType: ReferenceType): DomainValue =
//        AReferenceValue(-1, Set(referenceType), Unknown, false)
//
//    def newReferenceValue(pc: Int, referenceType: ReferenceType): DomainValue =
//        AReferenceValue(pc, Set(referenceType), Unknown, false)
//
//    def newObject(pc: Int, objectType: ObjectType): DomainValue =
//        AReferenceValue(pc, Set[ReferenceType](objectType), No, true)
//
//    def newInitializedObject(pc: Int, objectType: ObjectType): DomainValue =
//        newObject(pc, objectType)
//
//    def newStringValue(pc: Int, value: String): DomainValue =
//        AReferenceValue(pc, Set[ReferenceType](ObjectType.String), No, true)
//
//    def newClassValue(pc: Int, t: Type): DomainValue =
//        AReferenceValue(pc, Set[ReferenceType](ObjectType.Class), No, true)
//
//    def newArray(
//        pc: Int,
//        arrayType: ArrayType,
//        isNull: Answer = No,
//        isPrecise: Boolean = true): DomainValue =
//        AReferenceValue(pc, Set[ReferenceType](arrayType), isNull, isPrecise)
//
//    // -----------------------------------------------------------------------------------
//    //
//    // HANDLING OF COMPUTATIONS
//    //
//    // -----------------------------------------------------------------------------------
//
//    //
//    // CREATE ARRAY
//    //
//    def newarray(pc: Int,
//                 count: DomainValue,
//                 componentType: FieldType): Computation[DomainValue, DomainValue] =
//        //ComputedValueAndException(TypedValue(ArrayType(componentType)), TypedValue(ObjectType.NegativeArraySizeException))
//        ComputedValue(newArray(pc, ArrayType(componentType)))
//
//    /**
//     * @note The componentType may be (again) an array type.
//     */
//    def multianewarray(pc: Int,
//                       counts: List[DomainValue],
//                       arrayType: ArrayType) =
//        //ComputedValueAndException(TypedValue(arrayType), TypedValue(ObjectType.NegativeArraySizeException))
//        ComputedValue(newArray(pc, arrayType, No, true))
//
//    //
//    // LOAD FROM AND STORE VALUE IN ARRAYS
//    //
//    def aaload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
//        types(arrayref) match {
//            case HasSingleReferenceTypeBound(ArrayType(componentType)) ⇒
//                ComputedValue(newTypedValue(pc, componentType))
//            case _ ⇒
//                domainException(
//                    this,
//                    "cannot determine the type of the array's content: "+arrayref
//                )
//        }
//
//    def aastore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
//        ComputationWithSideEffectOnly
//}
//
//import analyses.ClassHierarchy
//
//trait TaggableReferenceValuesWithClosedHierarchy[+I]
//        extends TaggableReferenceValues[I] {
//
//    def classHierarchy: ClassHierarchy
//
//    override def newReferenceValue(referenceType: ReferenceType): DomainValue =
//        referenceType match {
//            case ot: ObjectType ⇒
//                val isPrecise = classHierarchy.subtypes(ot).isEmpty
//                AReferenceValue(-1, Set(referenceType), Unknown, isPrecise)
//            case _ ⇒ super.newReferenceValue(referenceType)
//        }
//
//    override def newReferenceValue(pc: Int, referenceType: ReferenceType): DomainValue =
//        referenceType match {
//            case ot: ObjectType ⇒
//                val isPrecise = classHierarchy.subtypes(ot).isEmpty
//                AReferenceValue(pc, Set(referenceType), Unknown, isPrecise)
//            case _ ⇒ super.newReferenceValue(pc, referenceType)
//        }
//
//}
