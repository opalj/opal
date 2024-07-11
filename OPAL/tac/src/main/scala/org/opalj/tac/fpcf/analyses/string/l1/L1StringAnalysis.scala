/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.SystemProperties
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.l1.interpretation.L1InterpretationHandler

/**
 * @author Maximilian Rüsch
 */
object L1StringAnalysis {

    private[l1] final val ConfigLogCategory = "analysis configuration - l1 string analysis"
}

object LazyL1StringAnalysis {

    def allRequiredAnalyses: Seq[FPCFLazyAnalysisScheduler] = Seq(
        LazyStringAnalysis,
        LazyMethodStringFlowAnalysis,
        LazyL1StringFlowAnalysis
    )
}

object LazyL1StringFlowAnalysis extends LazyStringFlowAnalysis {

    override final def uses: Set[PropertyBounds] = super.uses ++ PropertyBounds.ubs(
        Callees,
        FieldWriteAccessInformation,
        SystemProperties
    )

    override final def init(p: SomeProject, ps: PropertyStore): InitializationData = L1InterpretationHandler(p)

    override def requiredProjectInformation: ProjectInformationKeys = super.requiredProjectInformation ++
        L1InterpretationHandler.requiredProjectInformation
}
