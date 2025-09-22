/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l3

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.flowanalysis.LazyMethodStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.interpretation.LazyStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l3.interpretation.L3InterpretationHandler

/**
 * A string analysis that handles field read accesses, calls, and constant and binary expressions.
 *
 * @author Maximilian Rüsch
 */
object LazyL3StringFlowAnalysis extends LazyStringFlowAnalysis {

    def allRequiredAnalyses: Seq[FPCFLazyAnalysisScheduler] = Seq(
        LazyStringAnalysis,
        LazyMethodStringFlowAnalysis,
        LazyL3StringFlowAnalysis
    )

    override final def uses: Set[PropertyBounds] = super.uses ++ L3InterpretationHandler.uses

    override final def init(p: SomeProject, ps: PropertyStore): InitializationData = L3InterpretationHandler(p)

    override def requiredProjectInformation: ProjectInformationKeys = super.requiredProjectInformation ++
        L3InterpretationHandler.requiredProjectInformation
}
