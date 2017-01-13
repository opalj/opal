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
import scala.collection.JavaConverters._

import org.opalj.collection.mutable.UShortSet

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
 *          [[org.opalj.br.ExceptionHandler]].
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
) {

    /*
    // 1. Check that each basic block has a lower start pc than the end pc
    //    i.e., startPC <= endPC.
    assert(
        basicBlocks.forall { bb ⇒ bb == null || bb.startPC <= bb.endPC },
        basicBlocks.filter(bb ⇒ bb != null && bb.startPC > bb.endPC).mkString
    )

    // 2. Check that each pc belonging to a basic block (bb) actually points to the respective bb
    //    i.e., pc in basicBlock : bb => basicBlock(pc) == bb.
    assert(
        basicBlocks.filter(_ != null).toSet.forall { bb ⇒
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
    assert(
        basicBlocks.filter(_ != null).toSet.forall { bb ⇒
            bb.successors.forall { successorBB ⇒
                (successorBB.isBasicBlock && {
                    val succBB = successorBB.asBasicBlock
                    (basicBlocks(succBB.startPC) eq succBB) && (basicBlocks(succBB.endPC) eq succBB)
                }) ||
                    (successorBB.isCatchNode && catchNodes.contains(successorBB.asCatchNode)) ||
                    successorBB.isExitNode
            }
        },
        basicBlocks.filter(_ != null).
            map(bb ⇒ bb.toString+" => "+bb.successors.mkString(", ")).
            mkString("unexpected successors:\n\t", "\n\t", "")
    )
    assert(
        basicBlocks.filter(_ != null).toSet.forall { bb ⇒
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
    assert(
        basicBlocks.
            filter(bb ⇒ bb != null && bb.successors.exists { _.isCatchNode }).
            flatMap(bb ⇒ bb.successors.collect { case cn: CatchNode ⇒ cn }).
            forall(catchBB ⇒ catchNodes.contains(catchBB)),
        catchNodes.mkString("the set of catch nodes {", ", ", "} is incomplete:\n") +
            (basicBlocks.filter(_ != null).collect {
                case bb if bb.successors.exists(succBB ⇒ succBB.isCatchNode && !catchNodes.contains(succBB)) ⇒
                    s"$bb => ${bb.successors.collect { case cn: CatchNode ⇒ cn }.mkString(", ")}"
            }).mkString("\n")
    )

    // 5.   Check that predecessors and successors are consistent.
    assert(
        basicBlocks.filter(bb ⇒ bb != null).toSet.
            forall(bb ⇒ bb.successors.forall { succBB ⇒ succBB.predecessors.contains(bb) }),
        "successors and predecessors are inconsistent; e.g., "+
            basicBlocks.filter(bb ⇒ bb != null).toSet.
            find(bb ⇒ !bb.successors.forall { succBB ⇒ succBB.predecessors.contains(bb) }).
            map(bb ⇒ bb.successors.find(succBB ⇒ !succBB.predecessors.contains(bb)).map(succBB ⇒
                s"$succBB is a successor of $bb, but does not list it as a predecessor").get).get
    )
    assert(
        basicBlocks.filter(bb ⇒ bb != null).toSet.
            forall(bb ⇒ bb.predecessors.forall { predBB ⇒ predBB.successors.contains(bb) }),
        "predecessors and successors are inconsistent; e.g., "+
            basicBlocks.filter(bb ⇒ bb != null).toSet.
            find(bb ⇒ !bb.predecessors.forall { predBB ⇒ predBB.successors.contains(bb) }).
            map(bb ⇒ bb.predecessors.find(predBB ⇒ !predBB.successors.contains(bb)).map(predBB ⇒
                s"predBB is a predecessor of $bb, but does not list it as a successor").get).get
    )
    */

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
    def bb(pc: PC): BasicBlock = basicBlocks(pc)

    /**
     * Returns the set of all reachable [[CFGNode]]s of the control flow graph.
     */
    lazy val reachableBBs: SomeSet[CFGNode] = basicBlocks(0).reachable(reflexive = true)

    /**
     * Iterates over the set of all [[BasicBlock]]s. (I.e., the exit and catch nodes are
     * not returned.)
     */
    lazy val allBBs: Iterator[BasicBlock] = {
        //basicBlocks.view.filter(_ ne null).toSet
        new Iterator[BasicBlock] {

            var currentStartPC = 0

            def hasNext: Boolean = currentStartPC < basicBlocks.length

            def next: BasicBlock = {
                val current = basicBlocks(currentStartPC)
                currentStartPC = current.endPC + 1
                while (currentStartPC < basicBlocks.length && (basicBlocks(currentStartPC) eq null)) {
                    currentStartPC += 1
                }
                current
            }
        }
    }

    /**
     * Returns all direct runtime successors of the instruction with the given pc.
     *
     * If the returned set is empty, then the instruction is either a return instruction or an
     * instruction that always causes an exception to be thrown that is not handled by
     * a handler of the respective method.
     *
     * @note If possible the function `foreachSuccessor` should be used as it does not have
     *         to create comparatively expensive intermediate data structures.
     *
     * @param pc A valid pc of an instruction of the code block from which this cfg was derived.
     */
    def successors(pc: PC): Set[PC] /* IMPROVE Use (refactored) UShortSet */ = {
        val bb = this.bb(pc)
        if (bb.endPC > pc) {
            // it must be - w.r.t. the code array - the next instruction
            Set(code.instructions(pc).indexOfNextInstruction(pc)(code))
        } else {
            // the set of successor can be (at the same time) a RegularBB or an ExitNode
            bb.successors.collect {
                case bb: BasicBlock ⇒ bb.startPC
                case cb: CatchNode  ⇒ cb.handlerPC
            }
        }
    }

    /**
     * Iterates over the direct successors of the instruction with the given pc and calls the given
     * function `f` for each successor. `f` is guaranteed to be called only once for each successor
     * instruction. (E.g., relevant in case of a switch where multiple cases are handled in the
     * same way.)
     */
    def foreachSuccessor(pc: PC)(f: PC ⇒ Unit): Unit = {
        val bb = this.bb(pc)
        if (bb.endPC > pc) {
            // it must be - w.r.t. the code array - the next instruction
            f(code.instructions(pc).indexOfNextInstruction(pc)(code))
        } else {
            // the set of successor can be (at the same time) a RegularBB or an ExitNode
            var visited = UShortSet.empty
            bb.successors foreach { bb ⇒
                val nextPC =
                    if (bb.isBasicBlock) bb.asBasicBlock.startPC
                    else if (bb.isCatchNode) bb.asCatchNode.handlerPC
                    else -1
                if (nextPC != -1 && !visited.contains(nextPC)) {
                    visited = nextPC +≈: visited
                    f(nextPC)
                }
                // else if (bb.isExitNode)... is not relevant
            }
        }
    }

    def predecessors(pc: PC): Set[PC] /* IMPROVE Use (refactored) UShortSet */ = {
        if (pc == 0)
            return Set.empty;

        val bb = this.bb(pc)
        if (bb.startPC == pc) {
            bb.predecessors flatMap {
                case bb: BasicBlock ⇒ Set(bb.endPC)
                case cn: CatchNode  ⇒ cn.predecessors.map(_.asBasicBlock.endPC)
            }
        } else {
            Set(code.pcOfPreviousInstruction(pc))
        }
    }

    def foreachPredecessor(pc: PC)(f: PC ⇒ Unit): Unit = {
        if (pc == 0)
            return ;

        val bb = this.bb(pc)
        if (bb.startPC == pc) {
            var visited = UShortSet.empty
            bb.predecessors foreach { bb ⇒
                if (bb.isBasicBlock) {
                    f(bb.asBasicBlock.endPC)
                } else if (bb.isCatchNode) {
                    bb.asCatchNode.predecessors foreach { predBB ⇒
                        val nextPC = predBB.asBasicBlock.endPC
                        if (!visited.contains(nextPC)) {
                            visited = nextPC +≈: visited
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
     */
    def mapPCsToIndexes(pcToIndex: Array[PC], lastIndex: Int): CFG = {

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
            val endIndex = pcToIndex(oldBB.endPC)
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
                if ((tempBB ne null)) {
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

        // update the catch nodes
        val codeSize = code.instructions.length
        catchNodes.foreach { cn ⇒
            bbMapping.put(
                cn,
                new CatchNode(
                    startPC = pcToIndex(cn.startPC),
                    endPC = if (cn.endPC == codeSize) lastIndex + 1 else pcToIndex(cn.endPC),
                    handlerPC = pcToIndex(cn.handlerPC),
                    cn.catchType
                )
            )
        }

        val newNormalReturnNode = new ExitNode(normalReturn = true)
        bbMapping.put(normalReturnNode, newNormalReturnNode)
        val newAbnormalReturnNode = new ExitNode(normalReturn = false)
        bbMapping.put(abnormalReturnNode, newAbnormalReturnNode)

        // rewire the graph

        bbMapping.keySet().asScala.foreach { oldBB ⇒
            val newBB = bbMapping.get(oldBB)
            oldBB.successors.foreach { oldSuccBB ⇒
                val newSuccBB = bbMapping.get(oldSuccBB)
                assert(newSuccBB ne null, s"no mapping for $oldSuccBB")
                newBB.addSuccessor(newSuccBB)
                // Instead of iterating over the predecessors, we just iterate over
                // the successors; this way we only include the node that are
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

    //
    // Visualization

    override def toString: String = {
        //        code:                    Code,
        //        normalReturnNode:        ExitNode,
        //        abnormalReturnNode:      ExitNode,
        //        catchNodes:              Seq[CatchNode],
        //        private val basicBlocks: Array[BasicBlock]

        val bbIds: Map[CFGNode, Int] = (
            basicBlocks.filter(_ ne null).toSet +
            normalReturnNode + abnormalReturnNode ++ catchNodes
        ).zipWithIndex.toMap

        "CFG("+
            bbIds.map { bbId ⇒
                val (bb, id) = bbId
                s"$id: $bb"+bb.successors.map(bbIds(_)).mkString("=>{", ", ", "}")
            }.mkString("\n\t", "\n\t", "\n\t") +
            s"normalReturnNode=${bbIds(normalReturnNode)}:$normalReturnNode\n\t"+
            s"abnormalReturnNode=${bbIds(abnormalReturnNode)}:$abnormalReturnNode\n\t"+
            s"catchNodes=${catchNodes.mkString("{", ", ", "}")}\n"+
            ")"
    }

    def toDot: String = {
        val rootNodes = Set(startBlock) ++ catchNodes
        org.opalj.graphs.toDot(rootNodes)
    }
}
