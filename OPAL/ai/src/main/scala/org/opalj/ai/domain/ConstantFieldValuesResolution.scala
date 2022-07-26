/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.FieldType
import org.opalj.br.ObjectType

/**
 * Resolves references to final static fields that have simple constant values.
 *
 * @note '''A typical Java compiler automatically resolves all simple references
 *      and, hence, this trait has for Java projects in general no effect.''' If we analyze
 *      other languages that compile to the JVM platform, the effect might be different.
 *
 * @author Michael Eichberg
 */
trait ConstantFieldValuesResolution extends Domain { domain: TheProject =>

    abstract override def getstatic(
        pc:        Int,
        classType: ObjectType,
        fieldName: String,
        fieldType: FieldType
    ): Computation[DomainValue, Nothing] = {

        project.resolveFieldReference(classType, fieldName, fieldType) match {
            case Some(field) if field.isFinal && field.isStatic &&
                (field.fieldType.isBaseType || (field.fieldType eq ObjectType.String)) =>
                field.constantFieldValue.map(cv =>
                    ComputedValue(ConstantFieldValue(pc, cv))).getOrElse(
                    super.getstatic(pc, classType, fieldName, fieldType)
                )

            case _ =>
                super.getstatic(pc, classType, fieldName, fieldType)
        }
    }
}
