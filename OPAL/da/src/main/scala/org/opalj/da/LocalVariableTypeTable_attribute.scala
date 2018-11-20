/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class LocalVariableTypeTable_attribute(
        attribute_name_index:      Constant_Pool_Index,
        local_variable_type_table: Seq[LocalVariableTypeTableEntry]
) extends Attribute {

    final override def attribute_length: Int = 2 + (local_variable_type_table.size * 10)

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details>
            <summary class="attribute_name">LocalVariableTypeTable [size: { local_variable_type_table.size } item(s)]</summary>
            { local_variable_type_table.map(_.toXHTML) }
        </details>
    }

}
