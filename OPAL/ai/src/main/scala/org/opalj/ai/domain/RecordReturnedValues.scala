/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.Code

/**
 * Generic infrastructure to record the values returned by the method.
 * (Note that the computational type of the value(s) is not recorded.
 * It is directly determined by the signature of the method that is analyzed or
 * can be extracted using the respective method.)
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
trait RecordReturnedValues extends RecordReturnedValuesInfrastructure with CustomInitialization {
    domain: ValuesDomain with Configuration with ExceptionsFactory =>

    /**
     * Wraps the given value into a `ReturnedValue`.
     *
     * @param pc The program counter of the return instruction. The returned
     *      values are automatically associated with the pc of the instruction. Hence,
     *      it is not strictly required to store it in the `ReturnedValue`.
     *
     * @see For details study the documentation of the abstract type `ReturnedValue`
     *      and study the subclass(es) of `RecordReturnedValues`.
     */
    protected[this] def recordReturnedValue(pc: Int, value: DomainValue): ReturnedValue

    /**
     * Joins the previously returned value and the newly given `value`. Both values
     * are returned by the same return instruction (same `pc`).
     *
     * @param pc The program counter of the return instruction. The returned
     *      values are automatically associated with the pc of the instruction. Hence,
     *      it is not strictly required to store it in the `ReturnedValue`.
     *
     * @see For details study the documentation of the abstract type `ReturnedValue`
     *      and study the subclass(es) of `RecordReturnedValues`.
     */
    protected[this] def joinReturnedValues(
        pc:                      Int,
        previouslyReturnedValue: ReturnedValue,
        value:                   DomainValue
    ): ReturnedValue

    private[this] var returnedValues: IntMap[ReturnedValue] = _

    abstract override def initProperties(
        code:          Code,
        cfJoins:       IntTrieSet,
        initialLocals: Locals
    ): Unit = {
        returnedValues = IntMap.empty
        super.initProperties(code, cfJoins, initialLocals)
    }

    /**
     * Returns the set of all returned values.
     */
    def allReturnedValues: IntMap[ReturnedValue] = returnedValues

    protected[this] def doRecordReturnedValue(pc: Int, value: DomainValue): Boolean = {
        returnedValues.get(pc) match {
            case None =>
                returnedValues = returnedValues.updated(pc, recordReturnedValue(pc, value))
                true // <=> isUpdated
            case Some(returnedValue) =>
                val joinedReturnedValue = joinReturnedValues(pc, returnedValue, value)
                if (returnedValue ne joinedReturnedValue) {
                    returnedValues = returnedValues.updated(pc, joinedReturnedValue)
                    true
                } else {
                    false
                }
        }
    }
}
