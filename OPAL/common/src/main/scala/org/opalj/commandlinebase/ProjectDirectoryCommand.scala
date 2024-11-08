/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

object ProjectDirectoryCommand extends OpalPlainCommand[String] {
    override var name: String = "projectDir"
    override var argName: String = "projectDir"
    override var description: String = "directory with project class files relative to cp"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = {
        val projectDir = arg.asInstanceOf[String]
        Some(projectDir)
    }
}
