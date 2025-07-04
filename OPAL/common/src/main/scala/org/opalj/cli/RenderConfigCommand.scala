/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object RenderConfigCommand extends PlainCommand[Boolean] {
    override val name: String = "renderConfig"
    override val description: String = "Print the configuration"
    override val defaultValue: Option[Boolean] = Some(false)
}
