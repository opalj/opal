package org.opalj.Commandline_base.commandlines

trait OpalPlainCommand[T] extends OpalCommand {
    var name: String
    var argName: String
    var description: String
    var defaultValue: Option[T]
    var noshort: Boolean
}
