package org.opalj.support.parser

import org.opalj.Commandline_base.commandlines.OpalCommandExternalParser
import org.opalj.tac.cg.{AllocationSiteBasedPointsToCallGraphKey, CHACallGraphKey, RTACallGraphKey}

object CallGraphCommandExternalParser extends OpalCommandExternalParser{
    override def parse[T](arg: T): Any = {
        val callGraph = arg.asInstanceOf[String]

        callGraph match {
            case "CHA"        => CHACallGraphKey
            case "PointsTo"   => AllocationSiteBasedPointsToCallGraphKey
            case "RTA" | null => RTACallGraphKey
        }
    }
}
