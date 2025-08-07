/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object InvertArg extends PlainArg[Boolean] {
    override val name: String = "invert"
    override val description: String = "Return graph with inverted edges"
    override val defaultValue: Option[Boolean] = Some(false)
}
