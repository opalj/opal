/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Centralizes all configuration options related to how a domain should handle
 * situations in which the information about a value is (often) not completely available and
 * which could lead to some kind of exception.
 *
 * Basically all domains that perform some kind of abstraction should mix in this trait
 * and query the respective method to decide if a respective exception should be thrown
 * if it is possible that an exception may be thrown.
 *
 * ==Usage==
 * If you need to adapt a setting just override the respective method in your domain.
 *
 * In general, the [[org.opalj.ai.domain.ThrowAllPotentialExceptionsConfiguration]]
 * should be used as a foundation as it generates all exceptions that may be thrown; however,
 * configuring the behavior of method calls may be worth while.
 *
 * @author Michael Eichberg
 */
trait Configuration {

    //
    //
    // CONFIGURATION OF THE AI ITSELF
    //
    //

    /**
     * If `true` a `ClassCastException` is thrown by `CHECKCAST` instructions if it
     * cannot be verified that no `ClassCastException` will be thrown.
     *
     * @note Directly used by the [[AI]] itself.
     */
    def throwClassCastException: Boolean

    /**
     * If `true` a VM level `NullPointerExceptions` is thrown if the exception that is to be
     * thrown may be null.
     *
     * @note Directly used by the [[AI]] itself.
     */
    def throwNullPointerExceptionOnThrow: Boolean

    /**
     * If `true` the processing of the exception handlers related to an invoke statement will
     * be aborted if the relation between the type of the thrown exception and the caught type
     * is unknown.
     *
     * @note Directly used by the [[AI]] itself.
     */
    def abortProcessingExceptionsOfCalledMethodsOnUnknownException: Boolean

    /**
     * If `true` the processing of the exception handlers related to an athrow statement will
     * be aborted if the relation between the type of the thrown exception and the caught type
     * is unknown.
     *
     * @note Directly used by the [[AI]] itself.
     */
    def abortProcessingThrownExceptionsOnUnknownException: Boolean

    //
    //
    // DOMAIN SPECIFIC CONFIGURATION
    //
    //

    /**
     * Determines the behavior how method calls are handled when the exceptions that the
     * called method may throw are unknown.
     *
     * @note Used by domains which handle method invokations.
     */
    def throwExceptionsOnMethodCall: ExceptionsRaisedByCalledMethod

    /**
     * Returns `true` if potential `NullPointerExceptions` should be thrown and `false`
     * if such `NullPointerExceptions` should be ignored. However, if the interpreter
     * identifies a situation in which a `NullPointerException` is guaranteed to be
     * thrown, it will be thrown. Example:
     * {{{
     * def demo(o : Object) {
     *      o.toString  // - If "true", a NullPointerException will ALSO be thrown;
     *                  //   the operation also succeeds.
     *                  // - If "false" the operation will "just" succeed
     * }
     * }}}
     *
     * @note Used by domains which handle method invokations.
     */
    def throwNullPointerExceptionOnMethodCall: Boolean

    /**
     * Returns `true` if potential `NullPointerExceptions` should be thrown and `false`
     * if such `NullPointerExceptions` should be ignored. However, if the interpreter
     * identifies a situation in which a `NullPointerException` is guaranteed to be
     * thrown, it will be thrown.
     */
    def throwNullPointerExceptionOnFieldAccess: Boolean

    /**
     * If `true`, all instructions that may raise an arithmetic exception (e.g., ''idiv'',
     * ''ldiv'') should do so if it is impossible to statically determine that no
     * exception will occur. But, if we can statically determine that the operation will
     * raise an exception then the exception will be thrown – independently of this
     * setting. Furthermore, if we can statically determine that no exception will be
     * raised, no exception will be thrown. Hence, this setting only affects computations
     * with values with ''incomplete'' information.
     */
    def throwArithmeticExceptions: Boolean

    /**
     * Returns `true` if potential `NullPointerExceptions` should be thrown and `false`
     * if such `NullPointerExceptions` should be ignored. However, if the interpreter
     * identifies a situation in which a `NullPointerException` is guaranteed to be
     * thrown, it will be thrown.
     */
    def throwNullPointerExceptionOnMonitorAccess: Boolean

    /**
     * If `true` then `monitorexit` and the `(XXX)return` instructions will throw
     * `IllegalMonitorStateException`s unless the analysis is able to determine that
     * the exception is guaranteed not to be raised.
     */
    def throwIllegalMonitorStateException: Boolean

    /**
     * Returns `true` if potential `NullPointerExceptions` should be thrown and `false`
     * if such `NullPointerExceptions` should be ignored. However, if the interpreter
     * identifies a situation in which a `NullPointerException` is guaranteed to be
     * thrown, it will be thrown.
     */
    def throwNullPointerExceptionOnArrayAccess: Boolean

    /**
     * If `true` an `ArrayIndexOutOfBoundsException` is thrown if the index cannot
     * be verified to be valid.
     */
    def throwArrayIndexOutOfBoundsException: Boolean

    /**
     * If `true` an `ArrayStoreException` is thrown if it cannot be verified that the
     * value can be stored in the array.
     */
    def throwArrayStoreException: Boolean

    /**
     * If `true` a `NegativeArraySizeException` is thrown if the index cannot be
     * verified to be positive.
     */
    def throwNegativeArraySizeException: Boolean

    /**
     * Throw a `ClassNotFoundException` if the a specific reference type is not
     * known in the current context. The context is typically a specific `Project`.
     */
    def throwClassNotFoundException: Boolean

}
