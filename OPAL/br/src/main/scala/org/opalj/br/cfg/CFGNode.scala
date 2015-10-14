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
package org.opalj.br.cfg

import scala.collection.mutable
import org.opalj.graphs.Node

/**
 * The common super trait of all nodes belonging to a method's control flow graph.
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
trait CFGNode extends Node {

    //
    // MANAGING PREDECESSORS
    //

    private[this] var _predecessors: Set[CFGNode] = Set.empty

    private[cfg] def addPredecessor(predecessor: CFGNode): Unit = {
        //  if (predecessor eq this) throw new IllegalArgumentException()
        _predecessors = _predecessors + predecessor
    }
    private[cfg] def setPredecessors(predecessors: Set[CFGNode]): Unit = {
        _predecessors = predecessors
    }

    private[cfg] def updatePredecessor(oldBB: CFGNode, newBB: CFGNode): Unit = {
        _predecessors = _predecessors - (oldBB) + (newBB)
    }

    def predecessors: Set[CFGNode] = _predecessors

    //
    // MANAGING SUCCESSORS
    //

    final override def hasSuccessors: Boolean = _successors.nonEmpty

    final override def foreachSuccessor(f: Node ⇒ Unit): Unit = _successors foreach f

    private[this] var _successors: Set[CFGNode] = Set.empty

    private[cfg] def addSuccessor(successor: CFGNode): Unit = {
        //  if (successor eq this) throw new IllegalArgumentException(s"$this => $successor")
        _successors = _successors + successor
    }
    private[cfg] def setSuccessors(successors: Set[CFGNode]): Unit = {
        this._successors = successors
    }

    def successors: Set[CFGNode] = _successors

    //
    // GENERIC QUERY METHODS
    //

    private[cfg] def reachable(reachable: mutable.Set[CFGNode]): Unit = {
        _successors.
            filterNot { d ⇒ reachable.contains(d) }.
            foreach { d ⇒ reachable += d; d.reachable(reachable) }
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

}
