/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import scala.collection.immutable.LongMap

/**
 * Generic infrastructure to record the exceptions thrown by a method.
 * This trait can be used to record the thrown exceptions independently of the
 * precision of the domain.
 *
 * ==Usage==
 * This domain can be stacked on top of other traits that handle
 * [[abruptMethodExecution]]s.
 *
 * @author Michael Eichberg
 */
trait RecordThrownExceptions extends ai.ReturnInstructionsDomain {
    domain: ValuesDomain =>

    /**
     * This type determines in which way thrown exceptions are recorded.
     *
     * For example, if you want to collect all thrown exceptions, then
     * the type of `ThrownException` could be `Set[ThrownException]`.
     *
     * @see The implementation of [[org.opalj.ai.domain.RecordAllThrownExceptions]].
     * @see The implementation of [[org.opalj.ai.domain.RecordLastReturnedValues]]. It
     *      follows the same pattern.
     */
    type ThrownException <: AnyRef

    /**
     * Wraps the given value into a `ThrownException`.
     *
     * @param pc The program counter of the instruction that throws the exception. It
     *      is automatically stored in the map that associates instructions with
     *      the exceptions that are thrown.
     *
     * @see For details study the documentation of the abstract type `ThrownException`
     *      and study the subclass(es) of `RecordThrownExceptions`.
     */
    protected[this] def recordThrownException(
        pc:    Int,
        value: ExceptionValue
    ): ThrownException

    /**
     * Joins the previously thrown exception(s) and the newly thrown exception. Both
     * exceptions are thrown by the same instruction (same `pc`).
     *
     * @note The instruction might be an `athrow` instruction or some other instruction
     *      that throws an exception.
     *
     * @see For details study the documentation of the abstract type `ThrownException`
     *      and study the subclass(es) of `RecordThrownExceptions`.
     */
    protected[this] def joinThrownExceptions(
        pc:                        Int,
        previouslyThrownException: ThrownException,
        value:                     ExceptionValue
    ): ThrownException

    @volatile private[this] var thrownExceptions: LongMap[ThrownException] = LongMap.empty

    /**
     * Returns all thrown exceptions.
     * The key of the returned map is the pc of the instruction which threw the exception.
     */
    def allThrownExceptions: LongMap[ThrownException] = thrownExceptions

    abstract override def abruptMethodExecution(
        pc:        Int /*PC*/ ,
        exception: ExceptionValue
    ): Unit = {
        val longPC = pc.toLong
        thrownExceptions =
            thrownExceptions.updated(
                longPC,
                thrownExceptions.get(longPC) match {
                    case Some(previouslyThrownException) =>
                        joinThrownExceptions(pc, previouslyThrownException, exception)
                    case None =>
                        recordThrownException(pc, exception)
                }
            )
        super.abruptMethodExecution(pc, exception)
    }
}

