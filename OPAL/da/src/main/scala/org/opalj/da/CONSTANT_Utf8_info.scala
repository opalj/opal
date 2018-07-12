/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import java.io.DataOutputStream
import java.io.ByteArrayOutputStream

import scala.xml.Node
import scala.xml.NodeSeq

/**
 * @author Michael Eichberg
 */
case class CONSTANT_Utf8_info(raw: Array[Byte], value: String) extends Constant_Pool_Entry {

    final override def size: Int = {
        // The length of the string in bytes is not equivalent to `value.length` due to the
        // usage of the modified UTF8 enconding.
        1 /* tag */ + 2 /* the length */ + raw.length /* the bytes of the string */
    }

    override def Constant_Type_Value = bi.ConstantPoolTags.CONSTANT_Utf8

    override def asConstantUTF8: this.type = this

    override def asString = value

    override def asCPNode(implicit cp: Constant_Pool): Node = {
        <span class="cp_entry">CONSTANT_Utf8_info("<span class="constant_value">{ value }</span>")</span>
    }

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        throw new UnsupportedOperationException
    }

    override def toString(implicit cp: Constant_Pool): String = value
}

object CONSTANT_Utf8 {

    def apply(value: String): CONSTANT_Utf8_info = {
        new CONSTANT_Utf8_info(
            {
                val bout = new ByteArrayOutputStream(value.length + 2)
                val dout = new DataOutputStream(bout)
                dout.writeUTF(value)
                dout.flush()
                bout.toByteArray()
            },
            value
        )
    }
}
