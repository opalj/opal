/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.preprocessing

import org.opalj.br.cfg.CFG
import org.opalj.tac.If
import org.opalj.tac.Stmt
import org.opalj.tac.Switch
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * An approach based on an intuitive traversing of the control flow graph (CFG). This implementation
 * will use the CFG to find all paths from the given `startSites` to the `endSite`. ("Window" as
 * only part of the whole CFG is considered.)
 *
 * @param cfg The CFG on which this instance will operate on.
 *
 * @author Patrick Mell
 *
 * @note To fill gaps, e.g., from the very first statement of a context, such as a CFG, to the first
 *       control structure, a consecutive row of path elements are inserted. Arbitrarily inserted
 *       jumps within the bytecode might lead to a different order than the one computed by this
 *       class!
 */
class WindowPathFinder(cfg: CFG[Stmt[V], TACStmts[V]]) extends AbstractPathFinder(cfg) {

    /**
     * This implementation finds all paths based on an a naive / intuitive traversing of the `cfg`
     * and, based on that, determines in what relation a statement / instruction is with its
     * predecessors / successors.
     * The paths contain all instructions, not only those that modify a [[StringBuilder]] /
     * [[StringBuffer]] object.
     * For this implementation, `startSites` as well as `endSite` are required!
     *
     * @see [[AbstractPathFinder.findPaths]]
     */
    override def findPaths(startSites: List[Int], endSite: Int): Path = {
        // If there are multiple start sites, find the parent "if" or "switch" and use that as a
        // start site
        var startSite: Option[Int] = None
        if (startSites.tail.nonEmpty) {
            var nextStmt = startSites.min
            while (nextStmt >= 0 && startSite.isEmpty) {
                cfg.code.instructions(nextStmt) match {
                    case iff: If[V] if startSites.contains(iff.targetStmt) ⇒
                        startSite = Some(nextStmt)
                    case _: Switch[V] ⇒
                        val (startSwitch, endSwitch, _) = processSwitch(nextStmt)
                        val isParentSwitch = startSites.forall {
                            nextStartSite ⇒ nextStartSite >= startSwitch && nextStartSite <= endSwitch
                        }
                        if (isParentSwitch) {
                            startSite = Some(nextStmt)
                        }
                    case _ ⇒
                }
                nextStmt -= 1
            }
            if (startSite.isEmpty) {
                startSite = Some(0)
            }
        } else {
            startSite = Some(startSites.head)
        }

        val csInfo = findControlStructures(List(startSite.get), endSite)
        // In case the are no control structures, return a path from the first to the last element
        if (csInfo.isEmpty) {
            val indexLastStmt = cfg.code.instructions.length
            Path(cfg.startBlock.startPC.until(indexLastStmt).map(FlatPathElement).toList)
        } // Otherwise, order the control structures and assign the corresponding path elements
        else {
            val orderedCS = hierarchicallyOrderControlStructures(csInfo)
            hierarchyToPath(orderedCS.hierarchy.head._2, startSite.get, endSite)
        }
    }

}
