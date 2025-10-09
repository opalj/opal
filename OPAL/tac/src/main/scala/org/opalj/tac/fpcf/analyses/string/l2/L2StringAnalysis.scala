/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l2

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.flowanalysis.LazyMethodStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.interpretation.LazyStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l2.interpretation.L2InterpretationHandler

/**
 * A string analysis that handles calls via the call graph, in addition to constant and binary expressions.
 *
 * @author Maximilian Rüsch
 */
object LazyL2StringFlowAnalysis extends LazyStringFlowAnalysis {

    def allRequiredAnalyses: Seq[FPCFLazyAnalysisScheduler] = Seq(
        LazyStringAnalysis,
        LazyMethodStringFlowAnalysis,
        LazyL2StringFlowAnalysis
    )

    override final def uses: Set[PropertyBounds] = super.uses ++ L2InterpretationHandler.uses

    override final def init(p: SomeProject, ps: PropertyStore): InitializationData = L2InterpretationHandler(p)

    override def requiredProjectInformation: ProjectInformationKeys = super.requiredProjectInformation ++
        L2InterpretationHandler.requiredProjectInformation
}
