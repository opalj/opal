/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import java.lang.Math.abs

import org.opalj.br.ComputationalTypeInt
import org.opalj.br.CTIntType

/**
 * This domain implements the tracking of integer values at the level of ranges.
 *
 * @author Michael Eichberg
 */
trait DefaultIntegerRangeValues extends DefaultSpecialDomainValuesBinding with IntegerRangeValues {
    domain: CorrelationalDomainSupport with Configuration with ExceptionsFactory =>

    /**
     * Represents a specific but unknown Integer value.
     *
     * @note The functionality to propagate a constraint crucially depends on
     *      the fact that two integer values that are not guaranteed to represent the
     *      same runtime value are represented by two different instances
     *      of "AnIntegerValue"; otherwise, propagating the
     *      constraint that some value (after some kind of check) has to have a special
     *      value may affect unrelated values!
     */
    class AnIntegerValue extends super.AnIntegerValueLike {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] = {
            // ...this method is only called if we are not joining the "same" value...
            MetaInformationUpdate(AnIntegerValue())
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other.computationalType == ComputationalTypeInt
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.IntegerValue(origin = pc)
        }

        override def newInstance: AnIntegerValue = AnIntegerValue()

        override def hashCode: Int = 101

        override def equals(other: Any): Boolean = {
            other match {
                case _: AnIntegerValue => true
                case _                 => false
            }
        }

