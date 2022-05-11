/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.opalj.br.LongType

/**
 * Implements the shift operators for long values.
 *
 * @author Michael Eichberg
 * @author Riadh Chtara
 */
trait LongValuesShiftOperators extends LongValuesDomain {
    this: ConcreteLongValues with ConcreteIntegerValues =>

    override def lshl(
        pc: Int, longValue: DomainValue, shiftValue: DomainValue
    ): DomainTypedValue[LongType] = {
        this.longValue(longValue) { v =>
            this.intValue(shiftValue)(s =>
                LongValue(pc, v << s))(
                LongValue(pc)
            )
        } {
            LongValue(pc)
        }
    }

    override def lshr(
        pc: Int, longValue: DomainValue, shiftValue: DomainValue
    ): DomainTypedValue[LongType] = {
        this.longValue(longValue) { v =>
            this.intValue(shiftValue)(s =>
                LongValue(pc, v >> s))(
                LongValue(pc)
            )
        } {
            LongValue(pc)
        }
    }

    override def lushr(
        pc: Int, longValue: DomainValue, shiftValue: DomainValue
    ): DomainTypedValue[LongType] = {
        this.longValue(longValue) { v =>
            this.intValue(shiftValue)(s => LongValue(pc, v >>> s))(
                LongValue(pc)
            )
        } {
            LongValue(pc)
        }
    }
}
