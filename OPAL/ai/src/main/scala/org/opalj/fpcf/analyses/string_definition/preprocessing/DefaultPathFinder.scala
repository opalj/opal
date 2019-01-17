/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.preprocessing

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGNode
import org.opalj.tac.Goto
import org.opalj.tac.If
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

/**
 * An approach based on an a naive / intuitive traversing of the control flow graph.
 *
 * @author Patrick Mell
 */
class DefaultPathFinder extends AbstractPathFinder {

    /**
     * CSInfo stores information regarding control structures (CS) in the form: Index of the start
     * statement of that CS, index of the end statement of that CS and the type.
     */
    private type CSInfo = (Int, Int, NestedPathType.Value)
    private type CFGType = CFG[Stmt[V], TACStmts[V]]

    private def determineTypeOfIf(cfg: CFGType, stmtIndex: Int): NestedPathType.Value = {
        // Is the first condition enough to identify loops?
        if (isHeadOfLoop(stmtIndex, cfg.findNaturalLoops(), cfg)) {
            NestedPathType.Repetition
        } else if (isCondWithoutElse(stmtIndex, cfg)) {
            NestedPathType.CondWithoutAlternative
        } else {
            NestedPathType.CondWithAlternative
        }
    }

    private def getStartAndEndIndexOfLoop(headIndex: Int, cfg: CFGType): (Int, Int) = {
        var startIndex = -1
        var endIndex = -1
        val relevantLoop = cfg.findNaturalLoops().filter(_ ⇒
            isHeadOfLoop(headIndex, cfg.findNaturalLoops(), cfg))
        if (relevantLoop.nonEmpty) {
            startIndex = relevantLoop.head.head
            endIndex = relevantLoop.head.last
        }
        (startIndex, endIndex)
    }

    private def getStartAndEndIndexOfCondWithAlternative(
        branchingSite: Int, cfg: CFGType, processedIfs: mutable.Map[Int, Unit.type]
    ): (Int, Int) = {
        processedIfs(branchingSite) = Unit

        var endSite = -1
        val stack = mutable.Stack[Int](branchingSite)
        while (stack.nonEmpty) {
            val popped = stack.pop()
            val nextBlock = cfg.bb(popped).successors.map {
                case bb: BasicBlock ⇒ bb.startPC
                // Handle Catch Nodes?
                case _              ⇒ -1
            }.max
            var containsIf = false
            for (i ← cfg.bb(nextBlock).startPC.to(cfg.bb(nextBlock).endPC)) {
                if (cfg.code.instructions(i).isInstanceOf[If[V]]) {
                    processedIfs(i) = Unit
                    containsIf = true
                }
            }

            if (containsIf) {
                stack.push(nextBlock)
            } else {
                // Find the goto that points after the "else" part (the assumption is that this
                // goto is the very last element of the current branch
                endSite = cfg.code.instructions(nextBlock - 1).asGoto.targetStmt - 1
            }
        }

        (branchingSite, endSite)
    }

    private def getStartAndEndIndexOfCondWithoutAlternative(
        branchingSite: Int, cfg: CFGType, processedIfs: mutable.Map[Int, Unit.type]
    ): (Int, Int) = {
        // Find the index of very last element in the if block (here: The goto element; is it always
        // present?)
        val ifTarget = cfg.code.instructions(branchingSite).asInstanceOf[If[V]].targetStmt
        var endIndex = ifTarget
        do {
            endIndex -= 1
        } while (cfg.code.instructions(branchingSite).isInstanceOf[Goto])

        // It is now necessary to collect all ifs that belong to the whole if condition in the
        // high-level construct
        cfg.bb(ifTarget).predecessors.foreach {
            case pred: BasicBlock ⇒
                for (i ← pred.startPC.to(pred.endPC)) {
                    if (cfg.code.instructions(i).isInstanceOf[If[V]]) {
                        processedIfs(i) = Unit
                    }
                }
            // How about CatchNodes?
            case cn ⇒ println(cn)
        }

        (branchingSite, endIndex)
    }

