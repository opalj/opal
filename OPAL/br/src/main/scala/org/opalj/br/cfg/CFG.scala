/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
 * @param code The code for which the CFG was build.
 * @param normalReturnNode The unique exit node of the control flow graph if the
 * 		method returns normally. If the method always throws an exception this
 * 		node will not have any predecessors.
 * @param abnormalReturnNode The unique exit node of the control flow graph if the
 * 		method returns abnormally (throws an exception). If the method is guaranteed
 * 		to never throw an exception, this node will not have any predecessors.
 * @param catchNodes List of all catch nodes. (Usually, we have one [[CatchNode]] per
 * 		[[org.opalj.br.ExceptionHandler]].
 * @param basicBlocks An implicit map between a program counter and its associated
 * 		[[BasicBlock]]; it may be a sparse array!
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
     * Iterates over all runtime successors of the instruction with the given pc.
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
     * Iterates over the successors of the instruction with the given pc and calls the given
     * function `f` for each successor.
     */
    def foreachSuccessor(pc: PC)(f: PC ⇒ Unit): Unit = {
        val bb = this.bb(pc)
        if (bb.endPC > pc) {
            // it must be - w.r.t. the code array - the next instruction
            f(code.instructions(pc).indexOfNextInstruction(pc)(code))
        } else {
            // the set of successor can be (at the same time) a RegularBB or an ExitNode
            var visited = UShortSet.empty
            bb.successors.foreach { bb ⇒
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
            bb.predecessors.flatMap {
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
            bb.predecessors.foreach { bb ⇒
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
     * @param 	lastIndex The index of the last instruction of the underlying (non-empty) code array.
     * 			I.e., if the instruction array contains one instruction then the `lastIndex` has to be
     * 			`0`.
     */
    def mapPCsToIndexes(pcToIndex: Array[PC], lastIndex: Int): CFG = {

        val bbMapping = new IdentityHashMap[CFGNode, CFGNode]()

        val newBasicBlocks = new Array[BasicBlock](lastIndex + 1)
        var lastNewBB: BasicBlock = null
        var startIndex = 0
        val requiresNewStartBlock = pcToIndex(0) > 0
        if (requiresNewStartBlock) {
            // we have added instructions at the beginning which belong to a new start bb
            lastNewBB = new BasicBlock(0)
            startIndex = pcToIndex(0)
            lastNewBB.endPC = startIndex - 1
            Arrays.fill(newBasicBlocks.asInstanceOf[Array[Object]], 0, startIndex, lastNewBB)
        }
        var startPC = 0
        val max = basicBlocks.length
        do {
            var oldBB = basicBlocks(startPC)
            startIndex = pcToIndex(startPC)
            lastNewBB = new BasicBlock(startIndex)

            // find the start pc of the next bb that is ALIVE (!)
            var nextStartPC = oldBB.endPC
            do {
                bbMapping.put(oldBB, lastNewBB)
                nextStartPC += 1
                while (nextStartPC < max && {
                    val nextOldBB = basicBlocks(nextStartPC)
                    (nextOldBB eq null) || (nextOldBB eq oldBB)
                }) {
                    nextStartPC += 1
                }
                if (nextStartPC < max)
                    oldBB = basicBlocks(nextStartPC)
                // repeat this loop while we see dead basic blocks (i.e., we have actually found
                // code which cannot be reached on any path; this is a frequent issue with older
                // compilers or foreign language compilers such as the groovy compiler)
            } while (nextStartPC < max && pcToIndex(nextStartPC) == 0)

            val endIndex = if (nextStartPC < max) pcToIndex(nextStartPC) - 1 else lastIndex
            lastNewBB.endPC = endIndex
            Arrays.fill(newBasicBlocks.asInstanceOf[Array[Object]], startIndex, endIndex + 1, lastNewBB)
            startPC = nextStartPC
            startIndex = endIndex + 1
        } while (startPC < max)

        if (startIndex < lastIndex)
            Arrays.fill(newBasicBlocks.asInstanceOf[Array[Object]], startIndex, lastIndex, lastNewBB)

        if (requiresNewStartBlock) {
            newBasicBlocks(0).addSuccessor(newBasicBlocks(pcToIndex(0)))
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

        // rewire the graph

        val newNormalReturnNode = new ExitNode(normalReturn = true)
        bbMapping.put(normalReturnNode, newNormalReturnNode)
        normalReturnNode.successors.foreach { bb ⇒
            newNormalReturnNode.addSuccessor(bbMapping.get(bb))
        }

        val newAbnormalReturnNode = new ExitNode(normalReturn = false)
        bbMapping.put(abnormalReturnNode, newAbnormalReturnNode)
        abnormalReturnNode.successors.foreach { bb ⇒
            newAbnormalReturnNode.addSuccessor(bbMapping.get(bb))
        }

        bbMapping.keySet().asScala.foreach { oldBB ⇒
            val newBB = bbMapping.get(oldBB)
            oldBB.successors.foreach { oldSuccBB ⇒
                val newSuccBB = bbMapping.get(oldSuccBB)
                assert(newSuccBB ne null, s"no mapping for $oldSuccBB")
                newBB.addSuccessor(newSuccBB)
            }
            oldBB.predecessors.foreach { oldPredBB ⇒
                val newPredBB = bbMapping.get(oldPredBB)
                assert(newPredBB ne null, s"no mapping for $oldPredBB")
                newBB.addPredecessor(newPredBB)
            }
        }

        val newCatchNodes = catchNodes.map(bbMapping.get(_).asInstanceOf[CatchNode])
        assert(newCatchNodes.forall { _ ne null })
        val newCFG = CFG(
            code,
            newNormalReturnNode,
            newAbnormalReturnNode,
            newCatchNodes,
            newBasicBlocks
        )

        // let's see if we can merge the first two basic blocks
        if (requiresNewStartBlock && basicBlocks(0).predecessors.isEmpty) {
            val secondBB = newBasicBlocks(0).successors.head.asBasicBlock
            val firstBB = newBasicBlocks(0)
            firstBB.endPC = secondBB.endPC
            firstBB.setSuccessors(secondBB.successors)
        }

        newCFG
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
