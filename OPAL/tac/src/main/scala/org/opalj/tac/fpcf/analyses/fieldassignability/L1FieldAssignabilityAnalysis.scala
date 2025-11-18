/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.PropertyBounds
import org.opalj.tac.fpcf.analyses.fieldassignability.part.CalleesBasedReadWritePathAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.part.CalleesBasedReadWritePathAnalysisState

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
    with CalleesBasedReadWritePathAnalysis {

    case class State(field: Field) extends AbstractFieldAssignabilityAnalysisState
        with CalleesBasedReadWritePathAnalysisState
    type AnalysisState = State
    override def createState(field: Field): AnalysisState = State(field)
}

object EagerL1FieldAssignabilityAnalysis extends AbstractEagerFieldAssignabilityAnalysisScheduler {
    override def uses: Set[PropertyBounds] = super.uses + PropertyBounds.ub(Callees)

    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L1FieldAssignabilityAnalysis(p)
}

object LazyL1FieldAssignabilityAnalysis extends AbstractLazyFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L1FieldAssignabilityAnalysis(p)
}
