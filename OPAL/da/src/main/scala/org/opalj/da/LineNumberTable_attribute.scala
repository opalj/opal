/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class LineNumberTable_attribute(
        attribute_name_index: Constant_Pool_Index,
        line_number_table:    Seq[LineNumberTableEntry]
) extends Attribute {

    final override def attribute_length: Int = 2 + line_number_table.size * 4

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details>
            <summary class="attribute_name">LineNumberTable [size: { line_number_table.size } item(s)]</summary>
            { line_number_tableToXHTML() }
        </details>
    }

    def line_number_tableToXHTML(): Seq[Node] = line_number_table.map(_.toXHTML())

}
