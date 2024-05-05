/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package preprocessing

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringTreeConcat
import org.opalj.br.fpcf.properties.string_definition.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string_definition.StringTreeNeutralElement
import org.opalj.br.fpcf.properties.string_definition.StringTreeNode
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

trait PathFinder {

    def findPath(value: V, tac: TAC): Option[Path]

    def transformPath(path: Path, tac: TAC)(implicit state: ComputationState, ps: PropertyStore): StringTreeNode

    protected def evaluatePathElement(pe: PathElement)(implicit
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
}

/**
 * Implements very simple path finding, by checking if the given TAC may contain any control structures and creating a
 * path from the very first to the very last statement of the underlying CFG.
 */
object SimplePathFinder extends PathFinder {

    override def findPath(value: V, tac: TAC): Option[Path] = {
        val containsComplexControlFlow = tac.stmts.exists {
            case _: If[V]     => true
            case _: Switch[V] => true
            case _: Throw[V]  => true
            case _: Goto      => true
            case _: JSR       => true
            case _            => false
        } || tac.cfg.catchNodes.nonEmpty

        if (containsComplexControlFlow) {
            None
        } else {
            val cfg = tac.cfg
            Some(Path(cfg.startBlock.startPC.until(cfg.code.instructions.last.pc).map(PathElement.fromPC).toList))
        }
    }

    override def transformPath(path: Path, tac: TAC)(implicit
        state: ComputationState,
        ps:    PropertyStore
    ): StringTreeNode = {
        path.elements.size match {
            case 1 =>
                evaluatePathElement(path.elements.head).map(_.tree).getOrElse(StringTreeDynamicString)
            case _ =>
                StringTreeConcat(path.elements.flatMap(evaluatePathElement).map(_.tree))
        }
    }
}

object StructuralAnalysisPathFinder extends PathFinder {

    /**
     * Always returns a path from the first to the last statement pc in the CFG of the given TAC
     */
    override def findPath(value: V, tac: TAC): Option[Path] = {
        val structural = new StructuralAnalysis(tac.cfg)

        if (structural.graph.isCyclic || value.definedBy.size > 1) {
            return None;
        }

        val defSite = value.definedBy.head
        val allDefAndUseSites = tac.stmts(defSite).asAssignment.targetVar.usedBy.+(defSite).toList.sorted

        // val (_, _) = structural.analyze(structural.graph, Region(Block, Set(tac.cfg.startBlock.nodeId)))

        Some(Path(allDefAndUseSites.map(PathElement.apply(_)(tac.stmts))))
    }

    override def transformPath(path: Path, tac: TAC)(implicit
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
