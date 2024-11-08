/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

object RunnerCommand extends OpalPlainCommand[String] {

    override var name: String = "runner name"
    override var argName: String = "runner"
    override var description: String = "The name of the runner, for which some analyses should be set up"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
