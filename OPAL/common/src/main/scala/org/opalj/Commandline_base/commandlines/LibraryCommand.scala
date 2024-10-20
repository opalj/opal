package org.opalj.Commandline_base.commandlines

import commandlinebase.OpalPlainCommand

object LibraryCommand extends OpalPlainCommand[Boolean] {
    override var name: String = "library"
    override var argName: String = "library"
    override var description: String = "assumes that the target is a library"
    override var defaultValue: Option[Boolean] = None
    override var noshort: Boolean = true
}
