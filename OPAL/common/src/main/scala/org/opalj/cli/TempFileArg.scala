/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object TempFileArg extends PlainArg[Boolean] {
    override val name: String = "tmpFile"
    override val description: String = "Writes output to temporary file (and opens it)"
    override val defaultValue: Option[Boolean] = Some(false)
}
