package org.opalj.Commandline_base.commandlines

import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.analyses.LazyL0PurityAnalysis
import org.opalj.support.info.Purity.usage
import org.opalj.tac.fpcf.analyses.purity.{LazyL1PurityAnalysis, LazyL2PurityAnalysis}

object AnalysisCommand extends OpalChoiceCommand{
    override var name: String = "analysis"
    override var argName: String = "analysis"
    override var description: String = "<L0|L1|L2> (Default: L2, the most precise analysis configuration)"
    override var defaultValue: Some[String] = Some("L2")
    override var noshort: Boolean = true
    override var choices: Seq[String] = Seq("L0", "L1", "L2")

    def parse(analysisName: String) : FPCFLazyAnalysisScheduler = {
        analysisName match {
            case "L0" => LazyL0PurityAnalysis

            case "L1" => LazyL1PurityAnalysis

            case null | "L2" =>
                LazyL2PurityAnalysis

            case _ =>
                Console.println(s"unknown analysis: $analysisName")
                Console.println(usage)
                null
        }
    }
}
