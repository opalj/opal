/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.br.ComputationalTypeInt

/**
 * Base implementation of the `TypeLevelIntegerValues` trait that requires that
 * the domain's Value` trait is not extended. This implementation satisfies
 * the requirements of OPAL w.r.t. the domain's computational type. Additionally,
 * it collects information about a value's range, if possible.
 *
 * This domain is highly efficient as it uses a single value domain value to represents
 * all values of the same primitive type.
 *
 * =Adaptation/Reusability=
 * This domain '''does not support constraint propagation''' – due to its reuse of the
 * the same instance of a DomainValue across all potential instantiations of such values –
 * and should not be used to implement such a domain as this requires the
 * reimplementation of basically '''all''' methods.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelIntegerValues
    extends DefaultSpecialDomainValuesBinding
    with TypeLevelIntegerValues {
    this: Configuration =>

    //
    // IMPLEMENTATION NOTE
    //
    // It is safe to use singleton objects in this case since we do not propagate
    // constraints.
    // I.e., all constraints that are stated by the AI (e.g., `intHasValue`) are
    // completely ignored.
    //

    case object ABooleanValue extends super.BooleanValue {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] =
            value match {
                case ABooleanValue => NoUpdate
                case _             => StructuralUpdate(AnIntegerValue)
            }

        override def abstractsOver(other: DomainValue): Boolean = other eq this

        override def constantValue: Option[Boolean] = None
    }

    case object AByteValue extends super.ByteValue {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] =
            value match {
                case ABooleanValue | AByteValue => NoUpdate
                case _                          => StructuralUpdate(AnIntegerValue)
            }

        override def abstractsOver(other: DomainValue): Boolean = {
            (other eq this) || (other eq ABooleanValue)
        }

        override def constantValue: Option[Byte] = None

    }

    case object AShortValue extends super.ShortValue {

        override def doJoin(pc: Int, that: DomainValue): Update[DomainValue] =
            that match {
                case ABooleanValue | AByteValue | AShortValue =>
                    NoUpdate
                case _ =>
                    StructuralUpdate(AnIntegerValue)
            }

        override def abstractsOver(other: DomainValue): Boolean = {
            (other eq this) || (other eq ABooleanValue) || (other eq AByteValue)
        }

        override def constantValue: Option[Short] = None
    }

    case object ACharValue extends super.CharValue {

        override def doJoin(pc: Int, that: DomainValue): Update[DomainValue] =
            that match {
                case ABooleanValue | ACharValue => NoUpdate
                case _                          => StructuralUpdate(AnIntegerValue)
            }

        override def abstractsOver(other: DomainValue): Boolean = {
            (other eq this) || (other eq ABooleanValue)
        }

        override def constantValue: Option[Char] = None
    }

    case object AnIntegerValue extends super.IntegerValue {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] = {
            // the other value also has computational type Int
            NoUpdate
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (other ne TheIllegalValue) &&
                other.computationalType == ComputationalTypeInt
        }

        override def constantValue: Option[Int] = None
    }

    override def BooleanValue(valueOrigin: ValueOrigin): ABooleanValue.type = ABooleanValue
    override def BooleanValue(valueOrigin: ValueOrigin, value: Boolean): ABooleanValue.type = {
        ABooleanValue
    }

    override def ByteValue(valueOrigin: ValueOrigin): AByteValue.type = AByteValue
    override def ByteValue(valueOrigin: ValueOrigin, value: Byte): AByteValue.type = AByteValue

    override def ShortValue(valueOrigin: ValueOrigin): AShortValue.type = AShortValue
    override def ShortValue(valueOrigin: ValueOrigin, value: Short): AShortValue.type = AShortValue

    override def CharValue(valueOrigin: ValueOrigin): ACharValue.type = ACharValue
    override def CharValue(valueOrigin: ValueOrigin, value: Char): ACharValue.type = ACharValue

    override def IntegerValue(valueOrigin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def IntegerValue(valueOrigin: ValueOrigin, value: Int): AnIntegerValue.type = {
        AnIntegerValue
    }
}
