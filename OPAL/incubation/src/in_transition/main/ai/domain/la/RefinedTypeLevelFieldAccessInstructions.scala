/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package la

import org.opalj.br.ObjectType
import org.opalj.br.FieldType
import org.opalj.ai.domain.TheProject
import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.ai.domain.l0.TypeLevelFieldAccessInstructions

/**
 * Queries the project information to identify fields with refined field type information.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait RefinedTypeLevelFieldAccessInstructions extends TypeLevelFieldAccessInstructions {
    domain: ReferenceValuesDomain with ValuesFactory with Configuration with TheProject ⇒

    val fieldValueInformation: FieldValueInformation

    override def getfield(
        pc:             PC,
        objectref:      DomainValue,
        declaringClass: ObjectType,
        fieldName:      String,
        fieldType:      FieldType
    ): Computation[DomainValue, ExceptionValue] = {

        val field = project.resolveFieldReference(declaringClass, fieldName, fieldType)
        if (field.isDefined) {
            val fieldValue = fieldValueInformation.get(field.get)
            if (fieldValue.isDefined) {
                return doGetfield(pc, objectref, fieldValue.get.adapt(domain, pc))
            }
        }

        // fallback
        super.getfield(pc, objectref, declaringClass, fieldName, fieldType)
    }

    /**
     * Returns the field's value.
     */
    override def getstatic(
        pc:             PC,
        declaringClass: ObjectType,
        fieldName:      String,
        fieldType:      FieldType
    ): Computation[DomainValue, Nothing] = {
        val field = project.resolveFieldReference(declaringClass, fieldName, fieldType)
        if (field.isDefined) {
            val fieldValue = fieldValueInformation.get(field.get)
            if (fieldValue.isDefined) {
                return doGetstatic(pc, fieldValue.get.adapt(domain, pc))
            }
        }

        super.getstatic(pc, declaringClass, fieldName, fieldType)
    }

}
