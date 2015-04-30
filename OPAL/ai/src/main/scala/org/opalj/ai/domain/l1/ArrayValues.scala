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
package org.opalj
package ai
package domain
package l1

import scala.collection.SortedSet
import org.opalj.br._
import org.opalj.collection.commonPrefix
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import scala.reflect.ClassTag

/**
 * Enables the tracking of various properties related to arrays.
 *
 * This domain in particular enables the tracking of an array's concrete content
 * in some specific cases (e.g., the Strings stored in an array or some primitive values)
 * or the tracking of information about an array's elements at a higher
 * level. In both cases only arrays up to a specified size (cf. [[maxArraySize]]) are
 * tracked. The content of array's of mutable data-structures cannot be tracked since
 * the infrastructure to "update the array's content if the referenced value is changed"
 * is not available!
 *
 * @note '''This domain does not require modeling the heap'''. This however, strictly limits
 *      the kind of arrays that can be tracked/the information about elements that
 *      can be tracked. Tracking the contents of arrays of mutable values is not possible;
 *      unless we only
 *      track abstract properties that do not depend on the concrete array element's value.
 *      For example, if we just want to know the upper type bounds of the values stored
 *      in the array, then it is perfectly possible. This property cannot change in an
 *      unsound fashion without directly accessing the array.
 *
 * @note This domain requires that the instantiated domain is only used to analyze a
 *      one method.
 *
 * @author Michael Eichberg
 */
trait ArrayValues extends l1.ReferenceValues with PerInstructionPostProcessing with PostEvaluationMemoryManagement {
    domain: CorrelationalDomain with IntegerValuesDomain with ConcreteIntegerValues with TypedValuesFactory with Configuration with ClassHierarchy with LogContextProvider ⇒

    /**
     * Determines the maximum size of those arrays for which we track the content.
     * The default value is 16.
     *
     * This setting can dynamically be adapted at runtime and will be considered
     * for each new array that is created afterwards.
     */
    def maxArraySize: Int = 16

    /**
     * Returns `true` if instances of the given type - including subtypes - are
     * always effectively immutable. For example, `java.lang.String` and `java.lang.Class`
     * objects are effectively immutable.
     *
     * @note This method is used by the default implementation of [[reifyArray]] to
     *      decide if we want to track the array's content.
     */
    protected def isEffectivelyImmutable(objectType: ObjectType): Boolean = {
        (objectType eq ObjectType.String) || (objectType eq ObjectType.Class)
    }

    /**
     * Returns `true` if the specified array should be reified and precisely tracked.
     *
     * '''This method is intended to be overwritten by subclasses to configure which
     * arrays will be reified.''' Depending on the analysis task, it is in general only
     * useful to track selected arrays (e.g, arrays of certain types of values
     * or up to a specific length). For example, to facilitate the resolution
     * of reflectively called methods, it might be interesting to track arrays
     * that contain string values.
     *
     * By default only arrays of known immutable values up to a size of [[maxArraySize]]
     * are reified.
     *
     * @note Tracking the content of arrays generally has a significant performance
     *      impact and should be limited to cases where it is absolutely necessary.
     *      "Just tracking the contents of arrays" to improve the overall precision
     *      is in most cases not helpful.
     *
     * @note If we track information about the values of an array at a higher-level,
     *      where the properties do not depend on the concrete values, then it is also
     *      possible to track those arrays.
     */
    protected def reifyArray(pc: PC, count: Int, arrayType: ArrayType): Boolean = {
        count <= maxArraySize && (
            arrayType.componentType.isBaseType ||
            (
                arrayType.componentType.isObjectType &&
                isEffectivelyImmutable(arrayType.componentType.asObjectType)
            )
        )
    }

    // We do not refine the type DomainArrayValue any further since we also want
    // to use the super level ArrayValue class to represent arrays for which we have
    // no further knowledge about the size/the content.
    // DON'T DO: type DomainArrayValue <: ArrayValue with DomainSingleOriginReferenceValue

    type DomainInitializedArrayValue <: InitializedArrayValue with DomainArrayValue
    val DomainInitializedArrayValue: ClassTag[DomainInitializedArrayValue]

