/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.preprocessing

import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CatchNode
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGNode
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.tac.If
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * [[AbstractPathFinder]] provides a scaffolding for finding all relevant paths in a CFG in the
 * scope of string definition analyses.
 *
 * @author Patrick Mell
 */
trait AbstractPathFinder {

    /**
     * Generates a new [[NestedPathElement]] with a given number of inner [[NestedPathElement]]s.
     */
    protected def generateNestPathElement(
        numInnerElements: Int,
        elementType:      NestedPathType.Value
    ): NestedPathElement = {
        val outerNested = NestedPathElement(ListBuffer(), Some(elementType))
        for (_ ← 0.until(numInnerElements)) {
            outerNested.element.append(NestedPathElement(ListBuffer(), None))
        }
        outerNested
    }

    /**
     * Determines whether a given `site` is the head of a loop by comparing it to a set of loops
     * (here a list of lists). This function returns ''true'', if `site` is the head of one of the
     * inner lists.
     * Note that some high-level constructs, such as ''while-true'', might produce a loop where the
     * check, whether to loop again or leave the loop, is placed at the end of the loop. In such
     * cases, the very first statement of a loop is considered its head (which can be an assignment
     * or function call not related to the loop header for instance).
     */
    protected def isHeadOfLoop(
        site: Int, loops: List[List[Int]], cfg: CFG[Stmt[V], TACStmts[V]]
    ): Boolean = {
        var belongsToLoopHeader = false

        // First, check the trivial case: Is the given site the first statement in a loop (covers,
        // e.g., the above-mentioned while-true cases)
        loops.foreach { loop ⇒
            if (!belongsToLoopHeader) {
                if (loop.head == site) {
                    belongsToLoopHeader = true
                }
            }
        }

        // The loop header might not only consist of the very first element in 'loops'; thus, check
        // whether the given site is between the first site of a loop and the site of the very first
        // if (again, respect structures as produces by while-true loops)
        if (!belongsToLoopHeader) {
            loops.foreach { nextLoop ⇒
                // The second condition is to regard only those elements as headers which  have a
                // backedge
                if (!belongsToLoopHeader && cfg.bb(site).asBasicBlock.predecessors.size > 1) {
                    val start = nextLoop.head
                    var end = start
                    while (!cfg.code.instructions(end).isInstanceOf[If[V]]) {
                        end += 1
                    }
                    if (site >= start && site <= end && end < nextLoop.last) {
                        belongsToLoopHeader = true
                    }
                }
            }
        }
        belongsToLoopHeader
    }

    /**
     * Determines whether a given `site` is the end of a loop by comparing it to a set of loops
     * (here a list of lists). This function returns ''true'', if `site` is the last element of one
     * of the inner lists.
     */
    protected def isEndOfLoop(site: Int, loops: List[List[Int]]): Boolean =
        loops.foldLeft(false)((old: Boolean, nextLoop: List[Int]) ⇒ old || nextLoop.last == site)

    /**
     * Checks whether a given [[BasicBlock]] has one (or several) successors which have at least n
     * predecessors.
     *
     * @param bb The basic block to check whether it has a successor with at least n predecessors.
     * @param n The number of required predecessors.
     * @return Returns ''true'' if ''bb'' has a successor which has at least ''n'' predecessors.
     *
     * @note This function regards as successors and predecessors only [[BasicBlock]]s.
     */
    protected def hasSuccessorWithAtLeastNPredecessors(bb: BasicBlock, n: Int = 2): Boolean =
        bb.successors.filter(
            _.isInstanceOf[BasicBlock]
        ).foldLeft(false)((prev: Boolean, next: CFGNode) ⇒ {
                prev || (next.predecessors.count(_.isInstanceOf[BasicBlock]) >= n)
            })

    /**
     * This function checks if a branching corresponds to an if (or if-elseif) structure that has no
     * else block.
     * Currently, this function is implemented to check whether the very last element of the
     * successors of the given site is a path past the if (or if-elseif) paths.
     *
     * @param branchingSite The site / index of a branching that is to be checked.
     * @param cfg The control flow graph underlying the successors.
     * @return Returns ''true'', if the very last element of the successors is a child of one of the
     *         other successors. If this is the case, the branching corresponds to one without an
     *         ''else'' branch.
     */
    def isCondWithoutElse(branchingSite: Int, cfg: CFG[Stmt[V], TACStmts[V]]): Boolean = {
        val successorBlocks = cfg.bb(branchingSite).successors
        // CatchNode exists => Regard it as conditional without alternative
        if (successorBlocks.exists(_.isInstanceOf[CatchNode])) {
            return false
        }

        val successors = successorBlocks.map(_.nodeId).toArray.sorted
        // Separate the last element from all previous ones
        val branches = successors.reverse.tail.reverse
        val lastEle = successors.last

        // For every successor (except the very last one), execute a DFS to check whether the very
        // last element is a successor. If so, this represents a path past the if (or if-elseif).
        branches.count { next ⇒
            val seenNodes = ListBuffer[CFGNode](cfg.bb(branchingSite), cfg.bb(next))
            val toVisitStack = mutable.Stack[CFGNode](cfg.bb(next).successors.toArray: _*)
            while (toVisitStack.nonEmpty) {
                val from = toVisitStack.pop()
                val to = from.successors
                if (from.nodeId == lastEle || to.contains(cfg.bb(lastEle))) {
                    return true
                }
                seenNodes.append(from)
                toVisitStack.pushAll(to.filter(!seenNodes.contains(_)))
            }
            return false
        } > 1
    }

    /**
     * Implementations of this function find all paths, starting from the start node of the given
     * `cfg`, within the provided control flow graph, `cfg`. As this is executed within the
     * context of a string definition analysis, implementations are free to decide whether they
     * include only statements that work on [[StringBuffer]] / [[StringBuilder]] or include all
     * statements in the paths.
     *
     * @param cfg The underlying control flow graph which servers as the basis to find the paths.
     * @return Returns all found paths as a [[Path]] object. That means, the return object is a flat
     *         structure, however, captures all hierarchies and (nested) flows. Note that a
     *         [[NestedPathElement]] with only one child can either refer to a loop or an ''if''
     *         that has no ''else'' block (from a high-level perspective). It is the job of the
     *         implementations to attach these information to [[NestedPathElement]]s (so that
     *         procedures using results of this function do not need to re-process).
     */
    def findPaths(cfg: CFG[Stmt[V], TACStmts[V]]): Path

}
