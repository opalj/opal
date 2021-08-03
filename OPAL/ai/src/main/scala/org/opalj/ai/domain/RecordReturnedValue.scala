/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Record the value returned by a method across all return instructions.
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
trait RecordReturnedValue extends RecordReturnedValuesInfrastructure {
    domain: ValuesDomain =>

    protected[this] var theReturnedValue: DomainValue = null

    def returnedValue: Option[DomainValue] = Option(theReturnedValue)

    protected[this] def doRecordReturnedValue(pc: Int, value: DomainValue): Boolean = {
        val oldReturnedValue = theReturnedValue
        if (oldReturnedValue eq value)
            return false;

        if (oldReturnedValue == null) {
            theReturnedValue = value
            true
        } else {
            val joinedValue = oldReturnedValue.join(pc, value)
            if (joinedValue.isSomeUpdate) {
                theReturnedValue = joinedValue.value
                true
            } else {
                false
            }
        }
    }
}

