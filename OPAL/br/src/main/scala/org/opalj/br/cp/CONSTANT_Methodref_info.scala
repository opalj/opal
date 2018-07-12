/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a method.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_Methodref_info(
        class_index:         Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index
) extends AsMethodref {

    final override def tag: Int = ConstantPoolTags.CONSTANT_Methodref_ID

    final override def isInterfaceMethodRef: Boolean = false

}