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
    domain: ValuesDomain ⇒

    private[this] var theReturnedValue: DomainValue = null

    def returnedValue: Option[DomainValue] = Option(theReturnedValue)

    protected[this] def doRecordReturnedValue(pc: Int, value: DomainValue): Unit = {
        val oldReturnedValue = theReturnedValue
        if (oldReturnedValue eq value)
            return ;

        if (oldReturnedValue == null) {
            theReturnedValue = value
        } else {
            val joinedValue = oldReturnedValue.join(pc, value)
            if (joinedValue.isSomeUpdate)
                theReturnedValue = joinedValue.value
        }
    }
}

