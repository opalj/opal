/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.PC
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.immutability.FieldAssignability


/**
 * @author Maximilian Rüsch
 */
trait FieldAssignabilityAnalysisPart private[fieldassignability]
    extends FPCFAnalysis {

    type AnalysisState <: AbstractFieldAssignabilityAnalysisState

    def completePatternWithInitializerRead()(implicit state: AnalysisState): Option[FieldAssignability]

    def completePatternWithNonInitializerRead(
        context: Context,
        readPC:  Int
    )(implicit state: AnalysisState): Option[FieldAssignability]

    def completePatternWithInitializerWrite()(implicit state: AnalysisState): Option[FieldAssignability]

    def completePatternWithNonInitializerWrite(
        context: Context,
        tac:     TACode[TACMethodParameter, V],
        writePC: PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Option[FieldAssignability]
}
