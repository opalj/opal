/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package preprocessing

/**
 * An approach based on an intuitive traversing of the control flow graph (CFG). This implementation
 * will use the CFG to find all paths from the very first statement of the CFG to all end / leaf
 * statements in the CFG, ignoring `startSites` and `endSite` passed to
 * [[DefaultPathFinder#findPaths]].
 *
 * @author Maximilian Rüsch
 *
 * @note To fill gaps, e.g., from the very first statement of a context, such as a CFG, to the first
 *       control structure, a consecutive row of path elements are inserted. Arbitrarily inserted
 *       jumps within the bytecode might lead to a different order than the one computed by this
 *       class!
 */
class DefaultPathFinder(tac: TAC) extends AbstractPathFinder(tac) {

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
        if (csInfo.isEmpty) {
            // In case the are no control structures, return a path from the first to the last element
            Path(cfg.startBlock.startPC.until(cfg.code.instructions(endSite).pc).map(FlatPathElement.fromPC).toList)
        } else {
            // Otherwise, order the control structures and assign the corresponding path elements
            val orderedCS = hierarchicallyOrderControlStructures(csInfo)
            hierarchyToPath(orderedCS.hierarchy.head._2, startSite, endSite)
        }
    }
}
