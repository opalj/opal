/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package preprocessing

trait PathFinder {
    def containsComplexControlFlowForValue(value: V, tac: TAC): Boolean

    def findPath(value: V, tac: TAC): Path
}

/**
 * Implements very simple path finding, by checking if the given TAC may contain any control structures and creating a
 * path from the very first to the very last statement of the underlying CFG.
 */
object SimplePathFinder extends PathFinder {

    def containsComplexControlFlowForValue(value: V, tac: TAC): Boolean = {
        tac.stmts.exists {
            case _: If[V]     => true
            case _: Switch[V] => true
            case _: Throw[V]  => true
            case _: Goto      => true
            case _: JSR       => true
            case _            => false
        } || tac.cfg.catchNodes.nonEmpty
    }

    /**
     * Always returns a path from the first to the last statement pc in the CFG of the given TAC
     */
    def findPath(value: V, tac: TAC): Path = {
        val cfg = tac.cfg
        Path(cfg.startBlock.startPC.until(cfg.code.instructions.last.pc).map(PathElement.fromPC).toList)
    }
}
