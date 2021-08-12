/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Trait that can be mixed in if information is needed about all returned values and
 * the thrown exceptions. ''This information is, however, only available after the
 * evaluation of a method has completed.''
 *
 * @author Michael Eichberg
 */
trait MethodCallResults { domain: ValuesDomain =>

    /**
     * `true` if the method returned due to a `("void"|a|i|l|f|d)return` instruction.
     *
     * @note This method may only be called after the abstract interpretation of a
     *       method has completed.
     */
    def returnedNormally: Boolean

    /**
     * Adapts and returns the returned value.
     *
     * @note This method is only defined if the method returned normally. In this case
     *      `None` is returned if the method's return type is `void`;
     *      `Some(DomainValue)` is returned otherwise.
     *
     * @note This method may only be called after the abstract interpretation of a
     *       method has completed.
     */
    def returnedValue(target: TargetDomain, callerPC: Int): Option[target.DomainValue]

    /**
     * Maps the returned value back to the original operand value if possible.
     *
     * @note This method is only defined if the method returned normally. In this case
     *      `None` is returned if the method's return type is `void`;
     *      `Some(DomainValue)` is returned otherwise.
     *
     * @note This method may only be called after the abstract interpretation of a
     *       method has completed.
     */
    def returnedValueRemapped(
        callerDomain: TargetDomain,
        callerPC:     Int
    )(
        originalOperands: callerDomain.Operands,
        passedParameters: Locals
    ): Option[callerDomain.DomainValue]

    /**
     * Adapts and returns the exceptions that are thrown by the called method.
     *
     * In general, for each type of exception there should be at most one
     * `ExceptionValue`.
     *
     * @note This method may only be called after the abstract interpretation of a
     *       method has completed.
     */
    def thrownExceptions(target: TargetDomain, callerPC: Int): target.ExceptionValues

}

