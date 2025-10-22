/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.Field
import org.opalj.br.PC
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode

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

    override def analyzeInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): FieldAssignability = Assignable

    override def analyzeNonInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): FieldAssignability = NonAssignable

    override def analyzeInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): FieldAssignability = NonAssignable // TODO handle constructor escapes

    override def analyzeNonInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): FieldAssignability = Assignable
}

object EagerL0FieldAssignabilityAnalysis extends AbstractEagerFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L0FieldAssignabilityAnalysis(p)
}

object LazyL0FieldAssignabilityAnalysis extends AbstractLazyFieldAssignabilityAnalysisScheduler {
    override def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis = new L0FieldAssignabilityAnalysis(p)
}
