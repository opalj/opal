package org.opalj.Commandline_base.commandlines

import commandlinebase.OpalPlainCommand

object IndividualCommand extends OpalPlainCommand[Boolean] {
    override var name: String = "individual"
    override var argName: String = "individual"
    override var description: String = "reports the purity result for each method"
    override var defaultValue: Option[Boolean] = None
    override var noshort: Boolean = true
}
