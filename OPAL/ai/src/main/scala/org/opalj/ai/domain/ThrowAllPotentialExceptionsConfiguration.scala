/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * A configuration that forces abstract interpretation to always create an exception
 * if it is not possible to deduce that a specific exception will not be thrown.
 *
 * ==Usage==
 * If you need to adapt a setting just override the respective method in your domain
 * or create a class that inherits from [[Configuration]].
 *
 * ==Core Properties==
 *  - Concrete base implementation of the [[Configuration]] trait that can
 *    be used to create a final domain.
 *  - Thread safe.
 *
 * @author Michael Eichberg
 */
trait ThrowAllPotentialExceptionsConfiguration extends Configuration {

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnThrow: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwClassCastException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    def abortProcessingExceptionsOfCalledMethodsOnUnknownException: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    def abortProcessingThrownExceptionsOnUnknownException: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `ExceptionsRaisedByCalledMethods.Any`
     */
    /*implements(not override!)*/ def throwExceptionsOnMethodCall: ExceptionsRaisedByCalledMethod = {
        ExceptionsRaisedByCalledMethods.Any
    }

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnMethodCall: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnFieldAccess: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwArithmeticExceptions: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwIllegalMonitorStateException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnMonitorAccess: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnArrayAccess: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwArrayIndexOutOfBoundsException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwArrayStoreException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNegativeArraySizeException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwClassNotFoundException: Boolean = true

}
