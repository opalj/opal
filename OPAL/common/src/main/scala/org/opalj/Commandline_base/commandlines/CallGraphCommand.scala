package org.opalj.Commandline_base.commandlines

object CallGraphCommand extends OpalPlainCommand[String] {
    override var name: String = "callGraph"
    override var argName: String = "callGraph"
    override var description: String = "<CHA|RTA|PointsTo> (Default: RTA)"
    override var defaultValue: Option[String] = Some("RTA")
    override var noshort: Boolean = true
}
