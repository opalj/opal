/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.br.FieldType
import org.opalj.br.ObjectType

/**
 * Implements the handling of field access instructions at the type level.
 *
 * (Linkage related exceptions are currently generally ignored.)
 *
 * @author Michael Eichberg
 */
trait TypeLevelFieldAccessInstructions extends FieldAccessesDomain {
    domain: ReferenceValuesDomain with TypedValuesFactory with Configuration =>

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // FIELD READ ACCESS
    //

    /*override*/ def getfield(
        pc:             Int,
        objectref:      DomainValue,
        declaringClass: ObjectType,
        fieldName:      String,
        fieldType:      FieldType
    ): Computation[DomainValue, ExceptionValue] = {
        doGetfield(pc, objectref, TypedValue(pc, fieldType))
    }

    /*override*/ def doGetfield(
        pc:         Int,
        objectref:  DomainValue,
        fieldValue: DomainValue
    ): Computation[DomainValue, ExceptionValue] = {

        refIsNull(pc, objectref) match {
            case Yes => throws(VMNullPointerException(pc))
            case Unknown if throwNullPointerExceptionOnFieldAccess =>
                ComputedValueOrException(fieldValue, VMNullPointerException(pc))
            case _ => ComputedValue(fieldValue)
        }
    }

    /*override*/ def getstatic(
        pc:             Int,
        declaringClass: ObjectType,
        fieldName:      String,
        fieldType:      FieldType
    ): Computation[DomainValue, Nothing] = {
        doGetstatic(pc, TypedValue(pc, fieldType))
    }

    /*override*/ def doGetstatic(
        pc:         Int,
        fieldValue: DomainValue
    ): Computation[DomainValue, Nothing] = {
        ComputedValue(fieldValue)
    }

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // FIELD WRITE ACCESS
    //

    /*override*/ def putfield(
        pc:             Int,
        objectref:      DomainValue,
        value:          DomainValue,
        declaringClass: ObjectType,
        fieldName:      String,
        fieldType:      FieldType
    ): Computation[Nothing, ExceptionValue] = {
        refIsNull(pc, objectref) match {
            case Yes => throws(VMNullPointerException(pc))
            case Unknown if throwNullPointerExceptionOnFieldAccess =>
                ComputationWithSideEffectOrException(VMNullPointerException(pc))
            case _ => ComputationWithSideEffectOnly
        }
    }

    /*override*/ def putstatic(
        pc:             Int,
        value:          DomainValue,
        declaringClass: ObjectType,
        fieldName:      String,
        fieldType:      FieldType
    ): Computation[Nothing, Nothing] = {
        ComputationWithSideEffectOnly
    }

}
