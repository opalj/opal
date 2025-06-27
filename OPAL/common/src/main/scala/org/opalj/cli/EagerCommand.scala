/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object EagerCommand extends PlainCommand[Boolean] {
    override val name: String = "eager"
    override val description: String = "Execute all analyses eagerly"
    override val defaultValue: Option[Boolean] = Some(false)
}
