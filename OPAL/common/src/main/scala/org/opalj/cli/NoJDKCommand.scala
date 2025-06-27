/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object NoJDKCommand extends PlainCommand[Boolean] {
    override val name: String = "noJDK"
    override val description: String = "Do not analyze any JDK methods"
    override val defaultValue: Option[Boolean] = Some(false)
}
