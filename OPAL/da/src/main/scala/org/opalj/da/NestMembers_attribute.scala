/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeBuffer

/**
 * Represents the ''NestMembers'' attribute (Java 11).
 *
 * @author Dominik Helm
 */
case class NestMembers_attribute(
        attribute_name_index: Constant_Pool_Index,
        classes_array:        ClassesArray // Array[Constant_Pool_Index]
) extends Attribute {

    override def attribute_length: Int = 2 + classes_array.size * 2

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="attribute">
            <summary>NestMembers</summary>
            {
                classes_array.map[String](p => cp(p).toString).sorted.map[NodeBuffer] { p =>
                    <span>{ p }</span><br/>
                }
            }
        </details>
    }

}
