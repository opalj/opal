/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a constant object of the type String.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_String_info(
        string_index: Constant_Pool_Index
) extends CONSTANT_FieldValue_info {

    override def tag: Int = ConstantPoolTags.CONSTANT_String_ID

    override def asConstantFieldValue(cp: Constant_Pool): ConstantString = {
        ConstantString(cp(string_index).asString)
    }

    override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = asConstantValue(cp)

}