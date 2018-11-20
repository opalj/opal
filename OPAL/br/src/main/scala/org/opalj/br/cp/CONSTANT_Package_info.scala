/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a name of a package in internal form.
 *
 * @author Michael Eichberg
 */
case class CONSTANT_Package_info(name_index: Constant_Pool_Index) extends Constant_Pool_Entry {

    override def tag: Int = ConstantPoolTags.CONSTANT_Package_ID

    override def asPackageIdentifier(cp: Constant_Pool): String = cp(name_index).asString

}
