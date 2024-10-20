package org.opalj.Commandline_base.commandlines

import org.opalj.support.info.Purity.usage
import org.opalj.tac.cg.{AllocationSiteBasedPointsToCallGraphKey, CHACallGraphKey, CallGraphKey, RTACallGraphKey}

object CallGraphCommand extends OpalPlainCommand[String] {
    override var name: String = "callGraph"
    override var argName: String = "callGraph"
    override var description: String = "<CHA|RTA|PointsTo> (Default: RTA)"
    override var defaultValue: Option[String] = Some("RTA")
    override var noshort: Boolean = true

    def parse(callGraph: String) : CallGraphKey = {
        callGraph match {
            case "CHA"        => CHACallGraphKey
            case "PointsTo"   => AllocationSiteBasedPointsToCallGraphKey
            case "RTA" | null => RTACallGraphKey
            case _ =>
                Console.println(s"unknown call graph analysis: $callGraph")
                Console.println(usage)
            null
        }
    }
}
