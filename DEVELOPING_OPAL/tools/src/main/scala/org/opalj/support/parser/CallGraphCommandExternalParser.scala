package org.opalj.support.parser

import org.opalj.Commandline_base.commandlines.OpalCommandExternalParser
import org.opalj.tac.cg.{AllocationSiteBasedPointsToCallGraphKey, CHACallGraphKey, CallGraphKey, RTACallGraphKey}

/**
 * `CallGraphCommandExternalParser` is a parser for selecting a call graph analysis type.
 * It maps a command-line argument to the corresponding `CallGraphKey`.
 */
object CallGraphCommandExternalParser extends OpalCommandExternalParser[CallGraphKey]{
    override def parse[T](arg: T): CallGraphKey = {
        val callGraph = arg.asInstanceOf[String]

        callGraph match {
            case "CHA" => CHACallGraphKey
            case "PointsTo" => AllocationSiteBasedPointsToCallGraphKey
            case "RTA" | null => RTACallGraphKey
        }
    }
}
