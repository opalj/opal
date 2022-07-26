/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.FloatType

/**
 * Defines factory methods to create concrete representations of constant
 * float values.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait FloatValuesFactory extends ValuesDomain { domain =>

    /**
     * Factory method to create a `DomainValue` with the specified origin.
     * The origin is typically the program counter of the instruction
     * that created this value/where the value was observed for the first
     * time.
     *
     * The domain may ignore the information about the origin (`origin`).
     */
    def FloatValue(valueOrigin: Int): DomainTypedValue[FloatType]

    /**
     * Factory method to create a `DomainValue` with the specified origin.
     * The origin is typically the program counter of the instruction
     * that created this value/where the value was observed for the first
     * time.
     *
     * The domain may ignore the information about the origin (`origin`).
     */
    def FloatValue(valueOrigin: Int, value: Float): DomainTypedValue[FloatType]
}