    type DomainConcreteArrayValue <: ConcreteArrayValue with DomainArrayValue
    val DomainConcreteArrayValue: ClassTag[DomainConcreteArrayValue]

    /**
     * Represents some (multi-dimensional) array where the (initialized) dimensions have
     * the given size.
     *
     * @param lengths The list of the sizes of each initialized dimension.
     *      Currently, at most two dimensions are supported.
     */
    // NOTE THAT WE CANNOT STORE SIZE INFORMATION ABOUT N-DIMENSIONAL ARRAYS WHERE N IS
    // LARGER THAN 2 DUE TO THE LACK OF THE MODELING OF THE HEAP
    protected class InitializedArrayValue(
        origin: ValueOrigin,
        theType: ArrayType,
        val lengths: List[Int],
        t: Timestamp)
            extends ArrayValue(origin, isNull = No, isPrecise = true, theType, t) {
        this: DomainInitializedArrayValue ⇒

        def this(
            origin: ValueOrigin,
            theType: ArrayType,
            length: Int,
            t: Timestamp) = {
            this(origin, theType, lengths = List(length), t)
        }

        assert(length.size > 0, "uninitialized arrays are not supported")
        assert(
            lengths.size <= 2,
            s"tracking the concrete size of the ${lengths.size}th Dimension of arrays"+
                "is not supported (we are not modeling the heap)")

        /**
         * The length of the first dimension of this multi-dimensional array.
         */
        override def length: Some[Int] = Some(lengths.head)

        override def updateT(
            t: Timestamp,
            origin: ValueOrigin, isNull: Answer): DomainArrayValue = {
            InitializedArrayValue(origin, theUpperTypeBound, lengths, t)
        }

        /**
         * Extends `super.doLoad` by returning an initialized array object value that
         * reflects the size of the array.
         *
         * @note The returned array value always gets a new timestamp since the array
         *      field may have been updated.
         *      (It would be possible to use this array's timestamp if stores of
         *      (sub-)arrays with a different timestamp would lead to an update of the
         *      timestamp of this array.)
         */
        override def doLoad(
            pc: PC,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayLoadResult = {
            if (lengths.size > 1) {
                val value =
                    InitializedArrayValue(
                        origin,
                        theType.componentType.asArrayType,
                        lengths.tail,
                        nextT())
                ComputedValueOrException(value, potentialExceptions)
            } else {
                super.doLoad(pc, index, potentialExceptions)
            }
        }

        override def doStore(
            pc: PC,
            value: DomainValue,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayStoreResult = {

            //println(s"$pc - $value - $index - $potentialExceptions")

            if (lengths.size > 1) {
                value match {
                    // We don't have to consider the timestamp since every subarray
                    // gets a new timestamp when it is extracted anyway.
                    case DomainInitializedArrayValue(that) if (that.theUpperTypeBound eq this.theType.componentType) &&
                        that.lengths.startsWith(this.lengths.tail) ⇒
                        // well, our knowledge about the second dimension remains intact
                        super.doStore(pc, value, index, potentialExceptions)
                    case DomainConcreteArrayValue(that) if (that.theUpperTypeBound eq this.theType.componentType) &&
                        that.length.get == this.lengths.tail.head ⇒
                        // well, our knowledge about the second dimension remains intact
                        super.doStore(pc, value, index, potentialExceptions)
                    case _ ⇒
                        // We are now storing some value in this array; this basically
                        // invalidates our knowledge about the second dimension - unless
                        // an exception is raised - in this case the array remains
                        // unchanged; hence, we only have to schedule an update if no
                        // exception is raised!
                        updateAfterEvaluation(this, InitializedArrayValue(origin, theType, lengths.take(1), t))
                        super.doStore(pc, value, index, potentialExceptions)
                }
            } else {
                super.doStore(pc, value, index, potentialExceptions)
            }
        }

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {

            other match {
                case DomainInitializedArrayValue(that) if (this.theUpperTypeBound eq that.theUpperTypeBound) ⇒
                    val prefix = commonPrefix(this.lengths, that.lengths)
                    if (prefix eq this.lengths) {
                        if (this.t == that.t)
                            NoUpdate
                        else
                            TimestampUpdate(this.updateT(nextT()))
                    } else if (prefix eq that.lengths) {
                        StructuralUpdate(that.updateT(nextT()))
                    } else {
                        val newT = if (this.t == that.t) this.t else nextT()
                        if (prefix.nonEmpty)
                            StructuralUpdate(InitializedArrayValue(origin, this.theType, prefix, newT))
                        else
                            StructuralUpdate(ArrayValue(origin, No, true, this.theType, newT))
                    }

                //                case DomainConcreteArrayValue(that) if (this.theUpperTypeBound eq that.theUpperTypeBound) ⇒
                //                    if (this.lengths.tail.isEmpty && this.lengths.head == that.length.get) {
                //                        if (this.t == that.t)
                //                            NoUpdate
                //                        else
                //                            TimestampUpdate(this.updateT(nextT()))
                //                    } else if (prefix eq that.lengths) {
                //                        StructuralUpdate(that.updateT(nextT()))
                //                    } else {
                //                        val newT = if (this.t == that.t) this.t else nextT()
                //                        if (prefix.nonEmpty)
                //                            StructuralUpdate(InitializedArrayValue(origin, this.theType, prefix, newT))
                //                        else
                //                            StructuralUpdate(ArrayValue(origin, No, true, this.theType, newT))
                //                    }

                case _ ⇒
                    super.doJoinWithNonNullValueWithSameOrigin(joinPC, other) match {
                        case NoUpdate ⇒
                            // => This array and the other array have a corresponding
                            //    abstract representation (w.r.t. the next abstraction level!)
                            //    but we still need to drop the concrete information
                            val newT = if (other.t == this.t) this.t else nextT()
                            StructuralUpdate(
                                ArrayValue(origin, No, true, theType, newT)
                            )
                        case answer ⇒ answer
                    }
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            other match {
                case that: InitializedArrayValue ⇒
                    (this.theUpperTypeBound eq that.theUpperTypeBound) &&
                        this.lengths.size <= that.lengths.size &&
                        that.lengths.startsWith(this.lengths)
                case that: ConcreteArrayValue ⇒
                    (that.theUpperTypeBound eq this.theUpperTypeBound) && {
                        this.lengths.head == that.length.get && (
                            this.lengths.tail.isEmpty || {
                                val subArrayValue = InitializedArrayValue(origin, theType.componentType.asArrayType, this.lengths.tail)
                                that.values.forall { v ⇒ subArrayValue.abstractsOver(v) }
                            }
                        )
                    }

                case _ ⇒ false
            }
        }

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.InitializedArrayValue(vo, theType, lengths.take(1))
        }

        override def equals(other: Any): Boolean = other match {
            case DomainInitializedArrayValue(that) ⇒
                (that eq this) || (
                    (that canEqual this) &&
                    this.origin == that.origin &&
                    (this.theUpperTypeBound eq that.theUpperTypeBound) &&
                    this.lengths == that.lengths
                )
            case _ ⇒
                false
        }

        override protected def canEqual(other: ArrayValue): Boolean =
            other.isInstanceOf[InitializedArrayValue]

        override def hashCode: Int = (origin * 31 + upperTypeBound.hashCode) * 31

        override def toString() = {
            val theType = theUpperTypeBound.toJava
            val lengths = this.lengths.mkString("[", "][", "]")
            s"$theType[@$origin;lengths=$lengths;t=$t]"
        }

    }

    /**
     * Represents arrays and their content.
     *
     * The tracking of the content of an array is only possible as long as the
     * array is not merged with another array created at a different point in time.
     * From that point on, it is no longer possible to track the content of the arrays
     * that are merged as well as the "merged array" it self since operations on the
     * "merged array" are not reflected in the original arrays.
     */
    // NOTE THAT WE DO NOT SUPPORT THE CASE WHERE THE ARRAY STORES CONCRETE MUTABLE VALUES!
    // In that case it may be possible to load a value from the array and manipulate
    // it which could lead to a new domain value which is not referred to by the array!
    protected class ConcreteArrayValue(
        origin: ValueOrigin,
        theType: ArrayType,
        val values: Array[DomainValue],
        t: Timestamp)
            extends ArrayValue(origin, isNull = No, isPrecise = true, theType, t) {
        this: DomainConcreteArrayValue ⇒

        override def length: Some[Int] = Some(values.size)

        override def doLoad(
            loadPC: PC,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayLoadResult = {
            if (potentialExceptions.nonEmpty) {
                // - a "NullPointerException" is not possible
                // - if an ArrayIndexOutOfBoundsException may be thrown then we certainly
                //   do not have enough information about the index...
                return ComputedValueOrException(
                    TypedValue(loadPC, theUpperTypeBound.componentType),
                    potentialExceptions)
            }

            intValue[ArrayLoadResult](index) { index ⇒
                ComputedValue(values(index))
            } {
                // This handles the case that we know that the index is not precise
                // but it is still known to be valid.
                super.doLoad(loadPC, index, potentialExceptions)
            }
        }

        override def doStore(
            storePC: PC,
            value: DomainValue,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayStoreResult = {
            // Here, a "NullPointerException" is not possible

            if (potentialExceptions.nonEmpty) {
                // In both of the following cases, the array remains untouched:
                // - if an ArrayIndexOutOfBoundsException is thrown then the index
                //   is invalid
                // - if an ArrayStoreException may be thrown, we are totally lost..
                //
                // However, if some exception may be thrown, then we certainly
                // do not have enough information about the value/the index and
                // we are no longer able to track the array's content.
                val abstractValue =
                    InitializedArrayValue(origin, theType, List(values.size), t)
                updateAfterEvaluation(this, abstractValue)
                return ComputationWithSideEffectOrException(potentialExceptions);
            }

            // If we reach this point no exception will be thrown!
            // However, we now have to provide the solution for the happy path
            intValue[ArrayStoreResult](index) { index ⇒
                // let's check if we need to do anything
                if (values(index) ne value) {
                    val updatedValue =
                        ArrayValue(origin, theType, values.updated(index, value), t)
                    updateAfterEvaluation(this, updatedValue)
                }
                ComputationWithSideEffectOnly
            } {
                // This handles the case that the index is not precise, but is still
                // known to be valid. In this case we have to resort to the
                // abstract representation of the array.
                val abstractValue =
                    InitializedArrayValue(origin, theType, List(values.size), t)
                updateAfterEvaluation(this, abstractValue)
                ComputationWithSideEffectOnly
            }
        }

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {

            other match {
                case DomainConcreteArrayValue(that) if this.values.size == that.values.size && this.t == that.t ⇒
                    var update: UpdateType = NoUpdateType
                    var isOther: Boolean = true
                    val allValues = this.values.view.zip(that.values)
                    val newValues =
                        (allValues map { (v) ⇒
                            val (v1, v2) = v
                            if (v1 ne v2) {
                                val joinResult = v1.join(joinPC, v2)
                                joinResult match {
                                    case NoUpdate ⇒
                                        v1
                                    case SomeUpdate(newValue) ⇒
                                        if (v2 ne newValue) {
                                            isOther = false
                                        }
                                        update = joinResult &: update
                                        newValue
                                }
                            } else
                                v1
                        }).toArray // <= forces the evaluation - WHICH IS REQUIRED
                    update match {
                        case NoUpdateType ⇒ NoUpdate
                        case _ ⇒
                            if (isOther) {
                                update(other)
                            } else
                                update(ArrayValue(origin, theType, newValues))
                    }

                // case DomainInitializedArrayValue(that) ⇒

                case _ ⇒
                    val answer = super.doJoinWithNonNullValueWithSameOrigin(joinPC, other)
                    if (answer == NoUpdate) {
                        // => This array and the other array have a corresponding
                        //    abstract representation (w.r.t. the next abstraction level!)
                        //    but we still need to drop the concrete information
                        val abstractValue = ArrayValue(origin, No, true, theType, nextT)
                        StructuralUpdate(abstractValue)
                    } else {
                        answer
                    }

            }
        }

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue =
            target match {

                case thatDomain: l1.ArrayValues ⇒
                    val adaptedValues =
                        values.map(_.adapt(target, vo).asInstanceOf[thatDomain.DomainValue])
                    thatDomain.ArrayValue(
                        vo, theUpperTypeBound, adaptedValues).
                        asInstanceOf[target.DomainValue]

                case thatDomain: l1.ReferenceValues ⇒
                    thatDomain.ArrayValue(vo, No, true, theUpperTypeBound, thatDomain.nextT()).
                        asInstanceOf[target.DomainValue]

                case thatDomain: l0.TypeLevelReferenceValues ⇒
                    thatDomain.InitializedArrayValue(vo, theUpperTypeBound, List(values.size)).
                        asInstanceOf[target.DomainValue]

                case _ ⇒ super.adapt(target, vo)
            }

        override def equals(other: Any): Boolean = {
            other match {
                case DomainConcreteArrayValue(that) ⇒ (
                    (that eq this) ||
                    (
                        (that canEqual this) &&
                        this.origin == that.origin &&
                        (this.theUpperTypeBound eq that.theUpperTypeBound) &&
                        this.values == that.values
                    )
                )
                case _ ⇒ false
            }
        }

        override protected def canEqual(other: ArrayValue): Boolean =
            other.isInstanceOf[ConcreteArrayValue]

        override def hashCode: Int = origin * 79 + upperTypeBound.hashCode

        override def toString() = {
            val valuesAsString = values.mkString("«", ", ", "»")
            s"${theType.toJava}[@$origin;length=${values.size};t=$t,$valuesAsString]"
        }
    }

    override def NewArray(
        pc: PC,
        count: DomainValue,
        arrayType: ArrayType): DomainArrayValue = {

        val sizeOption = this.intValueOption(count)
        if (sizeOption.isEmpty)
            return ArrayValue(pc, No, isPrecise = true, arrayType, nextT()); // <====== early return

        val size: Int = sizeOption.get
        if (!reifyArray(pc, size, arrayType))
            return InitializedArrayValue(pc, arrayType, List(size));

        if (size >= 256)
            OPALLogger.logOnce(
                Warn(
                    "analysis configuration",
                    s"tracking very large arrays (${arrayType.toJava}) "+
                        "usually incurrs significant overhead without increasing "+
                        "the precision of the analysis.")
            )
        val virtualOrigin = 0xFFFF + pc * 1024

        val array: Array[DomainValue] = new Array[DomainValue](size)
        var i = 0; while (i < size) {
            // we initialize each element with a new instance and also
            // assign each value with a unique PC
            array(i) = DefaultValue(virtualOrigin + i, arrayType.componentType)
            i += 1
        }
        ArrayValue(pc, arrayType, array)

    }

    override def NewArray(
        origin: ValueOrigin,
        counts: List[DomainValue],
        arrayType: ArrayType): DomainArrayValue = {
        var intCounts: List[Int] = Nil
        counts.takeWhile { c ⇒
            intValue(c) { intCount ⇒ intCounts ::= intCount; true } { false }
        }
        if (intCounts.nonEmpty) {
            InitializedArrayValue(origin, arrayType, intCounts, nextT())
        } else {
            super.NewArray(origin, counts, arrayType)
        }
    }

    //
    // DECLARATION OF ADDITIONAL FACTORY METHODS
    //

    protected def ArrayValue( // for ArrayValue
        origin: ValueOrigin,
        theUpperTypeBound: ArrayType,
        values: Array[DomainValue]): DomainArrayValue

    protected def ArrayValue( // for ArrayValue
        origin: ValueOrigin,
        theUpperTypeBound: ArrayType,
        values: Array[DomainValue],
        t: Timestamp): DomainArrayValue

    def InitializedArrayValue(
        origin: ValueOrigin,
        arrayType: ArrayType,
        counts: List[Int],
        t: Timestamp): DomainArrayValue

}
