/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Text
import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class LocalVariableTable_attribute(
        attribute_name_index: Constant_Pool_Index,
        local_variable_table: Seq[LocalVariableTableEntry]
) extends Attribute {

    def attribute_length: Int = 2 + (local_variable_table.size * 10)

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details>
            <summary class="attribute_name">LocalVariableTable [size: { local_variable_table.size } item(s)]</summary>
            {
                if (local_variable_table.nonEmpty)
                    local_variable_table.map(_.toXHTML(cp))
                else
                    Text("<Empty>")
            }
        </details>
    }
}
