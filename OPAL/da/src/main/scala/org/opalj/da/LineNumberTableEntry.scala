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
case class LineNumberTableEntry(
        start_pc:    Int,
        line_number: Int
) {

    def toXHTML(): Node = {
        <div>start_pc: { start_pc }, line_number: { line_number }</div>
    }
}
