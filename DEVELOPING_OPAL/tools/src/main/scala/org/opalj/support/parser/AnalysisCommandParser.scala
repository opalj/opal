/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package parser

import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.analyses.LazyL0PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis

/**
 * The `AnalysisCommandParser` object is responsible for parsing the runner name and analysis level
 * to return the corresponding analysis scheduler for purity analysis.
 *
 * This utility object matches the given runner name (such as "Purity") and analysis level
 * (such as "L0", "L1", or "L2") to return the appropriate `FPCFLazyAnalysisScheduler` instance.
 * If an invalid combination of runner name or analysis level is provided, an
 * `IllegalArgumentException` is thrown.
 *
 * Example usage:
 * {{{
 * val scheduler = AnalysisCommandParser.parse("Purity", "L0")
 * }}}
 *
 * @object AnalysisCommandParser
 */
object AnalysisCommandParser {
    def parse(runnerName: Option[String], analysisLevel: Option[String]): Option[FPCFLazyAnalysisScheduler] =
        runnerName.get match {
            case "Purity" =>
                analysisLevel.get match {
                    case "L0"        => Some(LazyL0PurityAnalysis)
                    case "L1"        => Some(LazyL1PurityAnalysis)
                    case null | "L2" => Some(LazyL2PurityAnalysis)
                    case _           => throw new IllegalArgumentException(s"Unknown analysis: $runnerName, $analysisLevel")
                }
        }

}
