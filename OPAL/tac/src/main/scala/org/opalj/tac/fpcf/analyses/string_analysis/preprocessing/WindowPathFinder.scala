/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package preprocessing

/**
 * An approach based on an intuitive traversing of the control flow graph (CFG). This implementation
 * will use the CFG to find all paths from the given `startSites` to the `endSite`. ("Window" as
 * only part of the whole CFG is considered.)
 *
 * @author Maximilian Rüsch
 *
 * @note To fill gaps, e.g., from the very first statement of a context, such as a CFG, to the first
 *       control structure, a consecutive row of path elements are inserted. Arbitrarily inserted
 *       jumps within the bytecode might lead to a different order than the one computed by this
 *       class!
 */
case class WindowPathFinder(tac: TAC) extends AbstractPathFinder(tac) {

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
        // If there are multiple start sites, find the parent "if" or "switch" and use that as a start site
        var startSite: Option[Int] = None
        if (startSites.tail.nonEmpty) {
            var nextStmt = startSites.min
            while (nextStmt >= 0 && startSite.isEmpty) {
                cfg.code.instructions(nextStmt) match {
                    case iff: If[V] if startSites.contains(iff.targetStmt) => startSite = Some(nextStmt)
                    case _: Switch[V] =>
                        val (startSwitch, endSwitch, _) = processSwitch(nextStmt)
                        val isParentSwitch = startSites.forall {
                            nextStartSite => nextStartSite >= startSwitch && nextStartSite <= endSwitch
                        }
                        if (isParentSwitch) {
                            startSite = Some(nextStmt)
                        }
                    case _ =>
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
        if (csInfo.isEmpty) {
            Path(cfg.startBlock.startPC.until(cfg.code.instructions.last.pc).map(FlatPathElement.fromPC).toList)
        } else {
            // Otherwise, order the control structures and assign the corresponding path elements
            val orderedCS = hierarchicallyOrderControlStructures(csInfo)
            hierarchyToPath(orderedCS.hierarchy.head._2, startSite.get, endSite)
        }
    }
}
