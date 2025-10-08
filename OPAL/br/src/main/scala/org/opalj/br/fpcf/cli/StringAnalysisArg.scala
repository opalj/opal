/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cli

import org.opalj.cli.AnalysisLevelArg

object StringAnalysisArg extends AnalysisLevelArg(
        "String analysis used.",
        "trivial" -> "TrivialStringAnalysis",
        "L0" -> "L0StringFlowAnalysis",
        "L1" -> "L1StringFlowAnalysis",
        "L2" -> "L2StringFlowAnalysis",
        "L3" -> "L3StringFlowAnalysis"
    ) {

    override val name: String = "stringAnalysis"

    def getAnalyses(analysis: String): Seq[String] = analysis match {
        case "TrivialStringAnalysis" => List("TrivialStringAnalysis")
        case _                       => List("StringAnalysis", "MethodStringFlowAnalysis", analysis)
    }
}
