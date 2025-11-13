/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject

/**
 * A field assignability analysis that treats every field access (reads and writes) as unsafe and thus immediately
 * marks the field as assignable if one such read / write is detected.
 *
 * @author Maximilian Rüsch
 * @author Dominik Helm
 */
class L0FieldAssignabilityAnalysis private[fieldassignability] (val project: SomeProject)
    extends AbstractFieldAssignabilityAnalysis {

    case class State(field: Field) extends AbstractFieldAssignabilityAnalysisState
    type AnalysisState = State
    override def createState(field: Field): AnalysisState = State(field)

    override protected lazy val parts = Seq.empty[FieldAssignabilityAnalysisPart]
}

object EagerL0FieldAssignabilityAnalysis extends AbstractEagerFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L0FieldAssignabilityAnalysis(p)
}

object LazyL0FieldAssignabilityAnalysis extends AbstractLazyFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L0FieldAssignabilityAnalysis(p)
}
