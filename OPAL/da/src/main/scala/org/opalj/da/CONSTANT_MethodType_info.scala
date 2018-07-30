/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq
import org.opalj.bi.ConstantPoolTag
import org.opalj.bi.ConstantPoolTags

/**
 *
 * @author Michael Eichberg
 */
case class CONSTANT_MethodType_info(
        descriptor_index: Constant_Pool_Index
) extends Constant_Pool_Entry {

    final override def size: Int = 1 + 2

    override def Constant_Type_Value: ConstantPoolTag = ConstantPoolTags.CONSTANT_MethodType

    override def asCPNode(implicit cp: Constant_Pool): Node =
        <span class="cp_entry">
            Constant_Type_Value({ descriptor_index }
            /*
            <span class="cp_ref">{ cp(descriptor_index).asCPNode }</span>
            */)
        </span>

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        <span>MethodType({ methodDescriptorAsInlineNode("", cp(descriptor_index).toString, None) })</span>
    }

    override def toString(implicit cp: Constant_Pool): String = {
        s"CONSTANT_MethodType_info($descriptor_index)"
    }

}
