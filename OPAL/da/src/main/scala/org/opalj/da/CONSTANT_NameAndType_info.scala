/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq
import org.opalj.bi.ConstantPoolTag

import scala.xml.Text

/**
 *
 * @author Michael Eichberg
 */
case class CONSTANT_NameAndType_info(
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index
) extends Constant_Pool_Entry {

    final override def size: Int = 1 + 2 + 2

    override def Constant_Type_Value: ConstantPoolTag = bi.ConstantPoolTags.CONSTANT_NameAndType

    override def asCPNode(implicit cp: Constant_Pool): Node =
        <div class="cp_entry">
            { this.getClass().getSimpleName }
            (
            <div class="cp_ref">
                name_index={ name_index }
                &laquo;
                { cp(name_index).asCPNode }
                &raquo;
            </div>
            <div class="cp_ref">
                descriptor_index={ descriptor_index }
                &laquo;
                { cp(descriptor_index).asCPNode }
                &raquo;
            </div>
            )
        </div>

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        val descriptor = cp(descriptor_index).toString(cp)
        if (descriptor.charAt(0) != '(') {
            Seq(
                parseFieldType(cp(descriptor_index).asString).asSpan(""),
                Text(" "),
                <span class="name">{ cp(name_index).toString(cp) } </span>
            )
        } else {
            val name = cp(name_index).asString
            val descriptor = cp(descriptor_index).asString
            methodDescriptorAsInlineNode(name, descriptor, None)
        }
    }

    override def toString(implicit cp: Constant_Pool): String = {
        val descriptor = cp(descriptor_index).toString(cp)
        if (descriptor.charAt(0) != '(')
            parseFieldType(cp(descriptor_index).asString).asJava+" "+cp(name_index).toString(cp)
        else {
            val methodName = cp(name_index).asString
            var index = 1 // we are not interested in the leading '('
            var parameterTypes: IndexedSeq[String] = IndexedSeq.empty
            while (descriptor.charAt(index) != ')') {
                val (ft, nextIndex) = parseParameterType(descriptor, index)
                parameterTypes = parameterTypes :+ ft.asJava
                index = nextIndex
            }
            val returnType = parseReturnType(descriptor.substring(index + 1)).asJava

            parameterTypes.mkString(s"$returnType $methodName(", ", ", ")")
        }
    }

}
