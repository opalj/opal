package org.opalj.Commandline_base.commandlines

object ClassPathCommand extends OpalPlainCommand[String]{
    override var name: String = "classPath"
    override var argName: String = "classPath"
    override var description: String = "Directories or JAR/class files"
    override var defaultValue: Option[String] = Some(System.getProperty("user.dir"))
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
