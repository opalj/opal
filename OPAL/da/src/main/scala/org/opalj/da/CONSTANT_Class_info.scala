/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq

import org.opalj.bi.ConstantPoolTag

/**
 * @param name_index Reference to a CONSTANT_Utf8_info structure.
 * @author Michael Eichberg
 */
case class CONSTANT_Class_info(name_index: Constant_Pool_Index) extends Constant_Pool_Entry {

    override def size: Int = 1 + 2

    override def Constant_Type_Value: ConstantPoolTag = bi.ConstantPoolTags.CONSTANT_Class

    override def asConstantClass: this.type = this

    /**
     * Should be called if and only if the referenced type is known not be an array type and
     * therefore the underlying descriptor does not encode a field type descriptor.
     */
    def asJavaClassOrInterfaceType(implicit cp: Constant_Pool): ObjectTypeInfo = {
        asJavaObjectType(cp(name_index).asConstantUTF8.value)
    }

    override def asCPNode(implicit cp: Constant_Pool): Node = {
        <span class="cp_entry">
            CONSTANT_Class_info(name_index=
            { name_index }
            &laquo;
            <span class="cp_ref">
                { cp(name_index).asCPNode }
            </span>
            &raquo;
            )
        </span>
    }

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        asJavaReferenceType(name_index).asSpan("")
    }

    override def toString(implicit cp: Constant_Pool): String = {
        asJavaReferenceType(name_index).asJava
    }
}
