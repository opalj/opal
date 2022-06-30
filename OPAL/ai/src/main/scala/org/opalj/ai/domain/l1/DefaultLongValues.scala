/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.opalj.br.ComputationalTypeLong

/**
 * This domain is able to track constant long values and to perform mathematical
 * operations related to constant long values.
 *
 * @author Michael Eichberg
 * @author Riadh Chtara
 * @author David Becker
 */
trait DefaultLongValues extends DefaultSpecialDomainValuesBinding with LongValues {
    domain: IntegerValuesFactory with ExceptionsFactory with Configuration =>

    /**
     * Represents an unspecific, unknown long value.
     */
    case object ALongValue extends super.ALongValue {

        override def constantValue: Option[Long] = None

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = NoUpdate

        override def abstractsOver(other: DomainValue): Boolean = {
            other.computationalType == ComputationalTypeLong
        }

        override def summarize(origin: ValueOrigin): DomainValue = this

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue = {
            target.LongValue(origin)
        }

    }

    /**
     * Represents a concrete long value.
     */
    class ConcreteLongValue(override val value: Long) extends super.TheLongValue {

        override def constantValue: Option[Long] = Some(value)

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case ConcreteLongValue(thatValue) =>
                    if (this.value == thatValue) {
                        NoUpdate
                    } else {
                        StructuralUpdate(LongValue(pc))
                    }
                case _ => StructuralUpdate(other)
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other match {
                case ConcreteLongValue(`value`) => true
                case _                          => false
            }
        }

        override def summarize(origin: ValueOrigin): DomainValue = this

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue = {
            target.LongValue(origin, value)
        }

        override def equals(other: Any): Boolean = {
            other match {
                case that: ConcreteLongValue => that.value == this.value
                case _                       => false
            }
        }

        override def hashCode: Int = (value ^ (value >>> 32)).toInt

        override def toString: String = "long ="+value
    }

    //
    // FACTORY METHODS
    //

    override def LongValue(origin: ValueOrigin): ALongValue.type = ALongValue

    override def LongValue(origin: ValueOrigin, value: Long): ConcreteLongValue = new ConcreteLongValue(value)

}
