/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Infrastructure to record returned values.
 *
 * @author Michael Eichberg
 */
trait RecordReturnedValuesInfrastructure extends ai.ReturnInstructionsDomain {
    domain: ValuesDomain =>

    /**
     * This type determines in which way the returned values are recorded.
     *
     * For example, if it is sufficient to just record the last value that was
     * returned by a specific return instruction, then the type could be `DomainValue`
     * and the implementation of `joinReturnedValues(...)` would just return the last
     * given value. Furthermore, `returnedValue` would be the identity function.
     *
     * However, if you have a (more) precise domain you may want to collect all
     * returned values. In this case the type of `ReturnedValue` could be Set[DomainValue].
     */
    type ReturnedValue <: AnyRef

    /**
     * Records the returned value.
     *
     * @return `true` if the information about the returned value was updated.
     */
    protected[this] def doRecordReturnedValue(pc: Int, value: DomainValue): Boolean

    abstract override def areturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        doRecordReturnedValue(pc, value)
        super.areturn(pc, value)
    }

    abstract override def dreturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        doRecordReturnedValue(pc, value)
        super.dreturn(pc, value)
    }

    abstract override def freturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        doRecordReturnedValue(pc, value)
        super.freturn(pc, value)
    }

    abstract override def ireturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        doRecordReturnedValue(pc, value)
        super.ireturn(pc, value)
    }

    abstract override def lreturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        doRecordReturnedValue(pc, value)
        super.lreturn(pc, value)
    }

}

