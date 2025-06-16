/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.ClassType

/**
 * Default implementation of the [[ExceptionsFactory]] trait that relies
 * on the [[ReferenceValuesFactory]].
 *
 * @author Michael Eichberg
 */
trait DefaultExceptionsFactory extends ExceptionsFactory {
    this: ValuesDomain with ReferenceValuesFactory =>

    override final def Throwable(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ClassType.Throwable)
    }

    override final def ClassCastException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ClassType.ClassCastException)
    }

    override final def ClassNotFoundException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ClassType.ClassNotFoundException)
    }

    override final def NullPointerException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ClassType.NullPointerException)
    }

    override final def IllegalMonitorStateException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ClassType.IllegalMonitorStateException)
    }

    override final def NegativeArraySizeException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ClassType.NegativeArraySizeException)
    }

    override final def ArrayIndexOutOfBoundsException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ClassType.ArrayIndexOutOfBoundsException)
    }

    override final def ArrayStoreException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ClassType.ArrayStoreException)
    }

    override final def ArithmeticException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ClassType.ArithmeticException)
    }

}
