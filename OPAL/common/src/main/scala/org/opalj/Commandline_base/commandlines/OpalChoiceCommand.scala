package org.opalj.Commandline_base.commandlines

trait OpalChoiceCommand extends OpalCommand {
    var name: String
    var argName: String
    var description: String
    var defaultValue: Option[String]
    var noshort: Boolean
    var choices: Seq[String]
}
