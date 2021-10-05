/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package properties

import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation
import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassHierarchy
import org.opalj.br.FieldType

sealed trait FieldValueMetaInformation extends PropertyMetaInformation {

    final type Self = FieldValue

}

/**
 * Encapsulates the (intermediate) result of the computation of the properties of the values
 * stored in a field. The value generally abstracts over all values stored in a respective field.
 *
 * @author Michael Eichberg
 */
sealed trait FieldValue extends Property with FieldValueMetaInformation {

    /**
     * Returns the key used by all `FieldValue` properties.
     */
    final def key: PropertyKey[FieldValue] = FieldValue.key

    def value(implicit classHierarchy: ClassHierarchy): ValueInformation
}

case class ValueBasedFieldValueInformation(theValue: ValueInformation) extends FieldValue {
    override def value(implicit classHierarchy: ClassHierarchy): ValueInformation = theValue
}

case class TypeBasedFieldValueInformation(fieldType: FieldType) extends FieldValue {
    override def value(implicit classHierarchy: ClassHierarchy): ValueInformation = {
        ValueInformation.forProperValue(fieldType)
    }
}

object FieldValue extends FieldValueMetaInformation {

    /**
     * The key associated with every [[FieldValue]] property.
     */
    final val key: PropertyKey[FieldValue] = PropertyKey.create[Field, FieldValue](
        "opalj.FieldValue",
        // fallback property computation...
        (ps: PropertyStore, r: FallbackReason, f: Field) => {
            val p = ps.context(classOf[SomeProject])
            val vi = ValueInformation.forProperValue(f.fieldType)(p.classHierarchy)
            ValueBasedFieldValueInformation(vi)
        }
    )
}
