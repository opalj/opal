/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.commandlinebase

object FieldAssignabilityCommand extends OpalChoiceCommand {
    override var name: String = "fieldAssignability"
    override var argName: String = "fieldAssignability"
    override var description: String = "<none|L0|L1|L2> (Default: Depends on analysis level)"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true
    override var choices: Seq[String] = Seq("none", "LO", "L1", "L2")

    override def parse[T](arg: T): Any = null
}