    private def getTryCatchFinallyInfo(cfg: CFGType): List[CSInfo] = {
        // Stores the startPC as key and the index of the end of a catch (or finally if it is
        // present); a map is used for faster accesses
        val tryInfo = mutable.Map[Int, Int]()

        cfg.catchNodes.foreach { cn ⇒
            if (!tryInfo.contains(cn.startPC)) {
                val cnWithSameStartPC = cfg.catchNodes.filter(_.startPC == cn.startPC)
                // If there is only one CatchNode for a startPC, i.e., no finally, no other catches,
                // the end index can be directly derived from the successors
                if (cnWithSameStartPC.tail.isEmpty) {
                    tryInfo(cn.startPC) = cfg.bb(cn.endPC).successors.map {
                        case bb: BasicBlock ⇒ bb.startPC - 1
                        case _              ⇒ -1
                    }.max
                } // Otherwise, the largest handlerPC marks the end index
                else {
                    tryInfo(cn.startPC) = cnWithSameStartPC.map(_.handlerPC).max
                }
            }
        }

        tryInfo.map {
            case (key, value) ⇒ (key, value, NestedPathType.TryCatchFinally)
        }.toList
    }

    private def processBasicBlock(
        cfg: CFGType, stmt: Int, processedIfs: mutable.Map[Int, Unit.type]
    ): CSInfo = {
        val csType = determineTypeOfIf(cfg, stmt)
        val (startIndex, endIndex) = csType match {
            case NestedPathType.Repetition ⇒
                processedIfs(stmt) = Unit
                getStartAndEndIndexOfLoop(stmt, cfg)
            case NestedPathType.CondWithoutAlternative ⇒
                getStartAndEndIndexOfCondWithoutAlternative(stmt, cfg, processedIfs)
            // _ covers CondWithAlternative and TryCatchFinally, however, the latter one should
            // never be present as the element referring to stmts is / should be an If
            case _ ⇒
                getStartAndEndIndexOfCondWithAlternative(stmt, cfg, processedIfs)
        }
        (startIndex, endIndex, csType)
    }

    private def findControlStructures(cfg: CFGType): List[CSInfo] = {
        // foundCS stores all found control structures as a triple in the form (start, end, type)
        val foundCS = ListBuffer[CSInfo]()
        // For a fast loop-up which if statements have already been processed
        val processedIfs = mutable.Map[Int, Unit.type]()
        val startBlock = cfg.startBlock
        val stack = mutable.Stack[CFGNode](startBlock)
        val seenCFGNodes = mutable.Map[CFGNode, Unit.type]()
        seenCFGNodes(startBlock) = Unit

        while (stack.nonEmpty) {
            val next = stack.pop()
            seenCFGNodes(next) = Unit

            next match {
                case bb: BasicBlock ⇒
                    for (i ← bb.startPC.to(bb.endPC)) {
                        if (cfg.code.instructions(i).isInstanceOf[If[V]] &&
                            !processedIfs.contains(i)) {
                            foundCS.append(processBasicBlock(cfg, i, processedIfs))
                            processedIfs(i) = Unit
                        }
                    }
                case cn: CFGNode ⇒
                    println(cn)
                case _ ⇒
            }

            // Add unseen successors
            next.successors.filter(!seenCFGNodes.contains(_)).foreach(stack.push)
        }

        // Add try-catch information, sort everything in ascending order in terms of the startPC and
        // return
        foundCS.appendAll(getTryCatchFinallyInfo(cfg))
        foundCS.sortBy { case (start, _, _) ⇒ start }.toList
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
    override def findPaths(startSites: List[Int], endSite: Int, cfg: CFGType): Path = {
        val startPC = cfg.startBlock.startPC
        identity(startPC)
        val csInfo = findControlStructures(cfg)
        identity(csInfo)

        Path(List(FlatPathElement(0), FlatPathElement(1)))
    }

}
