package org.opalj.Commandline_base.commandlines

object DomainCommand extends OpalPlainCommand[String] {
    override var name: String = "domain"
    override var argName: String = "domain"
    override var description: String = "class name of the abstract interpretation domain"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
