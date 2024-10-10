/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.flowanalysis.LazyMethodStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.interpretation.LazyStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0InterpretationHandler

/**
 * @see [[L0InterpretationHandler]]
 * @author Maximilian Rüsch
 */
object LazyL0StringAnalysis {

    def allRequiredAnalyses: Seq[FPCFLazyAnalysisScheduler] = Seq(
        LazyStringAnalysis,
        LazyMethodStringFlowAnalysis,
        LazyL0StringFlowAnalysis
    )
}

object LazyL0StringFlowAnalysis extends LazyStringFlowAnalysis {

    override def init(p: SomeProject, ps: PropertyStore): InitializationData = L0InterpretationHandler(p)
}
