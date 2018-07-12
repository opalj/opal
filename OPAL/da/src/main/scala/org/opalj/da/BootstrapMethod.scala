/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

case class BootstrapMethod(method_ref: Constant_Pool_Index, arguments: Seq[BootstrapArgument]) {

    /**
     * Number of bytes to store the bootstrap method.
     */
    def size: Int = {
        2 /* bootstrap_method_ref */ + 2 + /* num_bootstrap_arguments */
            arguments.length * 2 /* bootstrap_arguments */
    }

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="bootstrap_method">
            <summary>{ cp(method_ref).asInstructionParameter }</summary>
            { argumentsToXHTML(cp) }
        </details>
    }

    def argumentsToXHTML(implicit cp: Constant_Pool): Seq[Node] = arguments.map(_.toXHTML(cp))
}
