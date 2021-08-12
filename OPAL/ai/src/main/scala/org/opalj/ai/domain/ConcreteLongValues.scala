/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Provides information about a long value's precise value, if this information
 * is available.
 *
 * @note This abstract interpreter never requires knowledge about the precise
 *      value of a value with computational type `long`.
 *
 * @author Michael Eichberg
 */
trait ConcreteLongValues { this: ValuesDomain =>

    /**
     * If the given value represents one specific long value then the function
     * `ifThen` is called with the respective value otherwise `orElse` is called.
     */
    def longValue[T](value: DomainValue)(ifThen: Long => T)(orElse: => T): T

    /**
     * Returns the current `Long` value represented by the domain value if it exists.
     *
     * @note This method returns `None` if the DomainValue does not represent a
     *      Long value or the precise value is not known. I.e., this method never fails.
     */
    def longValueOption(value: DomainValue): Option[Long]

    object ConcreteLongValue {
        def unapply(value: DomainValue): Option[Long] = longValueOption(value)
    }

}
