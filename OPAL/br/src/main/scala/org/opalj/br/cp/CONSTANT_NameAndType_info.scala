/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a field or a method without indicating which class or interface it belongs
 * to.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_NameAndType_info(
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index
) extends Constant_Pool_Entry {

    override def asNameAndType: this.type = this

    // this operation is very cheap and hence, it doesn't make sense to cache the result
    def name(cp: Constant_Pool): String = cp(name_index).asString

    def fieldType(cp: Constant_Pool): FieldType = cp(descriptor_index).asFieldType

    def methodDescriptor(cp: Constant_Pool): MethodDescriptor = {
        cp(descriptor_index).asMethodDescriptor
    }

    override def tag: Int = ConstantPoolTags.CONSTANT_NameAndType_ID
}