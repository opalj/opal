/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Encapsulates the result of a computation in a domain. In general, the
 * result is either some value `V` or some exception(s) `E`. In some cases, however,
 * when the domain cannot ''precisely'' determine the result, it may be both: some
 * exceptional value(s) and a value.
 *
 * In the latter case the abstract interpreter will generally follow all
 * possible paths. A computation that declares to return a result
 * (i.e., the type `V` is not `Nothing`) must not return a result and/or throw an
 * exception if the computation did not finish.
 *
 * ==Querying Computations==
 * Before accessing a computation's result ([[result]] or [[exceptions]]) it first
 * has to be checked whether the computation returned normally ([[returnsNormally]])
 * or threw an exception ([[throwsException]]). Only if `returnsNormally` returns
 * `true` the methods `result` and `hasResult` are defined.
 *
 * @tparam V The result of the computation. Typically a `DomainValue`;
 *      if the computation is executed for its side
 *      effect (e.g., as in case of a `monitorenter` or `monitorexit` instruction)
 *      the type of `V` maybe `Nothing`.
 * @tparam E The exception(s) that maybe thrown by the computation. Typically,
 *      a `DomainValue` which represents a reference value with type
 *      `java.lang.Throwable` or a subtype thereof. If multiple exceptions may be
 *      thrown it may also be a set or `Iterable` of `DomainValue`s (e.g.,
 *      `ExceptionValues`).
 *
 * @note The precise requirements on the result of a computation are determined
 *      by the [[Domain]] object's methods that perform computations.
 *
 * @author Michael Eichberg
 */
sealed abstract class Computation[+V, +E] {

    /**
     * Returns `true` if this computation ''may have returned normally'' without
     * throwing an exception. Given that some computations are performed for their
     * side effect only, the computation may not have a result.
     */
    def returnsNormally: Boolean

    /**
     * Returns `true` if this computation has a result value, `false` otherwise.
     *
     * @note A method with return type `void` may return normally ([[returnsNormally]]),
     *      but will never have a result. I.e., for such method, `hasResult` will always
     *      be false.
     */
    def hasResult: Boolean

    /**
     * The return value of the computation (if any); defined if and only if
     * `hasResult` returns true.
     */
    @throws[UnsupportedOperationException]
    def result: V

    /**
     * Returns `true` if this computation ''may have raised an exception''.
     */
    def throwsException: Boolean

    /**
     * The exception or exceptions when the computation raised an exception;
     * defined if and only if `throwsException` returns `true`.
     *
     * E.g., the invocation of a method may lead to several (checked/unchecked) exceptions.
     */
    @throws[UnsupportedOperationException]
    def exceptions: E

    /**
     * Updates the result associated with the represented computation.
     *
     * This method is only supported if the computation had a result!
     */
    @throws[UnsupportedOperationException]
    def updateResult[X](result: X): Computation[X, E]

    /**
     * Updates the exception associated with the represented computation.
     *
     * This method is only supported if the computation had an associated exception!
     */
    @throws[UnsupportedOperationException]
    def updateExceptions[X](exceptions: X): Computation[V, X]
}

/**
 * Encapsulates the result of a computation that returned normally and
 * that did not throw an exception.
 */
final case class ComputedValue[+V](result: V) extends Computation[V, Nothing] {

    def returnsNormally: Boolean = true

    def hasResult: Boolean = true

    def updateResult[X](r: X): ComputedValue[X] = ComputedValue(r)

    def throwsException: Boolean = false

    def exceptions: Nothing = throw new UnsupportedOperationException(
        "the computation succeeded without an exception"
    )

    def updateExceptions[X](es: X): ComputedValue[V] = throw new UnsupportedOperationException(
        "the computation succeeded without an exception"
    )

}

/**
 * Encapsulates the result of a computation that either returned normally
 * or threw an exception.
 */
