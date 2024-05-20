/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0InterpretationHandler

/**
 * @author Maximilian Rüsch
 */
class L0StringAnalysis(override val project: SomeProject) extends StringAnalysis

object LazyL0StringAnalysis extends LazyStringAnalysis {

    override def init(p: SomeProject, ps: PropertyStore): InitializationData = new L0StringAnalysis(p)
}

object LazyL0StringFlowAnalysis extends LazyStringFlowAnalysis {

    override def init(p: SomeProject, ps: PropertyStore): InitializationData = L0InterpretationHandler(p)
}