        override def toString: String = "an int"
    }

    /**
     * Factory method to create a new instance of [[AnIntegerValue]].
     */
    def AnIntegerValue(): AnIntegerValue = new AnIntegerValue()

    /**
     * Represents a specific integer value in the range [`lowerBound`,`upperBound`].
     */
    class IntegerRange(val lowerBound: Int, val upperBound: Int) extends super.IntegerRangeLike {

        assert(
            lowerBound <= upperBound,
            s"the lower bound $lowerBound is larger than the upper bound $upperBound"
        )

        /**
         * Creates a new `IntegerRange` value that also represents the given value.
         * A new integer value is always created.
         */
        def update(newValue: Int): DomainValue = {
            val newLowerBound = if (lowerBound > newValue) newValue else lowerBound
            val newUpperBound = if (upperBound < newValue) newValue else upperBound

            new IntegerRange(newLowerBound, newUpperBound)
        }

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            val result = other match {

                case _: AnIntegerValue => StructuralUpdate(AnIntegerValue())

                case IntegerRangeLike(otherLB, otherUB) =>
                    val thisLB = this.lowerBound
                    val thisUB = this.upperBound
                    val newLB = Math.min(thisLB, otherLB)
                    val newUB = Math.max(thisUB, otherUB)

                    if (newLB == newUB) {
                        // This is a "point-range" (a concrete value), hence there
                        // will be NO further constraints
                        NoUpdate
                    } else if (newLB == thisLB && newUB == thisUB) {
                        // This is NOT a "NoUpdate" since we have two values that may
                        // have the same range, but which can still be two different
                        // runtime values (they were not created at the same time!
                        // The "CorrelationalDomain" will make sure that this udpate is
                        // handled.
                        MetaInformationUpdate(IntegerRange(newLB, newUB))
                    } else if (thisLB == thisUB && otherLB == otherUB) {
                        // We are joining two concrete values... in this case we always first
                        // create a "true" range.
                        StructuralUpdate(IntegerRange(newLB, newUB))
                    } else if (abs(newLB.toLong - newUB.toLong) > maxCardinalityOfIntegerRanges) {
                        // let's just use one of the default ranges..
                        var adjustedNewLB =
                            if (newLB < Short.MinValue) {
                                Int.MinValue
                            } else if (newLB < Byte.MinValue) {
                                Short.MinValue
                            } else if (newLB < 0) {
                                Byte.MinValue
                            } else {
                                0
                            }

                        val adjustedNewUB =
                            if (newUB > Char.MaxValue) {
                                Int.MaxValue
                            } else if (newUB > Short.MaxValue) {
                                Char.MaxValue
                            } else if (newUB > Byte.MaxValue) {
                                Short.MaxValue
                            } else if (newUB < 0) {
                                -1
                            } else {
                                Byte.MaxValue
                            }

                        if (adjustedNewLB > adjustedNewUB)
                            // we adjusted it too much... let's correct that
                            adjustedNewLB = adjustedNewUB

                        if (adjustedNewLB == Int.MinValue && adjustedNewUB == Int.MaxValue) {
                            StructuralUpdate(AnIntegerValue())
                        } else if (adjustedNewLB == thisLB && adjustedNewUB == thisUB) {
                            // We have no idea about the concrete values, but the range is the same
                            MetaInformationUpdate(IntegerRange(adjustedNewLB, adjustedNewUB))
                        } else {
                            StructuralUpdate(IntegerRange(adjustedNewLB, adjustedNewUB))
                        }
                    } else {
                        StructuralUpdate(IntegerRange(newLB, newUB))
                    }
            }
            result
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (
                other match {
                    case IntegerRangeLike(thatLB, thatUB) =>
                        this.lowerBound <= thatLB && this.upperBound >= thatUB
                    case _ => false
                }
            )
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target match {
                case irv: IntegerRangeValues =>
                    irv.IntegerRange(lowerBound, upperBound).asInstanceOf[target.DomainValue]
                case _ =>
                    target.IntegerValue(pc)
            }
        }

        override def newInstance: IntegerRange = IntegerRange(lowerBound, upperBound)

        override def constantValue: Option[ValueOrigin] = {
            if (lowerBound == upperBound)
                Some(lowerBound)
            else
                None
        }

        override def hashCode: Int = this.lowerBound * 13 + this.upperBound

        override def equals(other: Any): Boolean = {
            other match {
                case that: IntegerRange =>
                    (this eq that) || (
                        that.lowerBound == this.lowerBound && that.upperBound == this.upperBound
                    )
                case _ =>
                    false
            }
        }

        override def toString: String = {
            if (lowerBound == upperBound)
                "int = "+lowerBound
            else
                s"int ∈ [$lowerBound,$upperBound]"
        }
    }

    @inline final override def IntegerRange(lb: Int, ub: Int): IntegerRange = {
        new IntegerRange(lb, ub)
    }

    override def BooleanValue(origin: ValueOrigin): DomainTypedValue[CTIntType] = IntegerRange(0, 1)
    override def BooleanValue(origin: ValueOrigin, value: Boolean): DomainTypedValue[CTIntType] = {
        if (value) IntegerValue(origin, 1) else IntegerValue(origin, 0)
    }

    override def ByteValue(origin: ValueOrigin): DomainTypedValue[CTIntType] = {
        IntegerRange(Byte.MinValue, Byte.MaxValue)
    }
    override def ByteValue(origin: ValueOrigin, value: Byte): DomainTypedValue[CTIntType] = {
        val theValue = value.toInt
        new IntegerRange(theValue, theValue)
    }

    override def ShortValue(origin: ValueOrigin): DomainTypedValue[CTIntType] = {
        IntegerRange(Short.MinValue, Short.MaxValue)
    }
    override def ShortValue(origin: ValueOrigin, value: Short): DomainTypedValue[CTIntType] = {
        val theValue = value.toInt
        new IntegerRange(theValue, theValue)
    }

    override def CharValue(origin: ValueOrigin): DomainTypedValue[CTIntType] = {
        IntegerRange(Char.MinValue, Char.MaxValue)
    }
    override def CharValue(origin: ValueOrigin, value: Char): DomainTypedValue[CTIntType] = {
        val theValue = value.toInt
        new IntegerRange(theValue, theValue)
    }

    override def IntegerValue(origin: ValueOrigin): DomainTypedValue[CTIntType] = {
        AnIntegerValue()
    }
    override def IntegerValue(origin: ValueOrigin, value: Int): DomainTypedValue[CTIntType] = {
        new IntegerRange(value, value)
    }

}
