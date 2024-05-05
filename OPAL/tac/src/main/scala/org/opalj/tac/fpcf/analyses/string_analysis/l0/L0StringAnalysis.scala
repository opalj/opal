/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.PathFinder
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.StructuralAnalysisPathFinder

/**
 * @author Maximilian Rüsch
 */
class L0StringAnalysis(override val project: SomeProject) extends StringAnalysis {

    override val pathFinder: PathFinder = StructuralAnalysisPathFinder
}

object LazyL0StringAnalysis extends LazyStringAnalysis {

    override def init(p: SomeProject, ps: PropertyStore): InitializationData =
        (new L0StringAnalysis(p), L0InterpretationHandler()(p, ps))
}
