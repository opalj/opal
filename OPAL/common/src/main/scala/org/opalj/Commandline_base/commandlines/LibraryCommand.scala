package org.opalj.Commandline_base.commandlines

object LibraryCommand extends OpalPlainCommand[Boolean] {
    override var name: String = "library"
    override var argName: String = "library"
    override var description: String = "assumes that the target is a library"
    override var defaultValue: Option[Boolean] = None
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
