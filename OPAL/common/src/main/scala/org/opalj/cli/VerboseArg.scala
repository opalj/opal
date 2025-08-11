/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object VerboseArg extends PlainArg[Boolean] {
    override val name: String = "verbose"
    override val description: String = "Produce more verbose output"
    override val defaultValue: Option[Boolean] = Some(false)
    override val noshort: Boolean = false
}
