/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.preprocessing

import org.opalj.br.cfg.CFG
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * An approach based on an intuitive traversing of the control flow graph (CFG). This implementation
 * will use the CFG to find all paths from the very first statement of the CFG to all end / leaf
 * statements in the CFG, ignoring `startSites` and `endSite` passed to
 * [[DefaultPathFinder#findPaths]].
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
class DefaultPathFinder(cfg: CFG[Stmt[V], TACStmts[V]]) extends AbstractPathFinder(cfg) {

    /**
     * This implementation finds all paths based on an a naive / intuitive traversing of the `cfg`
     * and, based on that, determines in what relation a statement / instruction is with its
     * predecessors / successors.
     * The paths contain all instructions, not only those that modify a [[StringBuilder]] /
     * [[StringBuffer]] object.
     * In this implementation, `startSites` as well as `endSite` are ignored, i.e., it is fine to
     * pass any values for these two.
     *
     * @see [[AbstractPathFinder.findPaths]]
     */
    override def findPaths(startSites: List[Int], endSite: Int): Path = {
        val startSite = cfg.startBlock.startPC
        val endSite = cfg.code.instructions.length - 1
        val csInfo = findControlStructures(List(startSite), endSite)
        // In case the are no control structures, return a path from the first to the last element
        if (csInfo.isEmpty) {
            Path(cfg.startBlock.startPC.until(endSite).map(FlatPathElement).toList)
        } // Otherwise, order the control structures and assign the corresponding path elements
        else {
            val orderedCS = hierarchicallyOrderControlStructures(csInfo)
            hierarchyToPath(orderedCS.hierarchy.head._2, startSite, endSite)
        }
    }

}
