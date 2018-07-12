/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents the name of a module.
 *
 * @author Michael Eichberg
 */
case class CONSTANT_Module_info(name_index: Constant_Pool_Index) extends Constant_Pool_Entry {

    override def tag: Int = ConstantPoolTags.CONSTANT_Module_ID

    override def asModuleIdentifier(cp: Constant_Pool): String = cp(name_index).asString

}
