package org.opalj.Commandline_base.commandlines

object AnalysisCommand extends OpalChoiceCommand{
    override var name: String = "analysis"
    override var argName: String = "analysis"
    override var description: String = "<L0|L1|L2> (Default: L2, the most precise analysis configuration)"
    override var defaultValue: Option[String] = Some("L2")
    override var noshort: Boolean = true
    override var choices: Seq[String] = Seq("L0", "L1", "L2")

    override def parse[T](arg: T): Any = null
}
