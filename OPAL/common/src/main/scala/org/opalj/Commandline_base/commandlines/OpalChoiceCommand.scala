package org.opalj.Commandline_base.commandlines

trait OpalChoiceCommand {
    var name: String
    var argName: String
    var description: String
    var defaultValue: Some[String]
    var noshort: Boolean
    var choices: Seq[String]
}
