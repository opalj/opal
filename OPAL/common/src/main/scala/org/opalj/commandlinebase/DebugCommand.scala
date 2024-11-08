/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

object DebugCommand extends OpalPlainCommand[Boolean] {
    override var name: String = "debug"
    override var argName: String = "debug"
    override var description: String = "enable debug output from PropertyStore"
    override var defaultValue: Option[Boolean] = None
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
