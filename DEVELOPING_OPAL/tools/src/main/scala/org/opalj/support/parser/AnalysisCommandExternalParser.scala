package org.opalj.support.parser

import org.opalj.Commandline_base.commandlines.OpalCommandExternalParser
import org.opalj.br.fpcf.analyses.LazyL0PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.{LazyL1PurityAnalysis, LazyL2PurityAnalysis}

object AnalysisCommandExternalParser extends OpalCommandExternalParser{
    override def parse[T](arg: T): Any = {
        val analysisName = arg.asInstanceOf[String]

        analysisName match {
            case "L0" => LazyL0PurityAnalysis
            case "L1" => LazyL1PurityAnalysis
            case null | "L2" => LazyL2PurityAnalysis
            case _ => throw new IllegalArgumentException(s"Unknown analysis: $analysisName")
        }
    }
}
