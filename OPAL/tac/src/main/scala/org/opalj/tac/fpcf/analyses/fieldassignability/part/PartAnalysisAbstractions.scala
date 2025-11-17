/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability
package part

import org.opalj.br.PC
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.fpcf.SomeEPS

/**
 * @author Maximilian Rüsch
 */
trait PartAnalysisAbstractions private[fieldassignability]
    extends FPCFAnalysis {

    type AnalysisState <: AnyRef

    private[fieldassignability] type PartHook =
        (Context, TACode[TACMethodParameter, V], PC, Option[V], AnalysisState) => Option[FieldAssignability]

    private[fieldassignability] type PartContinuation =
        (SomeEPS, AnalysisState) => Option[FieldAssignability]

    private[fieldassignability] case class PartInfo(
        onInitializerRead: PartHook = (_, _, _, _, _) => None,
        onNonInitializerRead: PartHook = (_, _, _, _, _) => None,
        onInitializerWrite: PartHook = (_, _, _, _, _) => None,
        onNonInitializerWrite: PartHook = (_, _, _, _, _) => None,
        continuation: PartContinuation = (_, _) => None,
    )

    def registerPart(partInfo: PartInfo): Unit
}
