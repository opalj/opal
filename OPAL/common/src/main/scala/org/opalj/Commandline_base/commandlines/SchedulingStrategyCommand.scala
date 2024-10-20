package org.opalj.Commandline_base.commandlines

import commandlinebase.OpalPlainCommand

object SchedulingStrategyCommand extends OpalPlainCommand[String] {
    override var name: String = "schedulingStrategy"
    override var argName: String = "schedulingStrategy"
    override var description: String = "schedulingStrategy which defines the analysis within the results file>"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true
}
