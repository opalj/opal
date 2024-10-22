package org.opalj.Commandline_base.commandlines

object JDKCommand extends OpalPlainCommand[Boolean] {
    override var name: String = "noJDK"
    override var argName: String = "noJDK"
    override var description: String = "do not analyze any JDK methods"
    override var defaultValue: Option[Boolean] = Some(false)
    override var noshort: Boolean = true
}
