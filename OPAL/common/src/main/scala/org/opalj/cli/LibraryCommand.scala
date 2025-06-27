/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object LibraryCommand extends PlainCommand[Boolean] {
    override val name: String = "library"
    override val description: String = "Assumes that the target is a library"
    override def defaultValue: Option[Boolean] = Some(false)
}
