/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.DoubleType

/**
 * Defines the primary factory methods for Double values.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait DoubleValuesFactory extends ValuesDomain { domain =>

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter, but where
     * we have no knowledge about the precise value.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def DoubleValue(origin: ValueOrigin): DomainTypedValue[DoubleType]

    /**
     * Factory method to create a `DomainValue` that represents the given double value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def DoubleValue(origin: ValueOrigin, value: Double): DomainTypedValue[DoubleType]
}
