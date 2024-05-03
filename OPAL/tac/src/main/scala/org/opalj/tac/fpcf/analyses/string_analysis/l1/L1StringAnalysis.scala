/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.l1.interpretation.L1InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.PathFinder
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SimplePathFinder

/**
 * @author Maximilian Rüsch
 */
class L1StringAnalysis(val project: SomeProject) extends StringAnalysis {

    override val pathFinder: PathFinder = SimplePathFinder
}

object L1StringAnalysis {

    private[l1] final val FieldWriteThresholdConfigKey = {
        "org.opalj.fpcf.analyses.string_analysis.l1.L1StringAnalysis.fieldWriteThreshold"
    }
}

object LazyL1StringAnalysis extends LazyStringAnalysis {

    override final def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(Callees)) ++ super.uses

    override final def init(p: SomeProject, ps: PropertyStore): InitializationData =
        (new L1StringAnalysis(p), L1InterpretationHandler(p, ps))

    override def requiredProjectInformation: ProjectInformationKeys = Seq(ContextProviderKey) ++
        L1InterpretationHandler.requiredProjectInformation ++
        super.requiredProjectInformation
}
