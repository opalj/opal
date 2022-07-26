/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Defines a factory method to create `IntegerRange` values.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
trait IntegerRangeValuesFactory extends IntegerValuesFactory { domain =>

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or implicitly)
     * by the instruction with the specified program counter and represents
     * an integer value runtime value which is known to be in the range
     * `[lowerBound,upperBound]`.
     */
    def IntegerRange(origin: ValueOrigin, lowerBound: Int, upperBound: Int): DomainValue

}
