package org.opalj.support.parser

import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.analyses.LazyL0PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.{LazyL1PurityAnalysis, LazyL2PurityAnalysis}

object AnalysisCommandParser {
    def parse(analysisName: String) : FPCFLazyAnalysisScheduler = {
        analysisName match {
            case "L0" => LazyL0PurityAnalysis

            case "L1" => LazyL1PurityAnalysis

            case null | "L2" =>
                LazyL2PurityAnalysis
        }
    }
}
