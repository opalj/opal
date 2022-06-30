/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq

/**
 * @author Michael Eichberg
 */
case class CONSTANT_String_info(string_index: Constant_Pool_Index) extends Constant_Pool_Entry {

    final override def size: Int = 1 + 2

    override def Constant_Type_Value = bi.ConstantPoolTags.CONSTANT_String

    override def asCPNode(implicit cp: Constant_Pool): Node =
        <span class="cp_entry">
            CONSTANT_String_info(string_index={ string_index }
            &laquo;
            <span class="cp_ref">{ cp(string_index).asCPNode }</span>
            &raquo;)
        </span>

    override def toString(implicit cp: Constant_Pool): String = cp(string_index).toString

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        <span class="constant_value">{ "\""+cp(string_index).toString+"\"" }</span>
    }
}
