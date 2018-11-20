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
case class CONSTANT_InvokeDynamic_info(
        bootstrap_method_attr_index: Int,
        name_and_type_index:         Constant_Pool_Index
) extends Constant_Pool_Entry {

    final override def size: Int = 1 + 2 + 2

    override def Constant_Type_Value: ConstantPoolTag = bi.ConstantPoolTags.CONSTANT_InvokeDynamic

    override def asCPNode(implicit cp: Constant_Pool): Node = {
        <div class="cp_entry">
            { this.getClass().getSimpleName }
            (<div class="attributes_ref">
                 bootstrap_method_attr_index={ bootstrap_method_attr_index }
             </div>
            <div class="cp_ref">
                name_and_type_index={ name_and_type_index }
                &laquo;
                { cp(name_and_type_index).asCPNode }
                &raquo;
            </div>
            )
        </div>
    }

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        val ntiNode = cp(name_and_type_index).asInstructionParameter
        val paramsNode =
            Seq(
                ntiNode,
                Text(s" //Bootstrap Method Attribute[$bootstrap_method_attr_index]")
            )
        <span class="cp_entry">{ paramsNode }</span>
    }

    override def toString(implicit cp: Constant_Pool): String = {
        "CONSTANT_InvokeDynamic_info("+
            s"$bootstrap_method_attr_index,"+
            s"${cp(name_and_type_index).toString(cp)}/*$name_and_type_index */ "+
            ")"
    }
}
