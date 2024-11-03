/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.commandlinebase

object JDKCommand extends OpalPlainCommand[Boolean] {
    override var name: String = "noJDK"
    override var argName: String = "noJDK"
    override var description: String = "do not analyze any JDK methods"
    override var defaultValue: Option[Boolean] = Some(false)
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
