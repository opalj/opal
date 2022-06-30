/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.opalj.br.ComputationalTypeInt

/**
 * This domain implements the tracking of simple integer values.
 *
 * @see [[IntegerValues]] for more details.
 * @note   This domain uses a single object to represent some integer. I.e., this
 *         domain does not support the identification of values that may be equal.
 * @author Michael Eichberg
 */
trait DefaultIntegerValues extends DefaultSpecialDomainValuesBinding with IntegerValues {
    domain: Configuration with ExceptionsFactory =>

    /**
     * Represents an unspecific, unknown Integer value.
     */
    object AnIntegerValue extends super.AnIntegerValue {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] = NoUpdate

        override def abstractsOver(other: DomainValue): Boolean = {
            other.computationalType == ComputationalTypeInt
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.IntegerValue(origin = pc)
        }

        override def hashCode: Int = 101

        override def equals(other: Any): Boolean = {
            other match {
                case that: AnyRef => that eq this
                case _            => false
            }
        }

        override def toString: String = "an int"
    }

    /**
     * Factory method to create a new instance of [[AnIntegerValue]].
     */
    //def AnIntegerValue(): AnIntegerValue.type = AnIntegerValue

    /**
     * Represents a specific integer value in the range [`lowerBound`,`upperBound`].
     */
    class ConcreteIntegerValue(val value: Int) extends super.TheIntegerValue {

        override def constantValue: Option[Int] = Some(value)

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case AnIntegerValue => StructuralUpdate(AnIntegerValue)
                case that: ConcreteIntegerValue =>
                    if (that.value == this.value)
                        NoUpdate
                    else
                        StructuralUpdate(AnIntegerValue)
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (
                other match {
                    case that: ConcreteIntegerValue => this.value == that.value
                    case _                          => false
                }
            )
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.IntegerValue(pc, value)
        }

        override def hashCode = this.value * 13 + 11

        override def equals(other: Any): Boolean = {
            other match {
                case that: ConcreteIntegerValue => this.value == that.value
                case _                          => false
            }
        }

        override def toString: String = "int = "+value

    }

    override def BooleanValue(origin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def BooleanValue(origin: ValueOrigin, value: Boolean): ConcreteIntegerValue = {
        if (value) new ConcreteIntegerValue(1) else new ConcreteIntegerValue(0)
    }

    override def ByteValue(origin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def ByteValue(origin: ValueOrigin, value: Byte): ConcreteIntegerValue = {
        new ConcreteIntegerValue(value.toInt)
    }

    override def ShortValue(origin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def ShortValue(origin: ValueOrigin, value: Short): ConcreteIntegerValue = {
        new ConcreteIntegerValue(value.toInt)
    }

    override def CharValue(origin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def CharValue(origin: ValueOrigin, value: Char): ConcreteIntegerValue = {
        new ConcreteIntegerValue(value.toInt)
    }

    override def IntegerValue(origin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def IntegerValue(origin: ValueOrigin, value: Int): ConcreteIntegerValue = {
        new ConcreteIntegerValue(value)
    }

}
