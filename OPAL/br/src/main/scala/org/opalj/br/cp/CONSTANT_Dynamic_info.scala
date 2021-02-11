/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Is used by the ldc/ldc_w/ldc2_w instructions to specify a bootstrap method, the dynamic
 * invocation name, the argument and return types of the call, and optionally, a
 * sequence of additional constants called static arguments to the bootstrap method.
 *
 * @param bootstrapMethodAttributeIndex This is an index into the bootstrap table.
 *    Since the bootstrap table is a class level attribute it is only possible
 *    to resolve this reference after loading the entire class file (class level
 *    attributes are loaded last).
 *
 * @author Dominik Helm
 */
case class CONSTANT_Dynamic_info(
        bootstrapMethodAttributeIndex: Int,
        nameAndTypeIndex:              Constant_Pool_Index
) extends Constant_Pool_Entry {

    override def isDynamic = true

    override def asDynamic: this.type = this

    override def tag: Int = ConstantPoolTags.CONSTANT_Dynamic_ID

    def name(cp: Constant_Pool): String = cp(nameAndTypeIndex).asNameAndType.name(cp)

    def descriptor(cp: Constant_Pool): FieldType = {
        cp(nameAndTypeIndex).asNameAndType.fieldType(cp)
    }

    override def asConstantValue(cp: Constant_Pool): ConstantValue[_] = {
        new DynamicConstant(name(cp), descriptor(cp), bootstrapMethodAttributeIndex)
    }

    override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument =
        asConstantValue(cp)

}