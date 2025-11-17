/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.fieldassignability.part.SimpleReadWritePathAnalysis

/**
 * Determines the assignability of a field based on a simple analysis of read -> write paths.
 *
 * @note May soundly overapproximate the assignability if the TAC is deeply nested.
 *
 * @author Maximilian Rüsch
 * @author Dominik Helm
 */
class L0FieldAssignabilityAnalysis private[fieldassignability] (val project: SomeProject)
    extends AbstractFieldAssignabilityAnalysis
    with SimpleReadWritePathAnalysis {

    case class State(field: Field) extends AbstractFieldAssignabilityAnalysisState
    type AnalysisState = State
    override def createState(field: Field): AnalysisState = State(field)
}

object EagerL0FieldAssignabilityAnalysis extends AbstractEagerFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L0FieldAssignabilityAnalysis(p)
}

object LazyL0FieldAssignabilityAnalysis extends AbstractLazyFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L0FieldAssignabilityAnalysis(p)
}
