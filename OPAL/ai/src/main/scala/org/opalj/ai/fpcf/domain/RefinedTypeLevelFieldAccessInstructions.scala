/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package domain

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.PropertyBounds
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.l0.TypeLevelFieldAccessInstructions
import org.opalj.ai.fpcf.domain.PropertyStoreBased
import org.opalj.ai.fpcf.properties.FieldValue

/**
 * Queries the project information to identify fields with refined field type information.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait RefinedTypeLevelFieldAccessInstructions
    extends TypeLevelFieldAccessInstructions
        with PropertyStoreBased {
    domain: ReferenceValuesDomain with ValuesFactory with Configuration with TheProject ⇒


    abstract override def usesPropertyBounds: Set[PropertyBounds] = {
        super.usesPropertyBounds ++ Set(PropertyBounds.lb(FieldValue))
    }

    var dependees : Set[EOptionP[]] = Set.empty

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
