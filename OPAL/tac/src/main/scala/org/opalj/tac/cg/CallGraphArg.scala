/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.rogach.scallop.stringConverter

import org.opalj.cli.ChoiceArg
import org.opalj.cli.ParsedArg

object CallGraphArg extends ParsedArg[String, CallGraphKey] with ChoiceArg[CallGraphKey] {

    override val name: String = "callGraph"
    override val argName: String = "algorithm"
    override val choices =
        Seq("CHA", "RTA", "MTA", "FTA", "CTA", "XTA", "TypeBasedPointsTo", "PointsTo", "1-0-CFA", "1-1-CFA")
    override val description: String = "Call-graph algorithm used. "
    override val defaultValue: Option[String] = Some("RTA")

    override def parse(callGraph: String): CallGraphKey = {
        callGraph match {
            case "CHA"               => CHACallGraphKey
            case "RTA"               => RTACallGraphKey
            case "MTA"               => MTACallGraphKey
            case "FTA"               => FTACallGraphKey
            case "CTA"               => CTACallGraphKey
            case "XTA"               => XTACallGraphKey
            case "TypeBasedPointsTo" => TypeBasedPointsToCallGraphKey
            case "PointsTo"          => AllocationSiteBasedPointsToCallGraphKey
            case "1-0-CFA"           => CFA_1_0_CallGraphKey
            case "1-1-CFA"           => CFA_1_1_CallGraphKey
        }
    }
}
