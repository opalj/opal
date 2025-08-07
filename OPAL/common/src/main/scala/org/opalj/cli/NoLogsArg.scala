/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object NoLogsArg extends PlainArg[Boolean] {
    override val name: String = "noLogs"
    override val description: String = "Print no log information"
    override val defaultValue: Option[Boolean] = Some(false)
}
