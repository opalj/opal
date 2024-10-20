package org.opalj.Commandline_base.commandlines

import commandlinebase.OpalPlainCommand

object DebugCommand extends OpalPlainCommand[Boolean] {
    override var name: String = "debug"
    override var argName: String = "debug"
    override var description: String = "enable debug output from PropertyStore"
    override var defaultValue: Option[Boolean] = None
    override var noshort: Boolean = true
}
