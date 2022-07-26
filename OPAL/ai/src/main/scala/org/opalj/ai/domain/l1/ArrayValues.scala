/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.reflect.ClassTag

import org.opalj.br.ArrayType

/**
 * Enables the tracking of the length of arrays in the most common cases.
 * Joins of arrays are only supported if both arrays have the same length.
 *
 * @note     Mixin of MaxArrayLengthRefinement may still be useful.
 *
 * @author   Michael Eichberg
 */
trait ArrayValues extends l1.ReferenceValues {
    domain: CorrelationalDomain with ConcreteIntegerValues =>

    // We do not refine the type DomainArrayValue any further since we also want
    // to use the super level ArrayValue class to represent arrays for which we have
    // no further knowledge about the size.
    // DON'T DO: type DomainArrayValue <: ArrayValue with DomainSingleOriginReferenceValue

    type DomainInitializedArrayValue <: InitializedArrayValue with DomainArrayValue
    val DomainInitializedArrayValueTag: ClassTag[DomainInitializedArrayValue]

    // IMPROVE Extend MultipleReferenceValues to handle the case that we reference multiple arrays.
    // IMPROVE Add support to track the size of arrays independent of a concrete instance

    /**
     * Represents some (multi-dimensional) array where the (initialized) dimensions have
     * the given size.
     *
     */
    // NOTE THAT WE CANNOT STORE SIZE INFORMATION FOR AlL DIMENSIONS BEYOND THE FIRST ONE;
    // WE ARE NOT TRACKING THE ESCAPE STATE!
    trait InitializedArrayValue extends ArrayValue { this: DomainInitializedArrayValue =>

        /**
         * The size of the first dimension of the array. (The size of this dimension is immutable!)
         */
        def theLength: Int

        assert(length.get >= 0, "impossible length")

        final override def length: Option[Int] = Some(theLength)

        final override def isNull: No.type = No

        final override def isPrecise: Boolean = true

        override def updateRefId(
            refId:  RefId,
            origin: ValueOrigin = this.origin,
            isNull: Answer      = this.isNull
        ): DomainArrayValue = {
            InitializedArrayValue(origin, theUpperTypeBound, theLength, refId)
        }

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: Int,
            other:  DomainSingleOriginReferenceValue
        ): Update[DomainSingleOriginReferenceValue] = {

            other match {
                case DomainInitializedArrayValueTag(that) =>
                    if (this.theUpperTypeBound eq that.theUpperTypeBound) {
                        if (that.theLength == this.theLength) {
                            if (this.refId == that.refId)
                                NoUpdate
                            else
                                RefIdUpdate(this.updateRefId(nextRefId()))
                        } else {
                            // the lengths are different...
                            val newRefId = if (this.refId == that.refId) this.refId else nextRefId()
                            StructuralUpdate(ArrayValue(origin, No, isPrecise = true, this.theUpperTypeBound, newRefId))
                        }
                    } else {
                        classHierarchy.joinArrayTypes(this.theUpperTypeBound, that.theUpperTypeBound) match {
                            case Left(newType) =>
                                StructuralUpdate(ArrayValue(origin, No, isPrecise = false, newType, nextRefId()))
                            case Right(utb) =>
                                StructuralUpdate(ObjectValue(origin, No, utb, nextRefId()))
                        }
                    }

                case _ =>
                    super.doJoinWithNonNullValueWithSameOrigin(joinPC, other) match {
                        case NoUpdate =>
                            // => This array and the other array have a corresponding
                            //    abstract representation (w.r.t. the next abstraction level!)
                            //    but we still need to drop the concrete information about the
                            //    length!
                            val thisRefId = this.refId
                            val newRefId = if (other.refId == thisRefId) thisRefId else nextRefId()
                            StructuralUpdate(ArrayValue(origin, No, true, theUpperTypeBound, newRefId))
                        case answer =>
                            answer
                    }
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            other match {
                case that: InitializedArrayValue =>
                    theLength == that.theLength && (theUpperTypeBound eq that.theUpperTypeBound)

                case _ =>
                    false
            }
        }

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            val adaptedValue = target match {
                case av: l1.ArrayValues =>
                    av.InitializedArrayValue(vo, theUpperTypeBound, theLength)
                case rv: l1.ReferenceValues =>
                    rv.ArrayValue(vo, No, isPrecise = true, theUpperTypeBound, rv.nextRefId())
                case _ =>
                    target.ReferenceValue(vo, theUpperTypeBound)
            }
            adaptedValue.asInstanceOf[target.DomainValue]
        }

        override def equals(other: Any): Boolean = other match {
            case DomainInitializedArrayValueTag(that) =>
                (that eq this) || (
                    (that canEqual this) &&
                    this.origin == that.origin &&
                    this.theLength == that.theLength &&
                    (this.theUpperTypeBound eq that.theUpperTypeBound)
                )
            case _ =>
                false
        }

        override protected def canEqual(other: ArrayValue): Boolean = {
            other.isInstanceOf[InitializedArrayValue]
        }

        override def hashCode: Int = (origin * 31 + upperTypeBound.hashCode) * 31 + theLength

        override def toString: String = {
            val theType = theUpperTypeBound.toJava
            s"$theType[↦$origin;refId=$refId;length=$theLength]"
        }

    }

    override def NewArray(
        pc:        Int,
        length:    DomainValue,
        arrayType: ArrayType
    ): DomainArrayValue = {
        this.intValue(length) { length =>
            InitializedArrayValue(pc, arrayType, length)
        } {
            super.NewArray(pc, length, arrayType)
        }
    }

    /**
     * The lengths per dimension are found in the following order:
     * `..., count1, [count2, ...] ->`
     */
    override def NewArray(
        pc:        Int,
        lengths:   Operands,
        arrayType: ArrayType
    ): DomainArrayValue = {
        intValue(lengths.last) { length =>
            InitializedArrayValue(pc, arrayType, length)
        } {
            super.NewArray(pc, lengths, arrayType)
        }
    }

    //
    // DECLARATION OF ADDITIONAL FACTORY METHODS (REQUIRED FOR DOMAIN VALUE ADAPTATION!)
    //

    final def InitializedArrayValue(
        origin:  ValueOrigin,
        theType: ArrayType,
        length:  Int
    ): DomainArrayValue = {
        InitializedArrayValue(origin, theType, length, nextRefId())
    }

    def InitializedArrayValue(
        origin:    ValueOrigin,
        arrayType: ArrayType,
        length:    Int,
        refId:     RefId
    ): DomainArrayValue

}
