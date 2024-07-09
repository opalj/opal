/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq

import org.opalj.bi.ConstantPoolTag

/**
 * @author Michael Eichberg
 */
case class CONSTANT_Double_info(value: Double) extends Constant_Pool_Entry {

    override final def size: Int = { 1 + 4 + 4 }

    override def Constant_Type_Value: ConstantPoolTag = bi.ConstantPoolTags.CONSTANT_Double

    override def asCPNode(implicit cp: Constant_Pool): Node =
        <span class="cp_entry">
            CONSTANT_Double_info(
            <span class="constant_value">{value}d</span>
            )
        </span>

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq =
        <span class="contant_value">{value.toString + 'd'}</span>

    override def toString(implicit cp: Constant_Pool): String = value.toString

}
