/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.NodeSeq

import org.opalj.bi.ConstantPoolTag

/**
 *
 * @author Michael Eichberg
 */
case class CONSTANT_InterfaceMethodref_info(
        class_index:         Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index
) extends CONSTANT_Ref {

    override def Constant_Type_Value: ConstantPoolTag = {
        bi.ConstantPoolTags.CONSTANT_InterfaceMethodref
    }

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        super.asInstructionParameter(Some("interface"))
    }

}
