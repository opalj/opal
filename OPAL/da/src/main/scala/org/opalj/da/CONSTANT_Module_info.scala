/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.da

import scala.xml.Node
import scala.xml.NodeSeq

import org.opalj.bi.ConstantPoolTag
import org.opalj.bi.ConstantPoolTags

/**
 * @param name_index Reference to a CONSTANT_Utf8_info structure encoding the name of a module.
 * @author Michael Eichberg
 */
case class CONSTANT_Module_info(name_index: Constant_Pool_Index) extends Constant_Pool_Entry {

    override def size: Int = 1 + 2

    override def Constant_Type_Value: ConstantPoolTag = ConstantPoolTags.CONSTANT_Module

    override def asCPNode(implicit cp: Constant_Pool): Node = {
        <span class="cp_entry">
            CONSTANT_Module_info(name_index=
            { name_index }
            &laquo;
            <span class="cp_ref">
                { cp(name_index).asCPNode }
            </span>
            &raquo;
            )
        </span>
    }

    override def toString(implicit cp: Constant_Pool): String = cp(name_index).toString(cp)

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        throw new UnsupportedOperationException("unexpected usage in combination with instructions")
    }

}
