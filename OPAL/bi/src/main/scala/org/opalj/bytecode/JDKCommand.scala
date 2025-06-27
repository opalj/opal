/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bytecode

import java.io.File

import org.opalj.cli.ParsedCommand

import org.rogach.scallop.flagConverter

object JDKCommand extends ParsedCommand[Boolean, File] {
    override val name: String = "JDK"
    override val description: String = "Analyze the JDK instead of a --cp"
    override val defaultValue: Option[Boolean] = Some(false)

    override def parse(arg: Boolean): File = JRELibraryFolder
}
