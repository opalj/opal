/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.opalj.br.FieldType
import org.opalj.br.ObjectType

/**
 * Default implementation of the `AsDomainValue` trait.
 *
 * @author Frederik Buss-Joraschek
 * @author Michael Eichberg
 */
trait DefaultJavaObjectToDomainValueConversion extends AsDomainValue {
    domain: ReferenceValuesDomain =>

    /**
     * Converts the given Java object to a corresponding `DomainValue` by creating an `DomainValue`
     * that represents an initialized (array/object) value.
     */
    override def toDomainValue(pc: Int, value: Object): DomainReferenceValue = {
        if (value eq null)
            return NullValue(pc);

        val clazz = value.getClass
        val fqnInBinaryNotation = clazz.getName.replace('.', '/')
        if (clazz.isArray) {
            val arrayType = FieldType(fqnInBinaryNotation).asArrayType
            val array: Array[_] = value.asInstanceOf[Array[_]]
            this match {
                case rv: ArrayValues =>
                    val domainValue = rv.InitializedArrayValue(pc, arrayType, array.length)
                    domainValue.asInstanceOf[domain.DomainReferenceValue]
                case _ => ReferenceValue(pc, arrayType)
            }
        } else /*if (!clazz.isPrimitive()) */ {
            InitializedObjectValue(pc, ObjectType(fqnInBinaryNotation))
        }
    }
}
