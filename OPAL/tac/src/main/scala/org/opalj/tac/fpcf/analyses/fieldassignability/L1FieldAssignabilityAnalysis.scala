/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.fieldassignability.part.ExtensiveReadWritePathAnalysis

/**
 * Determines the assignability of a field based on a more complex analysis of read-write paths than
 * [[L0FieldAssignabilityAnalysis]].
 *
 * @note May soundly overapproximate the assignability if the TAC is deeply nested.
 *
 * @author Maximilian Rüsch
 * @author Dominik Helm
 */
class L1FieldAssignabilityAnalysis private[fieldassignability] (val project: SomeProject)
    extends AbstractFieldAssignabilityAnalysis
    with ExtensiveReadWritePathAnalysis {

    case class State(field: Field) extends AbstractFieldAssignabilityAnalysisState
    type AnalysisState = State
    override def createState(field: Field): AnalysisState = State(field)
}

object EagerL2FieldAssignabilityAnalysis extends AbstractEagerFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L2FieldAssignabilityAnalysis(p)
}

object LazyL2FieldAssignabilityAnalysis extends AbstractLazyFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L2FieldAssignabilityAnalysis(p)
}
