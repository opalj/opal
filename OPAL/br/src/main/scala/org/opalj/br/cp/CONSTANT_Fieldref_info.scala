/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a field.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_Fieldref_info(
    class_index:         Constant_Pool_Index,
    name_and_type_index: Constant_Pool_Index
) extends Constant_Pool_Entry {

    override def tag: Int = ConstantPoolTags.CONSTANT_Fieldref_ID

    // We don't mind if the field is initialized more than once (if reading the classfile
    // should be parallelized) as it is just an optimization and the object reference
    // is of no importance; an equals check will return true. Hence, w.r.t. the
    // previous definition this code is thread-safe.
    private var fieldref: (ClassType, String, FieldType) = _ // to cache the result
    override def asFieldref(cp: Constant_Pool): (ClassType, String, FieldType) = {
        var fieldref = this.fieldref
        if (fieldref eq null) {
            val nameAndType = cp(name_and_type_index).asNameAndType
            fieldref =
                (
                    cp(class_index).asClassType(cp),
                    nameAndType.name(cp),
                    nameAndType.fieldType(cp)
                )
            this.fieldref = fieldref
        }
        fieldref
    }
}
