/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package parser

import org.opalj.commandlinebase.OpalCommandExternalParser
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.CallGraphKey
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey

/**
 * `CallGraphCommandExternalParser` is a parser for selecting a call graph analysis type.
 * It maps a command-line argument to the corresponding `CallGraphKey`.
 */
object CallGraphCommandExternalParser extends OpalCommandExternalParser[String, CallGraphKey] {
    override def parse(arg: String): CallGraphKey = {
        arg match {
            case "CHA"        => CHACallGraphKey
            case "PointsTo"   => AllocationSiteBasedPointsToCallGraphKey
            case "RTA" | null => RTACallGraphKey
        }
    }
}
