/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.preprocessing

import org.opalj.br.cfg.CatchNode
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.ExitNode
import org.opalj.collection.mutable.IntArrayStack

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * An approach based on an a naive / intuitive traversing of the control flow graph.
 *
 * @author Patrick Mell
 */
class DefaultPathFinder extends AbstractPathFinder {

    /**
     * This implementation finds all paths based on an a naive / intuitive traversing of the `cfg`
     * and, based on that, determines in what relation a statement / instruction is with its
     * predecessors / successors.
     * The paths contain all instructions, not only those that modify a [[StringBuilder]] /
     * [[StringBuffer]] object.
     * For this implementation, `endSite` is not required, thus passing any value is fine.
     *
     * @see [[AbstractPathFinder.findPaths]]
     */
    override def findPaths(startSites: List[Int], cfg: CFG[Stmt[V], TACStmts[V]]): Path = {
        // path will accumulate all paths
        val path = ListBuffer[SubPath]()
        var stack = IntArrayStack.fromSeq(startSites.reverse)
        val seenElements = ListBuffer[Int]()
        // For storing the node IDs of all seen catch nodes (they are to be used only once, thus
        // this store)
        val seenCatchNodes = mutable.Map[Int, Unit.type]()
        // numSplits serves a queue that stores the number of possible branches (or successors)
        val numSplits = ListBuffer[Int]()
        // Also a queue that stores the indices of which branch of a conditional to take next
        val currSplitIndex = ListBuffer[Int]()
        val numBackedgesLoop = ListBuffer[Int]()
        val backedgeLoopCounter = ListBuffer[Int]()
        // Used to quickly find the element at which to insert a sub path
        val nestedElementsRef = ListBuffer[NestedPathElement]()
        val natLoops = cfg.findNaturalLoops()

        // Multiple start sites => We start within a conditional => Prepare for that
        if (startSites.size > 1) {
            val outerNested =
                generateNestPathElement(startSites.size, NestedPathType.CondWithAlternative)
            numSplits.append(startSites.size)
            currSplitIndex.append(0)
            nestedElementsRef.append(outerNested)
            path.append(outerNested)
        }

        while (stack.nonEmpty) {
            val popped = stack.pop()
            val bb = cfg.bb(popped)
            val isLoopHeader = isHeadOfLoop(popped, natLoops, cfg)
            var isLoopEnding = false
            var loopEndingIndex = -1
            var belongsToLoopEnding = false
            var belongsToLoopHeader = false

            // Append everything of the current basic block to the path
            for (i ← bb.startPC.to(bb.endPC)) {
                seenElements.append(i)
                val toAppend = FlatPathElement(i)

                if (!isLoopEnding) {
                    isLoopEnding = isEndOfLoop(cfg.bb(i).endPC, natLoops)
                }

                // For loop headers, insert a new nested element (and thus, do the housekeeping)
                if (!belongsToLoopHeader && isHeadOfLoop(i, natLoops, cfg)) {
                    numSplits.prepend(1)
                    currSplitIndex.prepend(0)
                    numBackedgesLoop.prepend(bb.predecessors.size - 1)
                    backedgeLoopCounter.prepend(0)

                    val outer = generateNestPathElement(0, NestedPathType.Repetition)
                    outer.element.append(toAppend)
                    nestedElementsRef.prepend(outer)
                    path.append(outer)

                    belongsToLoopHeader = true
                } // For loop ending, find the top-most loop from the stack and add to that element
                else if (isLoopEnding) {
                    val loopElement = nestedElementsRef.find {
                        _.elementType match {
                            case Some(et) ⇒ et == NestedPathType.Repetition
                            case _        ⇒ false
                        }
                    }
                    if (loopElement.isDefined) {
                        if (!belongsToLoopEnding) {
                            backedgeLoopCounter(0) += 1
                            if (backedgeLoopCounter.head == numBackedgesLoop.head) {
                                loopEndingIndex = nestedElementsRef.indexOf(loopElement.get)
                            }
                        }
                        loopElement.get.element.append(toAppend)
                    }
                    belongsToLoopEnding = true
                } // The instructions belonging to a loop header are stored in a flat structure
                else if (!belongsToLoopHeader && (numSplits.isEmpty || bb.predecessors.size > 1)) {
                    path.append(toAppend)
                } // Within a nested structure => append to an inner element
                else {
                    // For loops
                    var ref: NestedPathElement = nestedElementsRef.head
                    // Refine for conditionals and try-catch(-finally)
                    ref.elementType match {
                        case Some(t) if t == NestedPathType.CondWithAlternative ||
                            t == NestedPathType.TryCatchFinally ⇒
                            ref = ref.element(currSplitIndex.head).asInstanceOf[NestedPathElement]
                        case _ ⇒
                    }
                    ref.element.append(toAppend)
                }
            }

            // Find all regular successors (excluding CatchNodes)
            val successors = bb.successors.filter {
                case _: ExitNode   ⇒ false
                case cn: CatchNode ⇒ cn.catchType.isDefined
                case _             ⇒ true
            }.map {
                case cn: CatchNode ⇒ cn.handlerPC
                case s             ⇒ s.nodeId
            }.toList.sorted
            val catchSuccessors = bb.successors.filter { s ⇒
                s.isInstanceOf[CatchNode] && !seenCatchNodes.contains(s.nodeId)
            }
            val successorsToAdd = successors.filter { next ⇒
                !seenElements.contains(next) && !stack.contains(next)
            }
            val hasSeenSuccessor = successors.foldLeft(false) {
                (old: Boolean, next: Int) ⇒ old || seenElements.contains(next)
            }

            // Clean a loop from the stacks if the end of a loop was reached
            if (loopEndingIndex != -1) {
                numSplits.remove(loopEndingIndex)
                currSplitIndex.remove(loopEndingIndex)
                nestedElementsRef.remove(loopEndingIndex)
                numBackedgesLoop.remove(0)
                backedgeLoopCounter.remove(0)
            }

            // At the join point of a branching, do some housekeeping
            if (currSplitIndex.nonEmpty &&
                ((bb.predecessors.size > 1 && !isLoopHeader) || hasSeenSuccessor)) {
                currSplitIndex(0) += 1
                if (currSplitIndex.head == numSplits.head) {
                    numSplits.remove(0)
                    currSplitIndex.remove(0)
                    nestedElementsRef.remove(0)
                }
            }

            if (numSplits.nonEmpty && (bb.predecessors.size == 1)) {
                // Within a conditional, prepend in order to keep the correct order
                val newStack = IntArrayStack.fromSeq(stack.reverse)
                newStack.push(IntArrayStack.fromSeq(successorsToAdd.reverse))
                stack = newStack
            } else {
                // Otherwise, append (also retain the correct order)
                val newStack = IntArrayStack.fromSeq(successorsToAdd.reverse)
                newStack.push(IntArrayStack.fromSeq(stack.reverse))
                stack = newStack
            }
            // On a split point, prepare the next (nested) element (however, not for loop headers),
            // this includes if a node has a catch node as successor
            if ((successorsToAdd.length > 1 && !isLoopHeader) || catchSuccessors.nonEmpty) {
                seenCatchNodes ++= catchSuccessors.map(n ⇒ (n.nodeId, Unit))
                val appendSite = if (numSplits.isEmpty) path else
                    nestedElementsRef(currSplitIndex.head).element
                var relevantNumSuccessors = successors.size
                var ifWithElse = true

                if (isCondWithoutElse(popped, cfg)) {
                    // If there are catch node successors, the number of relevant successor equals
                    // the number of successors (because catch node are excluded here)
                    if (catchSuccessors.isEmpty) {
                        relevantNumSuccessors -= 1
                    }
                    ifWithElse = false
                }

                val outerNestedType = if (catchSuccessors.nonEmpty) NestedPathType.TryCatchFinally
                else if (ifWithElse) NestedPathType.CondWithAlternative
                else NestedPathType.CondWithoutAlternative
                val outerNested = generateNestPathElement(relevantNumSuccessors, outerNestedType)

                numSplits.prepend(relevantNumSuccessors)
                currSplitIndex.prepend(0)
                nestedElementsRef.prepend(outerNested)
                appendSite.append(outerNested)
            }
        }

        Path(path.toList)
    }

}
