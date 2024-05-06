/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package preprocessing

import org.opalj.br.fpcf.properties.string.StringConstancyInformation
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string.StringTreeNeutralElement
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler

object PathFinder {

    private def evaluatePathElement(pe: PathElement)(implicit
        state: ComputationState,
        ps:    PropertyStore
    ): Option[StringConstancyInformation] = {
        val sci = ps(
            InterpretationHandler.getEntityForPC(pe.pc, state.dm, state.tac, state.entity._1),
            StringConstancyProperty.key
        ) match {
            case UBP(scp) => scp.sci
            case _        => StringConstancyInformation.lb
        }
        Option.unless(sci.isTheNeutralElement)(sci)
    }

    def findPath(value: V, tac: TAC): Option[Path] = {
        val structural = new StructuralAnalysis(tac.cfg)

        if (structural.graph.isCyclic || value.definedBy.size > 1) {
            return None;
        }

        val defSite = value.definedBy.head
        val allDefAndUseSites = tac.stmts(defSite).asAssignment.targetVar.usedBy.+(defSite).toList.sorted

        // val (_, _) = structural.analyze(structural.graph, Region(Block, Set(tac.cfg.startBlock.nodeId)))

        Some(Path(allDefAndUseSites.map(PathElement.apply(_)(tac.stmts))))
    }

    def transformPath(path: Path, tac: TAC)(implicit
        state: ComputationState,
        ps:    PropertyStore
    ): StringTreeNode = {
        path.elements.size match {
            case 1 =>
                evaluatePathElement(path.elements.head).map(_.tree).getOrElse(StringTreeDynamicString)
            case _ =>
                // IMPROVE handle entire control tree here
                val evaluatedElements = path.elements.flatMap(evaluatePathElement)
                evaluatedElements.foldLeft[StringTreeNode](StringTreeNeutralElement) {
                    (currentState, nextSci) => nextSci.treeFn(currentState)
                }
        }
    }
}
