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

    final override def Throwable(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.Throwable)
    }

    final override def ClassCastException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.ClassCastException)
    }

    final override def ClassNotFoundException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.ClassNotFoundException)
    }

    final override def NullPointerException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.NullPointerException)
    }

    final override def IllegalMonitorStateException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.IllegalMonitorStateException)
    }

    final override def NegativeArraySizeException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.NegativeArraySizeException)
    }

    final override def ArrayIndexOutOfBoundsException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.ArrayIndexOutOfBoundsException)
    }

    final override def ArrayStoreException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.ArrayStoreException)
    }

    final override def ArithmeticException(origin: ValueOrigin): ExceptionValue = {
        InitializedObjectValue(origin, ObjectType.ArithmeticException)
    }

}
