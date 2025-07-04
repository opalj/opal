/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object LibrariesAsInterfacesCommand extends PlainCommand[Boolean] {
    override val name: String = "libsAsInterfaces"
    override val description: String = "Load only interfaces of libraries, no implementations"
    override val defaultValue: Option[Boolean] = Some(false)
}
