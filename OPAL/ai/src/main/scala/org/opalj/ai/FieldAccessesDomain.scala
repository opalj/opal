/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.ObjectType
import org.opalj.br.FieldType

/**
 * Interface related to the handling of field access instructions.
 *
 * @author Michael Eichberg
 */
trait FieldAccessesDomain { this: ValuesDomain =>

    /**
     * Returns the field's value and/or a new `NullPointerException` if the given
     * `objectref` represents the value `null`.
     *
     * @return The field's value or a new `NullPointerException`.
     */
    def getfield(
        pc:             Int,
        objectref:      DomainValue,
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
    ): Computation[DomainValue, ExceptionValue]

    /**
     * Returns the field's value.
     */
    def getstatic(
        pc:             Int,
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
    ): Computation[DomainValue, Nothing]

    /**
     * Sets the field's value if the given `objectref` is not `null`(in the [[Domain]]).
     * In the latter case a `NullPointerException` is thrown.
     */
    def putfield(
        pc:             Int,
        objectref:      DomainValue,
        value:          DomainValue,
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
    ): Computation[Nothing, ExceptionValue]

    /**
     * Sets the field's value.
     */
    def putstatic(
        pc:             Int,
        value:          DomainValue,
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
    ): Computation[Nothing, Nothing]

}
