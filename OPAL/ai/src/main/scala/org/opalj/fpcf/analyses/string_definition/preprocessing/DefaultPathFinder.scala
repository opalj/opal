/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.preprocessing

import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.br.cfg.CFG
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

/**
 * An approach based on an a naive / intuitive traversing of the control flow graph.
 *
 * @param cfg The control flow graph (CFG) on which this instance will operate on.
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
     * This function transforms a hierarchy into a [[Path]].
     *
     * @param topElements A list of the elements which are present on the top-most level in the
     *                    hierarchy.
     * @param startIndex `startIndex` serves as a way to build a path between the first statement
     *                   (which is not necessarily a control structure) and the very first control
     *                  structure. For example, assume that the first control structure begins at
     *                   statement 5. `startIndex` will then be used to fill the gap `startIndex`
     *                   and 5.
     * @param endIndex  `endIndex` serves as a way to build a path between the last statement of a
     *                control structure (which is not necessarily the end of a scope of interest,
     *                such as a method) and the last statement (e.g., in `cfg`).
     * @return Returns the transformed [[Path]].
     */
    private def hierarchyToPath(
        topElements: List[HierarchicalCSOrder], startIndex: Int, endIndex: Int
    ): Path = {
        val finalPath = ListBuffer[SubPath]()
        // For the outer-most call, this is not the start index of the last control structure but of
        // the start PC of the first basic block
        var indexLastCSEnd = startIndex

        // Recursively transform the hierarchies to paths
        topElements.foreach { nextTopEle ⇒
            // Build path up to the next control structure
            val nextCSStart = nextTopEle.hierarchy.head._1.get._1
            indexLastCSEnd.until(nextCSStart).foreach { i ⇒
                finalPath.append(FlatPathElement(i))
            }

            val children = nextTopEle.hierarchy.head._2
            if (children.isEmpty) {
                // Recursion anchor: Build path for the correct type
                val (subpath, _) = buildPathForElement(nextTopEle, fill = true)
                // Control structures consist of only one element (NestedPathElement), thus "head"
                // is enough
                finalPath.append(subpath.elements.head)
            } else {
                val startIndex = nextTopEle.hierarchy.head._1.get._1
                val endIndex = nextTopEle.hierarchy.head._1.get._2
                val childrenPath = hierarchyToPath(children, startIndex, endIndex)
                var insertIndex = 0
                val (subpath, startEndPairs) = buildPathForElement(nextTopEle, fill = false)
                // npe is the nested path element that was produced above (head is enough as this
                // list will always contain only one element, due to fill=false)
                val npe = subpath.elements.head.asInstanceOf[NestedPathElement]
                val isRepElement = npe.elementType.getOrElse(NestedPathType.TryCatchFinally) ==
                    NestedPathType.Repetition
                var lastInsertedIndex = 0
                childrenPath.elements.foreach { nextEle ⇒
                    if (isRepElement) {
                        npe.element.append(nextEle)
                    } else {
                        npe.element(insertIndex).asInstanceOf[NestedPathElement].element.append(
                            nextEle
                        )
                    }

                    lastInsertedIndex = nextEle match {
                        case fpe: FlatPathElement     ⇒ fpe.element
                        case inner: NestedPathElement ⇒ Path.getLastElementInNPE(inner).element
                        // Compiler wants it but should never be the case!
                        case _                        ⇒ -1
                    }
                    if (lastInsertedIndex >= startEndPairs(insertIndex)._2) {
                        insertIndex += 1
                    }
                }
                // Fill the current NPE if necessary
                val currentToInsert = ListBuffer[FlatPathElement]()
                if (insertIndex < startEndPairs.length) {
                    currentToInsert.appendAll((lastInsertedIndex + 1).to(
                        startEndPairs(insertIndex)._2
                    ).map(FlatPathElement))
                    if (isRepElement) {
                        npe.element.appendAll(currentToInsert)
                    } else {
                        var insertPos = npe.element(insertIndex).asInstanceOf[NestedPathElement]
                        insertPos.element.appendAll(currentToInsert)
                        insertIndex += 1
                        // Fill the rest NPEs if necessary
                        insertIndex.until(startEndPairs.length).foreach { i ⇒
                            insertPos = npe.element(i).asInstanceOf[NestedPathElement]
                            insertPos.element.appendAll(
                                startEndPairs(i)._1.to(startEndPairs(i)._2).map(FlatPathElement)
                            )
                        }
                    }
                }
                finalPath.append(subpath.elements.head)
            }
            indexLastCSEnd = nextTopEle.hierarchy.head._1.get._2 + 1
        }

        finalPath.appendAll(indexLastCSEnd.to(endIndex).map(FlatPathElement))
        Path(finalPath.toList)
    }

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
        val csInfo = findControlStructures()
        // In case the are no control structures, return a path from the first to the last element
        if (csInfo.isEmpty) {
            val indexLastStmt = cfg.code.instructions.length
            Path(cfg.startBlock.startPC.until(indexLastStmt).map(FlatPathElement).toList)
        } // Otherwise, order the control structures and assign the corresponding path elements
        else {
            val lastStmtIndex = cfg.code.instructions.length - 1
            val orderedCS = hierarchicallyOrderControlStructures(csInfo)
            hierarchyToPath(orderedCS.hierarchy.head._2, cfg.startBlock.startPC, lastStmtIndex)
        }
    }

}
