/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.fieldassignability.part.CalleesBasedReadWritePathAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.part.CalleesBasedReadWritePathAnalysisState
import org.opalj.tac.fpcf.analyses.fieldassignability.part.ClonePatternAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.part.LazyInitializationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.part.LazyInitializationAnalysisState

/**
 * Determines the assignability of a field based on a more complex analysis of read-write paths than
 * [[L0FieldAssignabilityAnalysis]], and recognizes lazy initialization and clone / factory patterns as safe.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested; see [[LazyInitializationAnalysis]].
 *
 * @author Maximilian Rüsch
 */
class L2FieldAssignabilityAnalysis private[fieldassignability] (val project: SomeProject)
    extends AbstractFieldAssignabilityAnalysis
    with LazyInitializationAnalysis
    with ClonePatternAnalysis
    with CalleesBasedReadWritePathAnalysis {

    case class State(field: Field) extends AbstractFieldAssignabilityAnalysisState
        with LazyInitializationAnalysisState
        with CalleesBasedReadWritePathAnalysisState
    type AnalysisState = State
    override def createState(field: Field): AnalysisState = State(field)
}

object EagerL2FieldAssignabilityAnalysis extends AbstractEagerFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L2FieldAssignabilityAnalysis(p)
}

object LazyL2FieldAssignabilityAnalysis extends AbstractLazyFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L2FieldAssignabilityAnalysis(p)
}
