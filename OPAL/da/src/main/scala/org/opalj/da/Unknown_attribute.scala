/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.annotation.nowarn

import scala.xml.Node

/**
 * @author Michael Eichberg
 */
case class Unknown_attribute(
    attribute_name_index: Constant_Pool_Index,
    info:                 Array[Byte]
) extends Attribute {

    override final def attribute_length: Int = info.size

    @nowarn("msg=Discarded non-Unit value") // TODO Remove once scala compiler is fixed: https://github.com/scala/bug/issues/12658
    override def toXHTML(implicit cp: Constant_Pool): Node = {
        val attributeName = cp(attribute_name_index).toString
        <div class="simple_attribute">
            <span class="attribute_name">{attributeName}</span>
            {
                if (attribute_length > 0) {
                    <span>({attribute_length}bytes):</span>
                    <br/>
                    <div>{byteArrayToNode(info)}</div>
                }
            }
        </div>
    }
}
