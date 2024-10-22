package org.opalj.support.parser

import org.opalj.tac.cg.{AllocationSiteBasedPointsToCallGraphKey, CHACallGraphKey, CallGraphKey, RTACallGraphKey}

object CallGraphCommandParser {
    def parse(callGraph: String) : CallGraphKey = {
        callGraph match {
            case "CHA"        => CHACallGraphKey
            case "PointsTo"   => AllocationSiteBasedPointsToCallGraphKey
            case "RTA" | null => RTACallGraphKey
        }
    }
}
