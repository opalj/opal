/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.reflect.ClassTag

import java.lang.Math.max
import java.lang.Math.min

import scala.collection.immutable.SortedSet

import org.opalj.value.IsIntegerValue
import org.opalj.br.CTIntType

/**
 * This domain implements the tracking of integer values using sets.
 *
 * @author Michael Eichberg
 */
trait DefaultIntegerSetValues extends DefaultSpecialDomainValuesBinding with IntegerSetValues {
    domain: CorrelationalDomainSupport with Configuration with ExceptionsFactory =>

    class AnIntegerValue extends super.AnIntegerValueLike {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] = {
            // - we are not joining the "same" value
            // - the join stabilization trait takes care of handling potential aliases
            MetaInformationUpdate(AnIntegerValue())
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other.isInstanceOf[IsIntegerValue]
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.IntegerValue(pc)
        }

        override def hashCode: Int = 107

        override def equals(other: Any): Boolean = other.isInstanceOf[AnIntegerValue]

        override def toString: String = "AnIntegerValue"
    }

    def AnIntegerValue(): AnIntegerValue = new AnIntegerValue()

    class IntegerSet(val values: SortedSet[Int]) extends super.IntegerSetLike {

        assert(values.nonEmpty)

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            val result = other match {
                case _: AnIntegerValue => StructuralUpdate(AnIntegerValue())

                case that: BaseTypesBasedSet =>
                    // every base types based set is necessarily larger, because the
                    // absolute maximum cardinality is 254 (ByteSet.cardinality-1)
                    val thisLB = this.values.firstKey
                    val thisUB = this.values.lastKey
                    val newLB = min(thisLB, that.lowerBound)
                    val newUB = max(thisUB, that.upperBound)
                    StructuralUpdate(approximateSet(pc, newLB, newUB))

                case IntegerSetLike(thatValues) =>
                    val newValues = this.values ++ thatValues

                    if (newValues.size == 1) {
                        // This set represents a single, concrete value, hence there
                        // will be NO further constraints that affect this set.
                        // Furthermore, since we have "value semantics" for int
                        // values we are no longer concerned about potential aliasing
                        // relations
                        NoUpdate

                    } else if (newValues.size > maxCardinalityOfIntegerSets) {
                        StructuralUpdate(approximateSet(pc, newValues.firstKey, newValues.lastKey))

                    } else if (newValues.size == this.values.size) {
                        // This is NOT a "NoUpdate" since we have two values that may
                        // have the same range, but which can still be two different
                        // runtime values (they were not created at the same time!
                        MetaInformationUpdate(IntegerSet(this.values))
                    } else {
                        StructuralUpdate(IntegerSet(newValues))
                    }
            }
            result
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (
                other match {
                    case that: IntegerSet => that.values.subsetOf(this.values)
                    case _                => false
                }
            )
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            if (target.isInstanceOf[IntegerSetValues]) {
                val thatDomain = target.asInstanceOf[IntegerSetValues]
                thatDomain.IntegerSet(this.values).asInstanceOf[target.DomainValue]
            } else if (target.isInstanceOf[IntegerRangeValues]) {
                val thatDomain = target.asInstanceOf[IntegerRangeValues]
                val value = thatDomain.IntegerRange(this.values.firstKey, this.values.lastKey)
                value.asInstanceOf[target.DomainValue]
            } else {
                target.IntegerValue(pc)
            }
        }

        override def hashCode: Int = this.values.hashCode * 13

        override def equals(other: Any): Boolean = {
            other match {
                case that: IntegerSet => (this eq that) || (this.values == that.values)
                case _                => false
            }
        }

        override def toString: String = "IntegerSet("+values.mkString(",")+")"
    }

    // NOTE: IF WE HAVE TWO VALUES THAT ARE REFERENCE EQUAL, THEN THE
    // (UNKNOWN) UNDERLYING VALUE IS THE SAME!
    trait BaseTypesBasedSet extends super.BaseTypesBasedSetLike { this: DomainValue =>

        def name: String

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case _: AnIntegerValue => StructuralUpdate(AnIntegerValue())

                case that: BaseTypesBasedSet =>
                    val thisLB = this.lowerBound
                    val thisUB = this.upperBound
                    val newLB = min(thisLB, that.lowerBound)
                    val newUB = max(thisUB, that.upperBound)
                    if (thisLB == newLB && thisUB == newUB)
                        MetaInformationUpdate(newInstance)
                    else
                        StructuralUpdate(approximateSet(pc, newLB, newUB))

                case IntegerSetLike(thatValues) =>
                    val thisLB = this.lowerBound
                    val thisUB = this.upperBound
                    val thatLB = thatValues.firstKey
                    val thatUB = thatValues.lastKey
                    if (thisLB <= thatLB && thisUB >= thatUB)
                        MetaInformationUpdate(newInstance)
                    else {
                        val newLB = min(thisLB, thatLB)
                        val newUB = max(thisUB, thatUB)
                        StructuralUpdate(approximateSet(pc, newLB, newUB))
                    }

            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (
                other match {
                    case other: BaseTypesBasedSet =>
                        lowerBound <= other.lowerBound && upperBound >= other.upperBound
                    case that: IntegerSet =>
                        lowerBound <= that.values.firstKey && upperBound >= that.values.lastKey

                    case _ => false
                }
            )
        }

        override def summarize(pc: Int): DomainValue = this

        override def hashCode: Int = upperBound

        override def equals(other: Any): Boolean = {
            other match {
                case that: BaseTypesBasedSet =>
                    this.lowerBound == that.lowerBound && this.upperBound == that.upperBound
                case _ =>
                    false
            }
        }

        override def toString: String = s"$name=[$lowerBound,$upperBound]"
    }

    type DomainBaseTypesBasedSet = BaseTypesBasedSet
    val DomainBaseTypesBasedSet: ClassTag[DomainBaseTypesBasedSet] = implicitly

    def U7BitSet(): DomainTypedValue[CTIntType] = new U7BitSet

    class U7BitSet extends super.U7BitSetLike with BaseTypesBasedSet { this: DomainValue =>
        override def name = "Unsigned7BitValue"
        override def newInstance = new U7BitSet
        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            val result = target match {
                case isv: IntegerSetValues   => isv.U7BitSet()
                case irv: IntegerRangeValues => irv.IntegerRange(lowerBound, upperBound)
                case _                       => target.ByteValue(pc)
            }
            result.asInstanceOf[target.DomainValue]
        }
        override def constantValue: Option[Int] = None
    }

    def U15BitSet(): DomainTypedValue[CTIntType] = new U15BitSet()

    class U15BitSet extends super.U15BitSetLike with BaseTypesBasedSet { this: DomainValue =>
        override def name = "Unsigned15BitValue"
        override def newInstance = new U15BitSet
        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            val result = target match {
                case isv: IntegerSetValues   => isv.U15BitSet()
                case irv: IntegerRangeValues => irv.IntegerRange(lowerBound, upperBound)
                case _                       => target.ByteValue(pc)
            }
            result.asInstanceOf[target.DomainValue]
        }
        override def constantValue: Option[Int] = None
    }

    class ByteSet extends super.ByteSetLike with BaseTypesBasedSet {
        override def name = "ByteValue"
        override def newInstance = new ByteSet
        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.ByteValue(pc)
        }
        override def constantValue: Option[Int] = None
    }

    class ShortSet extends super.ShortSetLike with BaseTypesBasedSet {
        override def name = "ShortValue"
        override def newInstance = new ShortSet
        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.ShortValue(pc)
        }
        override def constantValue: Option[Int] = None
    }

    class CharSet extends super.CharSetLike with BaseTypesBasedSet {
        override def name = "CharValue"
        override def newInstance = new CharSet
        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.CharValue(pc)
        }
        override def constantValue: Option[Int] = None
    }

    override def IntegerSet(values: SortedSet[Int]): IntegerSet = new IntegerSet(values)

    override def BooleanValue(pc: Int): IntegerLikeValue = {
        if (maxCardinalityOfIntegerSets > 1)
            IntegerSet(SortedSet(0, 1))
        else
            new ByteSet()
    }

    override def BooleanValue(pc: Int, value: Boolean): DomainTypedValue[CTIntType] = {
        if (value) IntegerValue(pc, 1) else IntegerValue(pc, 0)
    }

    override def ByteValue(pc: Int): ByteSet = new ByteSet()
    override def ByteValue(pc: Int, value: Byte): DomainTypedValue[CTIntType] = {
        IntegerSet(value.toInt)
    }

    override def ShortValue(pc: Int): ShortSet = new ShortSet()
    override def ShortValue(pc: Int, value: Short): DomainTypedValue[CTIntType] = {
        IntegerSet(value.toInt)
    }

    override def CharValue(pc: Int): CharSet = new CharSet()
    override def CharValue(pc: Int, value: Char): DomainTypedValue[CTIntType] = {
        IntegerSet(value.toInt)
    }

    override def IntegerValue(pc: Int): AnIntegerValue = AnIntegerValue()
    override def IntegerValue(pc: Int, value: Int): DomainTypedValue[CTIntType] = {
        IntegerSet(value)
    }

}
