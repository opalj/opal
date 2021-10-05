/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Generic infrastructure to record the value returned by the method (calculated
 * across all return instructions)
 *
 * ==Usage==
 * This domain can be stacked on top of other traits that handle
 * return instructions that return some value.
 *
 * ==Usage==
 * A domain that mixes in this trait should only be used to analyze a single method.
 *
 * @author Michael Eichberg
 */
trait RecordReturnedValueInfrastructure extends RecordReturnedValuesInfrastructure {
    domain: ValuesDomain with Configuration with ExceptionsFactory =>

    /**
     *  A method that always throws an exception or returns "void" will never return a value.
     */
    def returnedValue: Option[DomainValue] // = Option(theReturnedValue)

}

