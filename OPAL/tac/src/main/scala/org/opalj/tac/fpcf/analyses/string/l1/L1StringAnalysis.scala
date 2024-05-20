/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.l1.interpretation.L1InterpretationHandler

/**
 * @author Maximilian Rüsch
 */
class L1StringAnalysis(val project: SomeProject) extends StringAnalysis

object L1StringAnalysis {

    private[l1] final val FieldWriteThresholdConfigKey = {
        "org.opalj.fpcf.analyses.string_analysis.l1.L1StringAnalysis.fieldWriteThreshold"
    }
}

object LazyL1StringAnalysis extends LazyStringAnalysis {

    override final def init(p: SomeProject, ps: PropertyStore): InitializationData = new L1StringAnalysis(p)
}

object LazyL1StringFlowAnalysis extends LazyStringFlowAnalysis {

    override final def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(Callees)) ++ super.uses

    override final def init(p: SomeProject, ps: PropertyStore): InitializationData = L1InterpretationHandler(p)

    override def requiredProjectInformation: ProjectInformationKeys = super.requiredProjectInformation ++
        L1InterpretationHandler.requiredProjectInformation
}
