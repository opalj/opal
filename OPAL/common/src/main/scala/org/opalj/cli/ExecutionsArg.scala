/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.intConverter

object ExecutionsArg extends PlainArg[Int] {
    override val name: String = "executions"
    override val argName: String = "numExecutions"
    override val description: String = "Number of times the analysis is to be executed"
    override val defaultValue: Option[Int] = Some(1)
    override val noshort: Boolean = false
    override val short: Char = 'n'
}
