/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.Text
import org.opalj.bi.ConstantPoolTag

import scala.xml.NodeSeq

/**
 *
 * @author Michael Eichberg
 */
case class CONSTANT_Integer_info(value: Int) extends Constant_Pool_Entry {

    final override def size: Int = 1 + 4

    override def Constant_Type_Value: ConstantPoolTag = bi.ConstantPoolTags.CONSTANT_Integer

    override def asCPNode(implicit cp: Constant_Pool): Node =
        <span class="cp_entry">
            CONSTANT_Integer_info(
            <span class="constant_value">{ value }</span>
            )
        </span>

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        val repr =
            if (value < 0 || value >= 10) {
                var additionalInfo = " = 0x"+value.toHexString
                if (value == Int.MinValue)
                    additionalInfo += " = Int.Min"
                else if (value == Int.MaxValue)
                    additionalInfo += " = Int.Max"
                Seq(
                    Text(value.toString),
                    <span class="comment">{ additionalInfo }</span>
                )
            } else {
                Seq(Text(value.toString))
            }

        <span class="constant_value">{ repr }</span>
    }

    override def toString(implicit cp: Constant_Pool): String = {
        if (value < 0 || value >= 10) {
            var r = s"$value (= 0x${value.toHexString}"
            if (value == Int.MinValue)
                r += " = Int.Min"
            else if (value == Int.MaxValue)
                r += " = Int.Max"
            r+")"
        } else {
            value.toString
        }
    }
}
