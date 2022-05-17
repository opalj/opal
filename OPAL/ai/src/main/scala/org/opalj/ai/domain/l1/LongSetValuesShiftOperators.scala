/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

/**
 * Implements the shift operators for long values.
 *
 * (The shift operators are put in their own module, because the shift value is
 * always an IntegerValue.)
 *
 * @author Michael Eichberg
 * @author Riadh Chtara
 * @author David Becker
 */
trait LongSetValuesShiftOperators extends LongValuesDomain {
    this: LongSetValues with ConcreteIntegerValues =>

    /*override*/ def lshl(pc: Int, value: DomainValue, shift: DomainValue): DomainValue = {
        value match {
            case LongSetLike(leftValues) =>
                intValue[DomainValue](shift) { s =>
                    LongSet(leftValues.map(_ << s))
                } {
                    if (leftValues.size == 1 && leftValues.head == 0)
                        value
                    else
                        LongValue(origin = pc)
                }

            case _ =>
                intValue(shift) { s =>
                    if (s == 0) value else LongValue(origin = pc)
                } {
                    LongValue(origin = pc)
                }
        }
    }

    /*override*/ def lshr(pc: Int, value: DomainValue, shift: DomainValue): DomainValue = {
        value match {
            case LongSetLike(leftValues) =>
                intValue[DomainValue](shift) { s =>
                    LongSet(leftValues.map(_ >> s))
                } {
                    if (leftValues.size == 1 && leftValues.head == 0)
                        value
                    else
                        LongValue(origin = pc)
                }

            case _ =>
                intValue(shift) { s =>
                    if (s == 0) value else LongValue(origin = pc)
                } {
                    LongValue(origin = pc)
                }
        }
    }

    /*override*/ def lushr(pc: Int, value: DomainValue, shift: DomainValue): DomainValue = {
        value match {
            case value @ LongSetLike(leftValues) =>
                intValue[DomainValue](shift) { s =>
                    LongSet(leftValues.map(_ >>> s))
                } {
                    if (leftValues.size == 1 && leftValues.head == 0)
                        value
                    else
                        LongValue(origin = pc)
                }

            case _ =>
                intValue(shift) { s =>
                    if (s == 0) value else LongValue(origin = pc)
                } {
                    LongValue(origin = pc)
                }
        }
    }

}

