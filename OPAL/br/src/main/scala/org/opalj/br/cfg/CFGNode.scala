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
package org.opalj.br
package cfg

import scala.collection.mutable
import org.opalj.graphs.Node

/**
 * The common super trait of all nodes belonging to a method's control flow graph.
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
trait CFGNode extends Node {

    def isBasicBlock: Boolean
    def asBasicBlock: BasicBlock = throw new ClassCastException();

    def isCatchNode: Boolean
    def asCatchNode: CatchNode = throw new ClassCastException();

    def isExitNode: Boolean
    def isAbnormalReturnExitNode: Boolean
    def isNormalReturnExitNode: Boolean

    def isStartOfSubroutine: Boolean

    //
    // MANAGING PREDECESSORS
    //

    private[this] var _predecessors: Set[CFGNode] = Set.empty

    def addPredecessor(predecessor: CFGNode): Unit = {
        //  if (predecessor eq this) throw new IllegalArgumentException()
        _predecessors += predecessor
    }
    def addPredecessors(predecessor: TraversableOnce[CFGNode]): Unit = {
        //  if (predecessor eq this) throw new IllegalArgumentException()
        _predecessors ++= predecessor
    }
    private[cfg] def setPredecessors(predecessors: Set[CFGNode]): Unit = {
        _predecessors = predecessors
    }
    def removePredecessor(predecessor: CFGNode): Unit = {
        _predecessors -= predecessor
    }
    private[cfg] def clearPredecessors(): Unit = {
        _predecessors = Set.empty
    }

    private[cfg] def updatePredecessor(oldBB: CFGNode, newBB: CFGNode): Unit = {
        _predecessors = _predecessors - (oldBB) + (newBB)
    }

    def predecessors: Set[CFGNode] = _predecessors

    //
    // MANAGING SUCCESSORS
    //

    final override def hasSuccessors: Boolean = _successors.nonEmpty

    /**
     * Returns `true` if the last instruction of this basic block throws/may throw an exception;
     * whether the exception is handled or not is not relevant!
     */
    def mayThrowException: Boolean = {
        _successors.exists(successor ⇒ successor.isCatchNode || successor.isAbnormalReturnExitNode)
    }

    final override def foreachSuccessor(f: Node ⇒ Unit): Unit = _successors foreach f

    private[this] var _successors: Set[CFGNode] = Set.empty

    def addSuccessor(successor: CFGNode): Unit = {
        //  if (successor eq this) throw new IllegalArgumentException(s"$this => $successor")
        _successors = _successors + successor
    }
    private[cfg] def setSuccessors(successors: Set[CFGNode]): Unit = {
        this._successors = successors
    }
    private[cfg] def clearSuccessors(): Unit = {
        _successors = Set.empty
    }

    def successors: Set[CFGNode] = _successors

    //
    // GENERIC QUERY METHODS
    //

    private[cfg] def reachable(reachable: mutable.Set[CFGNode]): Unit = {
        // the following
        //_successors.
        //filterNot(reachable.contains).
        //foreach { d ⇒ reachable += d; d.reachable(reachable) }

        var remainingSuccessors = this._successors
        while (remainingSuccessors.nonEmpty) {
            val successor = remainingSuccessors.head
            remainingSuccessors = remainingSuccessors.tail
            if (reachable.add(successor)) {
                for {
                    nextSuccessor ← successor.successors
                    if !remainingSuccessors.contains(nextSuccessor)
                    if !reachable.contains(nextSuccessor)
                } {
                    remainingSuccessors += nextSuccessor
                }
            }
        }
    }

    /**
     * Computes the set of all [[CFGNode]]s that are reachable from this one.
     *
     * @note The result is not cached.
     */
    def reachable(reflexive: Boolean = false): mutable.Set[CFGNode] = {
        val reachable = mutable.HashSet.empty[CFGNode]
        if (reflexive) reachable += this
        this.reachable(reachable)
        reachable
    }

    /**
     * Computes the (current) set of all [[BasicBlock]]s that belong to this current subroutine and
     * which have no successors.
     *
     * @note  The result is not cached.
     * @note  This method is primarily (exclusively?) intended to be used to complete a call
     *        graph containing subroutines.
     */
    private[cfg] def subroutineFrontier(code: Code, bbs: Array[BasicBlock]): List[BasicBlock] = {
        assert(this.isStartOfSubroutine)

        var frontier: List[BasicBlock] = Nil

        val seen = mutable.HashSet[CFGNode](this)
        var worklist: List[CFGNode] = List(this)
        while (worklist.nonEmpty) {
            val bb = worklist.head
            worklist = worklist.tail

            val successors = bb.successors
            if (successors.isEmpty) {
                if (bb.isBasicBlock) frontier = bb.asBasicBlock :: frontier
            } else {
                successors.foreach { succBB ⇒
                    var nextBB = succBB
                    // The basic block to which a subroutine returns to cannot be the start
                    // of a subroutine because a subroutine's code is never reached via a
                    // normal control flow...(it may however contain a call to a subroutine!)
                    if (nextBB.isStartOfSubroutine) {
                        val jsrPC = bb.asBasicBlock.endPC /*the jsr instruction...*/
                        nextBB = bbs(code.pcOfNextInstruction(jsrPC))
                    }
                    if (!seen.contains(nextBB)) {
                        seen += nextBB
                        worklist = nextBB :: worklist
                    }
                }
            }
        }

        frontier
    }

}