final case class ComputedValueOrException[+V, +E](
        result: V,
        exceptions: E) extends Computation[V, E] {

    def returnsNormally: Boolean = true

    def hasResult: Boolean = true

    def updateResult[X](result: X): ComputedValueOrException[X, E] = ComputedValueOrException(result, exceptions)

    def throwsException: Boolean = true

    def updateExceptions[X](exceptions: X): ComputedValueOrException[V, X] = ComputedValueOrException(result, exceptions)
}

/**
 * Encapsulates the result of a computation that threw an exception.
 */
final case class ThrowsException[+E](exceptions: E) extends Computation[Nothing, E] {

    def returnsNormally: Boolean = false

    def hasResult: Boolean = false

    def result: Nothing = throw new UnsupportedOperationException(
        "the computation resulted in an exception"
    )

    def updateResult[X](result: X): ThrowsException[E] = throw new UnsupportedOperationException(
        "the computation resulted in an exception"
    )

    def throwsException: Boolean = true

    def updateExceptions[X](exceptions: X): ThrowsException[X] = ThrowsException(exceptions)

}

/**
 * Encapsulates the result of a computation that returned normally (but which
 * did not return some value) or that threw an exception/multiple exceptions.
 */
final case class ComputationWithSideEffectOrException[+E](
        exceptions: E) extends Computation[Nothing, E] {

    def returnsNormally: Boolean = true

    def hasResult: Boolean = false

    def updateResult[X](result: X): ComputationWithSideEffectOrException[E] =
        throw new UnsupportedOperationException("the computation only had side effects")

    def result: Nothing = throw new UnsupportedOperationException(
        "the computation was executed for its side effect only"
    )

    def throwsException: Boolean = true

    def updateExceptions[X](es: X): ComputationWithSideEffectOrException[X] = ComputationWithSideEffectOrException(es)
}

/**
 * Represents a computation that completed normally.
 */
case object ComputationWithSideEffectOnly extends Computation[Nothing, Nothing] {

    def returnsNormally: Boolean = true

    def hasResult: Boolean = false

    def result: Nothing = throw new UnsupportedOperationException(
        "the computation was executed for its side effect only"
    )

    def updateResult[X](result: X): ComputationWithSideEffectOnly.type =
        throw new UnsupportedOperationException("the computation only had side effects")

    def throwsException: Boolean = false

    def exceptions: Nothing = throw new UnsupportedOperationException(
        "the computation succeeded without an exception"
    )

    def updateExceptions[X](es: X): ComputationWithSideEffectOnly.type = throw new UnsupportedOperationException(
        "the computation succeeded without an exception"
    )
}

/**
 * Indicates that the computation did not succeed. This is typically the case
 * for methods that contain an endless loop, such as:
 * {{{
 *  while(true){.../* no break statements */}
 * }}}
 */
case object ComputationFailed extends Computation[Nothing, Nothing] {

    def returnsNormally: Boolean = false

    def hasResult: Boolean = false

    def result: Nothing = throw new UnsupportedOperationException("the computation failed")

    def updateResult[X](result: X): ComputationFailed.type =
        throw new UnsupportedOperationException("the computation failed")

    def throwsException: Boolean = false

    def exceptions: Nothing = throw new UnsupportedOperationException("the computation failed")

    def updateExceptions[X](es: X): ComputationFailed.type =
        throw new UnsupportedOperationException("the computation failed")
}

// -------------------------------------------------------------------------------------------------
//
// EXTRACTORS
//
// -------------------------------------------------------------------------------------------------

object ComputationWithResultAndException {

    def unapply[V, E](c: Computation[V, E]): Option[(V, E)] =
        if (c.hasResult && c.throwsException) Some((c.result, c.exceptions)) else None

}

object ComputationWithResult {

    def unapply[V](c: Computation[V, _]): Option[V] = if (c.hasResult) Some(c.result) else None

}

object ComputationWithException {

    def unapply[E](c: Computation[_, E]): Option[E] = if (c.throwsException) Some(c.exceptions) else None
}
