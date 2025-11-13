/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis

/**
 * Determines the assignability of a field.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 * @author Maximilian Rüsch
 */
class L2FieldAssignabilityAnalysis private[fieldassignability] (val project: SomeProject)
    extends AbstractFieldAssignabilityAnalysis
    with FPCFAnalysis {

    case class State(field: Field) extends AbstractFieldAssignabilityAnalysisState
        with LazyInitializationAnalysisState
    type AnalysisState = State
    override def createState(field: Field): AnalysisState = State(field)

    private class L2LazyInitializationPart(val project: SomeProject) extends LazyInitializationAnalysis {
        override type AnalysisState = L2FieldAssignabilityAnalysis.this.AnalysisState
    }
    private class L2ClonePatternPart(val project: SomeProject) extends ClonePatternAnalysis {
        override type AnalysisState = L2FieldAssignabilityAnalysis.this.AnalysisState
    }
    private class L2ReadWritePathPart(val project: SomeProject) extends ExtensiveReadWritePathAnalysis {
        override type AnalysisState = L2FieldAssignabilityAnalysis.this.AnalysisState
    }

    override protected lazy val parts: Seq[FieldAssignabilityAnalysisPart] = List(
        L2LazyInitializationPart(project),
        L2ClonePatternPart(project),
        L2ReadWritePathPart(project)
    )
}

object EagerL2FieldAssignabilityAnalysis extends AbstractEagerFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L2FieldAssignabilityAnalysis(p)
}

object LazyL2FieldAssignabilityAnalysis extends AbstractLazyFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L2FieldAssignabilityAnalysis(p)
}
