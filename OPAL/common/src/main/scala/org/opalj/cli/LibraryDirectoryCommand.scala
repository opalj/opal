/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.stringConverter

object LibraryDirectoryCommand extends PlainCommand[String] {
    override val name: String = "libDir"
    override val description: String = "Directory with library class files relative to --cp"
}
