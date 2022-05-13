/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.Text

/**
 * <pre>
 * Exceptions_attribute {
 *  u2 attribute_name_index;
 *  u4 attribute_length;
 *  u2 number_of_exceptions;
 *  u2 exception_index_table[number_of_exceptions];
 * }
 * </pre>
 *
 * @author Michael Eichberg
 */
case class Exceptions_attribute(
        attribute_name_index:  Constant_Pool_Index,
        exception_index_table: ExceptionIndexTable
) extends Attribute {

    override def attribute_length: Int = 2 /*table_size*/ + exception_index_table.size * 2

    def exceptionsSpan(implicit cp: Constant_Pool): Node = {
        if (exception_index_table.nonEmpty)
            <span class="throws">
                throws
                {
                    exception_index_table.map(cp(_).asInstructionParameter).reduce[Seq[Node]] { (r, e) =>
                        (r.theSeq :+ Text(", ")) ++ e.theSeq
                    }
                }
            </span>
        else
            <span>&lt;Empty&gt;</span>
    }

    // Primarily implemented to handle the case if the attribute is not found in an expected place.
    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details>
            <summary class="attribute_name">Exceptions [size: { exception_index_table.size } item(s)]</summary>
            <ol>
                {
                    exception_index_table.map[Node] { cpIndex =>
                        <li>{ cp(cpIndex).asInstructionParameter }</li>
                    }
                }
            </ol>
        </details>
    }

}
