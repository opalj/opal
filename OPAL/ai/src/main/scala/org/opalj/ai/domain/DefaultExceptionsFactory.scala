/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.ObjectType

/**
 * Default implementation of the [[ExceptionsFactory]] trait that relies
 * on the [[ReferenceValuesFactory]].
 *
 * @author Michael Eichberg
 */
trait DefaultExceptionsFactory extends ExceptionsFactory {
    this: ValuesDomain with ReferenceValuesFactory =>

    override final def Throwable(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.Throwable)
    }

    override final def ClassCastException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.ClassCastException)
    }

    override final def ClassNotFoundException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.ClassNotFoundException)
    }

    override final def NullPointerException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.NullPointerException)
    }

    override final def IllegalMonitorStateException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.IllegalMonitorStateException)
    }

    override final def NegativeArraySizeException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.NegativeArraySizeException)
    }

    override final def ArrayIndexOutOfBoundsException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.ArrayIndexOutOfBoundsException)
    }

    override final def ArrayStoreException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.ArrayStoreException)
    }

    override final def ArithmeticException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.ArithmeticException)
    }

}
