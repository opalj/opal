package org.opalj.Commandline_base.commandlines

object LibraryDirectoryCommand extends OpalPlainCommand[String] {
    override var name: String = "libDir"
    override var argName: String = "libDir"
    override var description: String = "directory with library class files relative to ClassPath"
    override var defaultValue: Option[String] = null
    override var noshort: Boolean = true

    def parse(libDir: String) : Option[String] = {
        Some(libDir)
    }
}
