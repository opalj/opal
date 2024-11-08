/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

object EscapeCommand extends OpalChoiceCommand {
    override var name: String = "escape"
    override var argName: String = "escape"
    override var description: String = "<none|L0|L1> (Default: L1, the most precise configuration)"
    override var defaultValue: Option[String] = Some("L1")
    override var noshort: Boolean = true
    override var choices: Seq[String] = Seq("none", "L0", "L1")

    override def parse[T](arg: T): Any = null
}
