/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object CFGArg extends PlainArg[Boolean] {
    override val name: String = "cfg"
    override val description: String = "Produce a Control-Flow Graph"
    override val defaultValue: Option[Boolean] = Some(false)
}
