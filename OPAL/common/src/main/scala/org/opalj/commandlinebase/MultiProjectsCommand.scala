/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

object MultiProjectsCommand extends OpalPlainCommand[Boolean] {
    override var name: String = "multiProjects"
    override var argName: String = "multiProjects"
    override var description: String = "analyzes multiple projects in the subdirectories of -classPath"
    override var defaultValue: Option[Boolean] = None
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
