/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

case class BootstrapArgument(cp_ref: Constant_Pool_Index) {

    def toXHTML(implicit cp: Constant_Pool): Node = <div>{ cp(cp_ref).asInstructionParameter }</div>

}
