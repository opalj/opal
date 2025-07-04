/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object LibraryArg extends PlainArg[Boolean] {
    override val name: String = "library"
    override val description: String = "Assumes that the target is a library"
    override val defaultValue: Option[Boolean] = Some(false)
}
