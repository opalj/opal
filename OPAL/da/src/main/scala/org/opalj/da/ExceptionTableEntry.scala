/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import org.opalj.bytecode.PC

import scala.xml.Node
import scala.xml.Text

/**
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class ExceptionTableEntry(
        start_pc:   PC,
        end_pc:     PC,
        handler_pc: PC,
        catch_type: Int
) {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        // IMPROVE [L2] Write it out as a table row (adapt toXHTML) in ExceptionTable
        <li>try [{ start_pc }-{ end_pc }) catch { handler_pc } { if (catch_type != 0) { asJavaObjectType(catch_type).asSpan("") } else Text("<ANY>") }</li>
    }
}
