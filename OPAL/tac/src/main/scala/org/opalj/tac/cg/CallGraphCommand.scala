/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.cli.ChoiceCommand
import org.opalj.cli.ParsedCommand

import org.rogach.scallop.stringConverter

object CallGraphCommand extends ParsedCommand[String, CallGraphKey] with ChoiceCommand[CallGraphKey] {

    override val name: String = "callGraph"
    override val argName: String = "algorithm"
    override val choices = Seq("CHA", "RTA", "PointsTo")
    override val description: String = "Call-graph algorithm used. "
    override val defaultValue: Option[String] = Some("RTA")

    override def parse(callGraph: String): CallGraphKey = {
        callGraph match {
            case "CHA"      => CHACallGraphKey
            case "PointsTo" => AllocationSiteBasedPointsToCallGraphKey
            case "RTA"      => RTACallGraphKey
        }
    }
}
