/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

object AnalysisLevelCommand extends OpalChoiceCommand {
    override var name: String = "analysis level"
    override var argName: String = "level"
    override var description: String = "<L0|L1|L2> (Default: L2, the most precise analysis configuration)"
    override var defaultValue: Option[String] = Some("L2")
    override var noshort: Boolean = true
    override var choices: Seq[String] = Seq("L0", "L1", "L2")

    override def parse[T](arg: T): Any = null
}
