/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq

import org.opalj.bi.ConstantPoolTag

/**
 * @author Michael Eichberg
 */
case class CONSTANT_Long_info(value: Long) extends Constant_Pool_Entry {

    final override def size: Int = { 1 + 4 + 4 }

    override def Constant_Type_Value: ConstantPoolTag = bi.ConstantPoolTags.CONSTANT_Long

    override def asCPNode(implicit cp: Constant_Pool): Node =
        <span class="cp_entry">
            CONSTANT_Long_info(
            <span class="constant_value">{ value }l</span>
            )
        </span>

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        <span class="constant_value">{ value.toString+"l" }</span>
    }

    override def toString(implicit cp: Constant_Pool): String = value.toString
}
