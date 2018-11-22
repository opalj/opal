/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.preprocessing

import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGNode
import org.opalj.fpcf.analyses.string_definition.Path
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * [[SubPath]] represents the general item that forms a [[Path]].
 */
sealed class SubPath()

/**
 * A flat element, e.g., for representing a single statement. The statement is identified by
 * `element`.
 */
case class FlatPathElement(element: Int) extends SubPath

/**
 * Identifies the nature of a nested path element.
 */
object NestedPathType extends Enumeration {
    val Loop, Conditional = Value
}

/**
 * A nested path element, that is, items can be used to form arbitrary structures / hierarchies.
 * `element` holds all child elements. Path finders should set the `elementType` property whenever
 * possible, i.e., when they compute / have this information.
 */
case class NestedPathElement(
        element:     ListBuffer[SubPath],
        elementType: Option[NestedPathType.Value]
) extends SubPath

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
     */
    protected def isHeadOfLoop(site: Int, loops: List[List[Int]]): Boolean =
        loops.foldLeft(false)((old: Boolean, nextLoop: List[Int]) ⇒ old || nextLoop.head == site)

    /**
     * Determines whether a given `site` is the end of a loop by comparing it to a set of loops
     * (here a list of lists). This function returns ''true'', if `site` is the last element of one
     * of the inner lists.
     */
    protected def isEndOfLoop(site: Int, loops: List[List[Int]]): Boolean =
        loops.foldLeft(false)((old: Boolean, nextLoop: List[Int]) ⇒ old || nextLoop.last == site)

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
        val successors = cfg.bb(branchingSite).successors.map(_.nodeId).toArray.sorted
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
                if (to.contains(cfg.bb(lastEle))) {
                    return true
                }
                seenNodes.append(from)
                toVisitStack.pushAll(to.filter(!seenNodes.contains(_)))
            }
            return false
        } > 1
    }

    /**
     * Implementations of this function find all paths starting from the sites, given by
     * `startSites`, within the provided control flow graph, `cfg`. As this is executed within the
     * context of a string definition analysis, implementations are free to decide whether they
     * include only statements that work on [[StringBuffer]] / [[StringBuilder]] or include all
     * statements in the paths.
     *
     * @param startSites A list of possible start sites, that is, initializations. Several start
     *                  sites denote that an object is initialized within a conditional.
     * @param endSite An end site which an implementation might use to early-stop the procedure.
     *                This site can be the read operation of interest, for instance.
     * @param cfg The underlying control flow graph which servers as the basis to find the paths.
     * @return Returns all found paths as a [[Path]] object. That means, the return object is a flat
     *         structure, however, captures all hierarchies and (nested) flows. Note that a
     *         [[NestedPathElement]] with only one child can either refer to a loop or an ''if''
     *         that has no ''else'' block (from a high-level perspective). It is the job of the
     *         implementations to attach these information to [[NestedPathElement]]s (so that
     *         procedures using results of this function do not need to re-process).
     */
    def findPaths(startSites: List[Int], endSite: Int, cfg: CFG[Stmt[V], TACStmts[V]]): Path

}
