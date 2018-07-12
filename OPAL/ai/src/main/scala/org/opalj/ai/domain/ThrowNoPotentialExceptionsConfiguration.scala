/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * A configuration that forces the abstract interpretor to never create an exception
 * if it is not possible to deduce that the specific exception is guaranteed to be thrown.
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
trait ThrowNoPotentialExceptionsConfiguration extends Configuration {

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnThrow: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwClassCastException: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    def abortProcessingExceptionsOfCalledMethodsOnUnknownException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    def abortProcessingThrownExceptionsOnUnknownException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `ExceptionsRaisedByCalledMethods.Known`
     */
    /*implements(not override!)*/ def throwExceptionsOnMethodCall: ExceptionsRaisedByCalledMethod = {
        ExceptionsRaisedByCalledMethods.Known
    }

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnMethodCall: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnFieldAccess: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwArithmeticExceptions: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwIllegalMonitorStateException: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnMonitorAccess: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnArrayAccess: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwArrayIndexOutOfBoundsException: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwArrayStoreException: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwNegativeArraySizeException: Boolean = false

    /**
     * @inheritdoc
     *
     * @return `false`
     */
    /*implements(not override!)*/ def throwClassNotFoundException: Boolean = false

}
