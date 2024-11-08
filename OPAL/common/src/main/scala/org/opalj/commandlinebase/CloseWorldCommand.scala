/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

object CloseWorldCommand extends OpalPlainCommand[Boolean] {
    override var name: String = "closeWordAssumption"
    override var argName: String = "cwa"
    override var description: String = "uses closed world assumption, i.e. no class can be extended"
    override var defaultValue: Option[Boolean] = None
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
