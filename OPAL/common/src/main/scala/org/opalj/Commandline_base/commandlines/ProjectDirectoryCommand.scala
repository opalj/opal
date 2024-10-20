package org.opalj.Commandline_base.commandlines

import commandlinebase.OpalPlainCommand

object ProjectDirectoryCommand extends OpalPlainCommand[String] {
    override var name: String = "projectDir"
    override var argName: String = "projectDir"
    override var description: String = "directory with project class files relative to cp"
    override var defaultValue: Option[String] = null
    override var noshort: Boolean = true

    def parse(projectDir: String) : Option[String] = {
        Some(projectDir)
    }
}
