/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package br
package cfg

import java.util.Arrays
import java.util.IdentityHashMap

import scala.collection.{Set ⇒ SomeSet}
import scala.collection.AbstractIterator
import net.ceedubs.ficus.Ficus._

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.info
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntTrieSet1
import org.opalj.graphs.DefaultMutableNode
import org.opalj.graphs.Node

/**
 * Represents the control flow graph of a method.
 *
 * To compute a `CFG` use the [[CFGFactory]].
 *
 * ==Thread-Safety==
 * This class is thread-safe; all data is effectively immutable
 * '''after construction''' time.
 *
 * @param   code The code for which the CFG was build.
 * @param   normalReturnNode The unique exit node of the control flow graph if the
 *          method returns normally. If the method always throws an exception this
 *          node will not have any predecessors.
 * @param   abnormalReturnNode The unique exit node of the control flow graph if the
 *          method returns abnormally (throws an exception). If the method is guaranteed
 *          to never throw an exception, this node will not have any predecessors.
 * @param   catchNodes List of all catch nodes. (Usually, we have one [[CatchNode]] per
 *          [[org.opalj.br.ExceptionHandler]], but if an exception handler does not catch
 *          anything, no [[CatchNode]] is created.)
 * @param   basicBlocks An implicit map between a program counter and its associated
 *          [[BasicBlock]]; it may be a sparse array!
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
case class CFG(
        code:                    Code,
        normalReturnNode:        ExitNode,
        abnormalReturnNode:      ExitNode,
        catchNodes:              Seq[CatchNode],
        private val basicBlocks: Array[BasicBlock]
) { cfg ⇒

    if (CFG.CheckConsistency) {
        val allBBs = basicBlocks.filter(_ != null)
        val allBBsSet = allBBs.toSet
        // 1. Check that each basic block has a lower start pc than the end pc
        //    i.e., startPC <= endPC.
        check(
            allBBs.forall(bb ⇒ bb.startPC <= bb.endPC),
            allBBs.filter(bb ⇒ bb.startPC > bb.endPC).mkString
        )

        // 2. Check that each pc belonging to a basic block (bb) actually points to the respective bb
        //    i.e., pc in basicBlock : bb => basicBlock(pc) == bb.
        check(
            allBBsSet.forall { bb ⇒
                (bb.startPC to bb.endPC).forall { pc ⇒
                    (basicBlocks(pc) eq null) || (basicBlocks(pc) eq bb)
                }
            },
            basicBlocks.zipWithIndex.filter(_._1 != null).
                map(bb ⇒ s"${bb._2}:${bb._1}#${System.identityHashCode(bb._1).toHexString}").
                mkString("basic blocks mapping broken:\n\t", ",\n\t", "\n")
        )

        // 3. Check that the CFG is self-consistent; i.e., that no node references a node
        //    that does not occur in the BB.
        check(
            allBBsSet.forall { bb ⇒
                bb.successors.forall { successorBB ⇒
                    (successorBB.isBasicBlock && {
                        val succBB = successorBB.asBasicBlock
                        (basicBlocks(succBB.startPC) eq succBB) && (basicBlocks(succBB.endPC) eq succBB)
                    }) ||
                        (successorBB.isCatchNode && catchNodes.contains(successorBB.asCatchNode)) ||
                        successorBB.isExitNode
                }
            },
            allBBs.
                map(bb ⇒ bb.toString+" => "+bb.successors.mkString(", ")).
                mkString("unexpected successors:\n\t", "\n\t", "")
        )
        check(
            allBBsSet.forall { bb ⇒
                bb.predecessors.forall { predecessorBB ⇒
                    (
                        predecessorBB.isBasicBlock && {
                            val predBB = predecessorBB.asBasicBlock
                            (basicBlocks(predBB.startPC) eq predBB) && (basicBlocks(predBB.endPC) eq predBB)
                        }
                    ) ||
                        (predecessorBB.isCatchNode && catchNodes.contains(predecessorBB.asCatchNode))
                }
            },
            basicBlocks.zipWithIndex.filter(_._1 != null).map(_.swap).
                map(bb ⇒ bb._1+":"+bb._2.toString+" predecessors: "+bb._2.predecessors.mkString(", ")).
                mkString("unexpected predecessors:\n\t", "\n\t", s"\ncode:$code")
        )

        // 4.  Check that all catch nodes referred to by the basic blocks are listed in the
        //     sequence of catch nodes
        check(
            allBBs.
                filter(bb ⇒ bb.successors.exists { _.isCatchNode }).
                flatMap(bb ⇒ bb.successors.collect { case cn: CatchNode ⇒ cn }).
                forall(catchBB ⇒ catchNodes.contains(catchBB)),
            catchNodes.mkString("the set of catch nodes {", ", ", "} is incomplete:\n") +
                (allBBs.collect {
                    case bb if bb.successors.exists(succBB ⇒ succBB.isCatchNode && !catchNodes.contains(succBB)) ⇒
                        s"$bb => ${bb.successors.collect { case cn: CatchNode ⇒ cn }.mkString(", ")}"
                }).mkString("\n")
        )

        // 5.   Check that predecessors and successors are consistent.
        check(
            allBBsSet.
                forall(bb ⇒ bb.successors.forall { succBB ⇒ succBB.predecessors.contains(bb) }),
            "successors and predecessors are inconsistent; e.g., "+
                allBBsSet.
                find(bb ⇒ !bb.successors.forall { succBB ⇒ succBB.predecessors.contains(bb) }).
                map(bb ⇒ bb.successors.find(succBB ⇒ !succBB.predecessors.contains(bb)).map(succBB ⇒
                    s"$succBB is a successor of $bb, but does not list it as a predecessor").get).get
        )
        check(
            allBBsSet.
                forall(bb ⇒ bb.predecessors.forall { predBB ⇒ predBB.successors.contains(bb) }),
            "predecessors and successors are inconsistent; e.g., "+
                allBBsSet.
                find(bb ⇒ !bb.predecessors.forall { predBB ⇒ predBB.successors.contains(bb) }).
                map(bb ⇒ bb.predecessors.find(predBB ⇒ !predBB.successors.contains(bb)).map(predBB ⇒
                    s"predBB is a predecessor of $bb, but does not list it as a successor").get).get
        )
    }

    /**
     * The basic block associated with the very first instruction.
     */
    final def startBlock: BasicBlock = basicBlocks(0)

    /**
     * Returns the basic block to which the instruction with the given `pc` belongs.
     *
     * @param pc A valid pc.
     * @return The basic block associated with the given `pc`. If the `pc` is not valid
     *         `null` is returned or an index out of bounds exception is thrown.
     */
    def bb(pc: Int): BasicBlock = basicBlocks(pc)

    /**
     * Returns the set of all reachable [[CFGNode]]s of the control flow graph.
     */
    lazy val reachableBBs: SomeSet[CFGNode] = basicBlocks(0).reachable(reflexive = true)

    /**
     * Iterates over the set of all [[BasicBlock]]s. (I.e., the exit and catch nodes are
     * not returned.) Always returns the basic block containing the first instruction first.
     */
    def allBBs: Iterator[BasicBlock] = {
        new AbstractIterator[BasicBlock] {

            private[this] var currentBBPC = 0

            def hasNext: Boolean = currentBBPC < basicBlocks.length

            def next: BasicBlock = {
                val basicBlocks = cfg.basicBlocks
                val current = basicBlocks(currentBBPC)
                currentBBPC = current.endPC + 1
                // jump to the end and check if the instruction directly following this bb
                // actually belongs to a basic block
                val maxPC = basicBlocks.length
                while (currentBBPC < maxPC && (basicBlocks(currentBBPC) eq null)) {
                    currentBBPC += 1
                }
                current
            }
        }
    }

    def allNodes: Iterator[CFGNode] = {
        allBBs ++ catchNodes.iterator ++ Iterator(normalReturnNode, abnormalReturnNode)
    }

    /**
     * Returns all direct runtime successors of the instruction with the given pc.
     *
     * If the returned set is empty, then the instruction is either a return instruction or an
     * instruction that always causes an exception to be thrown that is not handled by
     * a handler of the respective method.
     *
     * @note   If possible the function `foreachSuccessor` should be used as it does not have
     *         to create comparatively expensive intermediate data structures.
     *
     * @param pc A valid pc of an instruction of the code block from which this cfg was derived.
     */
    def successors(pc: Int): IntTrieSet = {
        val bb = this.bb(pc)
        if (bb.endPC > pc) {
            // it must be - w.r.t. the code array - the next instruction
            IntTrieSet1(code.instructions(pc).indexOfNextInstruction(pc)(code))
        } else {
            // the set of successor can be (at the same time) a RegularBB or an ExitNode
            var successorPCs = IntTrieSet.empty
            bb.successors foreach {
                case bb: BasicBlock ⇒ successorPCs += bb.startPC
                case cb: CatchNode  ⇒ successorPCs += cb.handlerPC
                case _              ⇒
            }
            successorPCs
        }
    }

    /**
     * Iterates over the direct successors of the instruction with the given pc and calls the given
     * function `f` for each successor. `f` is guaranteed to be called only once for each successor
     * instruction. (E.g., relevant in case of a switch where multiple cases are handled in the
     * same way.)
     */
    def foreachSuccessor(pc: Int)(f: Int ⇒ Unit): Unit = {
        val bb = this.bb(pc)
        if (bb.endPC > pc) {
            // it must be - w.r.t. the code array - the next instruction
            f(code.instructions(pc).indexOfNextInstruction(pc)(code))
        } else {
            // the set of successor can be (at the same time) a RegularBB or an ExitNode
            var visited = IntTrieSet.empty
            bb.successors foreach { bb ⇒
                val nextPC =
                    if (bb.isBasicBlock) bb.asBasicBlock.startPC
                    else if (bb.isCatchNode) bb.asCatchNode.handlerPC
                    else -1
                if (nextPC != -1 && !visited.contains(nextPC)) {
                    visited += nextPC
                    f(nextPC)
                }
                // else if (bb.isExitNode)... is not relevant
            }
        }
    }

    def predecessors(pc: Int): IntTrieSet = {
        if (pc == 0)
            return IntTrieSet.empty;

        val bb = this.bb(pc)
        if (bb.startPC == pc) {
            var predecessorPCs = IntTrieSet.empty
            bb.predecessors foreach {
                case bb: BasicBlock ⇒
                    predecessorPCs += bb.endPC
                case cn: CatchNode ⇒
                    cn.predecessors.foreach { bb ⇒
                        predecessorPCs += bb.asBasicBlock.endPC
                    }
            }
            predecessorPCs
        } else {
            IntTrieSet1(code.pcOfPreviousInstruction(pc))
        }
    }

    def foreachPredecessor(pc: Int)(f: Int ⇒ Unit): Unit = {
        if (pc == 0)
            return ;

        val bb = this.bb(pc)
        if (bb.startPC == pc) {
            var visited = IntTrieSet.empty
            bb.predecessors foreach { bb ⇒
                if (bb.isBasicBlock) {
                    f(bb.asBasicBlock.endPC)
                } else if (bb.isCatchNode) {
                    bb.asCatchNode.predecessors foreach { predBB ⇒
                        val nextPC = predBB.asBasicBlock.endPC
                        if (!visited.contains(nextPC)) {
                            visited += nextPC
                            f(nextPC)
                        }
                    }
                }
            }
        } else {
            f(code.pcOfPreviousInstruction(pc))
        }
    }

    /**
     * Creates a new CFG where the boundaries of the basic blocks are updated given the `pcToIndex`
     * mapping. The assumption is made that the indexes are continuous.
     * If the first index (i.e., `pcToIndex(0)` is not 0, then a new basic block for the indexes
     * in {0,pcToIndex(0)} is created if necessary.
     *
     * @param  lastIndex The index of the last instruction of the underlying (non-empty) code array.
     *         I.e., if the instruction array contains one instruction then the `lastIndex` has
     *         to be `0`.
     * @param  singletonBBsExpander Function called for each basic block which encompasses a single
     *         instruction to expand the BB to encompass more instructions. This supports the
     *         case where an instruction was transformed in a way that resulted in multiple
     *         instructions/statements, but which all belong to the same basic block.
     *         ''This situation cannot be handled using pcToIndex.''
     *         This information is used to ensure that - if a basic block which currently just
     *         encompasses a single instruction will encompass the new and the old instruction afterwards.
     *         The returned value will be used as the `endIndex.`
     *         `endIndex = singletonBBsExpander(pcToIndex(pc of singleton bb))`
     *         Hence, the function is given the mapped index has to return that value if the index
     *         does not belong to the expanded instruction.
     */
    def mapPCsToIndexes(
        pcToIndex:            Array[Int /*PC*/ ],
        singletonBBsExpander: Int /*PC*/ ⇒ Int,
        lastIndex:            Int
    ): CFG = {

        /*
        // [USED FOR DEBUGGING PURPOSES] *********************************************************
        println(
            basicBlocks.
                filter(_ != null).
                toSet.
                map((bb: BasicBlock) ⇒ bb.toString+" => "+bb.successors.mkString(", ")).
                mkString("Successors:\n", "\n", "\n")
        )
        println(
            basicBlocks.
                filter(_ != null).
                toSet.
                map((bb: BasicBlock) ⇒ bb.predecessors.mkString(", ")+" => "+bb.toString).
                mkString("Predecessors:\n", "\n", "\n")
        )
        println(catchNodes.mkString("CatchNodes:", ",", "\n"))
        println(pcToIndex.zipWithIndex.map(_.swap).mkString("Mapping:", ",", "\n"))
        //
        // ********************************************************* [USED FOR DEBUGGING PURPOSES]
        */

        val bbsLength = basicBlocks.length
        val bbMapping = new IdentityHashMap[CFGNode, CFGNode]()

        val newBasicBlocks = new Array[BasicBlock](lastIndex + 1)
        val newBasicBlocksArray = newBasicBlocks.asInstanceOf[Array[Object]]
        val requiresNewStartBlock = pcToIndex(0) > 0

        var lastNewBB: BasicBlock = null
        if (requiresNewStartBlock) {
            val endIndex = pcToIndex(0) - 1
            // we have added instructions at the beginning which belong to a new start bb
            lastNewBB = new BasicBlock(startPC = 0, _endPC = endIndex)
            Arrays.fill(newBasicBlocksArray, 0, endIndex + 1, lastNewBB)
        }
        var startPC = 0
        do {
            val oldBB = basicBlocks(startPC)
            val startIndex = pcToIndex(startPC)
            val endIndex = {
                val endIndexCandidate = pcToIndex(oldBB.endPC)
                if (startIndex == endIndexCandidate) {
                    singletonBBsExpander(startIndex)
                } else {
                    endIndexCandidate
                }
            }
            lastNewBB = new BasicBlock(startIndex, endIndex)
            bbMapping.put(oldBB, lastNewBB)
            Arrays.fill(newBasicBlocksArray, startIndex, endIndex + 1, lastNewBB)
            // let's advance startPC to the next instruction which is live (which has a BB)
            startPC = oldBB.endPC + 1
            var tempBB: BasicBlock = null
            while (startPC < bbsLength && {
                tempBB = basicBlocks(startPC)
                (tempBB eq null) || pcToIndex(tempBB.startPC) < 0
            }) {
                assert(tempBB ne oldBB)
                // This (index < 0) handles the case where the initial CFG was created using
                // a simple algorithm that actually resulted in a CFG with detached basic blocks;
                // we now kill these basic blocks by jumping over them!
                // NOTE: This is indicative of dead code in the bytecode in the first place!
                if (tempBB ne null) {
                    startPC = tempBB.endPC
                }
                startPC += 1
            }
        } while (startPC < bbsLength)

        if (requiresNewStartBlock) {
            val firstBB = newBasicBlocks(0)
            val secondBB = newBasicBlocks(pcToIndex(0))
            firstBB.addSuccessor(secondBB)
            secondBB.addPredecessor(firstBB)
        }

        // add the catch nodes
        val codeSize = code.instructions.length
        catchNodes foreach { cn ⇒
            val newCN = cn.copy(
                startPC = pcToIndex(cn.startPC),
                endPC = if (cn.endPC == codeSize) lastIndex + 1 else pcToIndex(cn.endPC),
                handlerPC = pcToIndex(cn.handlerPC)
            )
            bbMapping.put(cn, newCN)
        }

        val newNormalReturnNode = new ExitNode(normalReturn = true)
        bbMapping.put(normalReturnNode, newNormalReturnNode)
        val newAbnormalReturnNode = new ExitNode(normalReturn = false)
        bbMapping.put(abnormalReturnNode, newAbnormalReturnNode)

        // rewire the graph
        val oldBBToNewBBIt = bbMapping.entrySet().iterator()
        while (oldBBToNewBBIt.hasNext) {
            val oldBBToNewBB = oldBBToNewBBIt.next()
            val oldBB = oldBBToNewBB.getKey
            val newBB = oldBBToNewBB.getValue
            oldBB.successors foreach { oldSuccBB ⇒
                val newSuccBB = bbMapping.get(oldSuccBB)
                assert(newSuccBB ne null, s"no mapping for $oldSuccBB")
                newBB.addSuccessor(newSuccBB)
                // Instead of iterating over the predecessors, we just iterate over
                // the successors; this way, we only include the nodes that are
                // live; nodes that; e.g., are attached to the exit node but for
                // which there is no path to reach them at all are dropped!
                newSuccBB.addPredecessor(newBB)
            }
        }

        val newCatchNodes = catchNodes.map(bbMapping.get(_).asInstanceOf[CatchNode])
        assert(newCatchNodes.forall { _ ne null })

        // let's see if we can merge the first two basic blocks
        if (requiresNewStartBlock && basicBlocks(0).predecessors.isEmpty) {
            val firstBB = newBasicBlocks(0)
            val secondBB = firstBB.successors.head.asBasicBlock
            val newFirstBB = secondBB.copy(startPC = 0, predecessors = Set.empty)
            newFirstBB.successors.foreach(succBB ⇒ succBB.updatePredecessor(secondBB, newFirstBB))
            Arrays.fill(newBasicBlocksArray, 0, secondBB._endPC + 1 /* (exclusive)*/ , newFirstBB)
        }

        CFG(code, newNormalReturnNode, newAbnormalReturnNode, newCatchNodes, newBasicBlocks)
    }

    // ---------------------------------------------------------------------------------------------
    //
    // Visualization & Debugging
    //
    // ---------------------------------------------------------------------------------------------

    override def toString: String = {
        //        code:                    Code,
        //        normalReturnNode:        ExitNode,
        //        abnormalReturnNode:      ExitNode,
        //        catchNodes:              Seq[CatchNode],
        //        private val basicBlocks: Array[BasicBlock]

        val cfgNodes: Set[CFGNode] =
            basicBlocks.filter(_ ne null).toSet[CFGNode] ++
                catchNodes +
                normalReturnNode +
                abnormalReturnNode

        val bbIds: Map[CFGNode, Int] = cfgNodes.zipWithIndex.toMap

        bbIds.map { bbId ⇒
            val (bb, id) = bbId
            if (bb.isExitNode) {
                s"BB_${id.toHexString}: $bb"
            } else {
                bb.successors.
                    map(succBB ⇒ "BB_"+bbIds(succBB).toHexString).
                    mkString(s"BB_${id.toHexString}: $bb → {", ",", "}")
            }
        }.toList.sorted.mkString("CFG(\n\t", "\n\t", "\n)")
    }

    def toDot: String = {
        val rootNodes = Set(startBlock) ++ catchNodes
        org.opalj.graphs.toDot(rootNodes)
    }

    def toDot(f: BasicBlock ⇒ String): Iterable[Node] = {
        // 1. create a node foreach cfg node
        val bbsIterator = allBBs
        val startBB = bbsIterator.next()
        val startNodeVisualProperties = Map("fillcolor" → "green", "style" → "filled", "shape" → "box")
        var cfgNodeToGNodes: Map[CFGNode, DefaultMutableNode[String]] =
            Map(
                startBB →
                    new DefaultMutableNode(f(startBB), theVisualProperties = startNodeVisualProperties)
            )
        cfgNodeToGNodes ++= bbsIterator.map(bb ⇒ (bb, new DefaultMutableNode(f(bb))))
        cfgNodeToGNodes ++= catchNodes.map(cn ⇒ (cn, new DefaultMutableNode(cn.toString)))
        cfgNodeToGNodes += (
            abnormalReturnNode →
            new DefaultMutableNode(
                "abnormal return", theVisualProperties = abnormalReturnNode.visualProperties
            )
        )
        cfgNodeToGNodes += (
            normalReturnNode →
            new DefaultMutableNode(
                "return", theVisualProperties = normalReturnNode.visualProperties
            )
        )

        // 2. reconnect nodes
        cfgNodeToGNodes foreach { cfgNodeToGNode ⇒
            val (cfgNode, gNode) = cfgNodeToGNode
            cfgNode.successors foreach { cfgNode ⇒ gNode.addChild(cfgNodeToGNodes(cfgNode)) }
        }

        val nodes = cfgNodeToGNodes.values
        nodes
    }
}

object CFG {

    final val CheckConsistencyKey = "org.opalj.br.debug.cfg.CFG.consistency"

    final val CheckConsistency: Boolean = {
        implicit val logContext = GlobalLogContext
        if (BaseConfig.as[Option[Boolean]](CheckConsistencyKey).getOrElse(false)) {
            info("OPAL", s"org.opalj.br.cfg.CFG: validation on (setting: $CheckConsistencyKey)")
            true
        } else {
            info("OPAL", s"org.opalj.br.cfg.CFG: validation off (setting: $CheckConsistencyKey)")
            false
        }
    }

}
