package org.opalj.Commandline_base.commandlines

object FieldAssignabilityCommand extends OpalChoiceCommand{
    override var name: String = "fieldAssignability"
    override var argName: String = "fieldAssignability"
    override var description: String = "<none|L0|L1|L2> (Default: Depends on analysis level)"
    override var defaultValue: Some[String] = null
    override var noshort: Boolean = true
    override var choices: Seq[String] = Seq("none", "LO", "L1", "L2")
}
