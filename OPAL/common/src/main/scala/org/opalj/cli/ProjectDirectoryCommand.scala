/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.stringConverter

object ProjectDirectoryCommand extends ParsedCommand[String, String] {
    override val name: String = "projectDir"
    override val description: String = "Directory with project class files relative to --cp"

    override def parse(projectDir: String): String = {
        projectDir
    }
}
