/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package parser

import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.analyses.LazyL0PurityAnalysis
import org.opalj.commandlinebase.OpalCommandExternalParser
import org.opalj.tac.fpcf.analyses.purity.LazyL1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis

/**
 * `AnalysisCommandExternalParser` interprets a command-line argument specifying a purity analysis level
 * and returns the corresponding `FPCFLazyAnalysisScheduler`.
 */
object AnalysisCommandExternalParser extends OpalCommandExternalParser[String, FPCFLazyAnalysisScheduler] {
    override def parse(arg: String): FPCFLazyAnalysisScheduler = {
        arg match {
            case "L0"        => LazyL0PurityAnalysis
            case "L1"        => LazyL1PurityAnalysis
            case null | "L2" => LazyL2PurityAnalysis
            case _           => throw new IllegalArgumentException(s"Unknown analysis: $arg")
        }
    }
}
