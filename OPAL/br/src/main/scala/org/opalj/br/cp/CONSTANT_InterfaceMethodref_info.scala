/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents an interface method.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_InterfaceMethodref_info(
        class_index:         Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index
) extends AsMethodref {

    override def tag: Int = ConstantPoolTags.CONSTANT_InterfaceMethodref_ID

    final def isInterfaceMethodRef: Boolean = true

}
