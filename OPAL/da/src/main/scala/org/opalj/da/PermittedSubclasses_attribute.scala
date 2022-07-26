/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.da

import scala.xml.Node
import scala.xml.NodeBuffer

/**
 * Represents the ''PermittedSubclasses'' attribute (Java 17).
 *
 * @author Julius Naeumann
 */
case class PermittedSubclasses_attribute(
        attribute_name_index: Constant_Pool_Index,
        permitted_subclasses: ClassesArray
) extends Attribute {

    override def attribute_length: Int = 2 + permitted_subclasses.size * 2

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="attribute">
            <summary>PermittedSubclasses</summary>
            {
                permitted_subclasses.map[String](p => cp(p).toString).sorted.map[NodeBuffer] { p =>
                    <span>{ p }</span><br/>
                }
            }
        </details>
    }

}
