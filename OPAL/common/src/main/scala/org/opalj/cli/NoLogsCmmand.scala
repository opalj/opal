/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.cli

import org.rogach.scallop.flagConverter

object NoLogsCommand extends PlainCommand[Boolean] {
    override val name: String = "noLogs"
    override val description: String = "Print no log information"
    override val defaultValue: Option[Boolean] = Some(false)
}
