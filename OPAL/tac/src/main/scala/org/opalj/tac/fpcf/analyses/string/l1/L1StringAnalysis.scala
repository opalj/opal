/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.flowanalysis.LazyMethodStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.interpretation.LazyStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l1.interpretation.L1InterpretationHandler

/**
 * @see [[L1InterpretationHandler]]
 * @author Maximilian Rüsch
 */
object LazyL1StringAnalysis {

    def allRequiredAnalyses: Seq[FPCFLazyAnalysisScheduler] = Seq(
        LazyStringAnalysis,
        LazyMethodStringFlowAnalysis,
        LazyL1StringFlowAnalysis
    )
}

object LazyL1StringFlowAnalysis extends LazyStringFlowAnalysis {

    override def init(p: SomeProject, ps: PropertyStore): InitializationData = L1InterpretationHandler(p)
}
