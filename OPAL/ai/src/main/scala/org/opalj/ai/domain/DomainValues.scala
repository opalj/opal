/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Encapsulates a `domain` and some values created by the respective domain.
 *
 * Using the class `DomainValues` enables type-safety when we need to store and
 * pass on a `Domain` object and some of its values.
 *
 * @author Michael Eichberg
 */
sealed abstract class DomainValues {
    val domain: Domain
    val values: Iterable[domain.DomainValue]
}

/**
 * Factory for creating `DomainValues` objects.
 *
 * @author Michael Eichberg
 */
object DomainValues {

    def apply(
        theDomain: Domain
    )(
        domainValues: Iterable[theDomain.DomainValue]
    ): DomainValues { val domain: theDomain.type } = {

        new DomainValues {
            val domain: theDomain.type = theDomain
            val values: Iterable[domain.DomainValue] = domainValues
        }
    }

}
