/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Provides information about an integer value's precise value, if this information
 * is available.
 *
 * @note The information about an integer value's range that is required by the
 *      abstract interpreter is defined by the `Domain` trait.
 *
 * @note This functionality is not required by the OPAL core.
 *
 * @author Michael Eichberg
 */
trait ConcreteIntegerValues { this: ValuesDomain =>

    /**
     * If the given value encapsulates a precise integer value then the function
     * `ifThen` is called with the respective value otherwise `orElse` is called.
     */
    def intValue[T](value: DomainValue)(ifThen: Int => T)(orElse: => T): T

    /**
     * Returns the current `Int` value represented by the domain value if it exists.
     *
     * @note This method returns `None` if the DomainValue does not represent an
     *      Integer value or the precise value is not known. I.e., this method never fails.
     */
    def intValueOption(value: DomainValue): Option[Int]

    object ConcreteIntegerValue {
        def unapply(value: DomainValue): Option[Int] = intValueOption(value)
    }

}
