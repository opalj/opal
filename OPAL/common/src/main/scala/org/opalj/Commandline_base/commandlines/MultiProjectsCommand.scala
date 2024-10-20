package org.opalj.Commandline_base.commandlines

import commandlinebase.OpalPlainCommand


object MultiProjectsCommand extends OpalPlainCommand[Boolean] {
    override var name: String = "multiProjects"
    override var argName: String = "multiProjects"
    override var description: String = "analyzes multiple projects in the subdirectories of -classPath"
    override var defaultValue: Option[Boolean] = None
    override var noshort: Boolean = true
}
