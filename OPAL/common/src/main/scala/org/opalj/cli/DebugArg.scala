/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object DebugArg extends PlainArg[Boolean] {
    override val name: String = "debug"
    override val description: String = "Enable additional debug output"
    override val defaultValue: Option[Boolean] = Some(false)
    override val noshort: Boolean = false
}
