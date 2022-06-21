/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.LongType

/**
 * Defines the primary factory methods to create `long` values.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait LongValuesFactory extends ValuesDomain { domain =>

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def LongValue(origin: ValueOrigin): DomainTypedValue[LongType]

    /**
     * Factory method to create a `DomainValue` that represents the given long value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def LongValue(origin: ValueOrigin, value: Long): DomainTypedValue[LongType]

}

