/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.FieldsReader

import scala.reflect.ClassTag

/**
 *
 * @author Michael Eichberg
 */
trait FieldsBinding extends FieldsReader { this: ConstantPoolBinding with AttributeBinding =>

    type Field_Info = br.Field
    override implicit val fieldInfoType: ClassTag[Field_Info] = ClassTag(classOf[br.Field])

    def Field_Info(
        cp:               Constant_Pool,
        access_flags:     Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes
    ): Field_Info = {
        Field.unattached(
            access_flags,
            cp(name_index).asString.intern(),
            cp(descriptor_index).asFieldType,
            attributes
        )
    }
}
