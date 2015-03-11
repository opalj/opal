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
package ai
package domain

import org.opalj.collection.mutable.UShortSet
import org.opalj.graphs.Node
import org.opalj.graphs.DefaultMutableNode
import org.opalj.br.instructions.ReturnInstruction
import org.opalj.br.instructions.ATHROW
import org.opalj.br.PC
import org.opalj.graphs.MutableNode

/**
 * Records the abstract interpretation time control-flow graph (CFG).
 * This CFG is always a sound approximation.
 *
 * ==Usage==
 * This domain can be stacked on top of other traits that handle [[flow]]s. It requires
 * that it sees all calls to [[flow]].
 *
 * ==Core Properties==
 *  - Thread-safe: '''No'''.
 *  - Reusable: '''No'''; state directly associated with the analyzed code block is
 *          collected. Hence, a new instance of the domain needs to be created per
 *          analyzed method.
 *
 * @author Michael Eichberg
 */
trait RecordCFG extends CoreDomainFunctionality { domain: TheCode ⇒

    private[this] val regularSuccessors =
        new Array[UShortSet](domain.code.instructions.size)

    private[this] val exceptionHandlerSuccessors =
        new Array[UShortSet](domain.code.instructions.size)

    /**
     * Returns the program counter(s) of the instruction(s) that is(are) executed next if
     * the evaluation of this instruction may succeed without raising an exception.
     *
     * The returned set is always empty for `return` instructions. It is also empty for
     * instructions that always throw an exception (e.g., an integer value that is divided
     * by zero will always result in a NullPointException.)
     *
     * @note The [[org.opalj.br.instructions.ATHROW]] instruction will never have a
     *      `regularSuccessor`. The `return` instructions will never have any successors.
     */
    def regularSuccessorsOf(pc: PC): PCs = {
        val s = regularSuccessors(pc)
        if (s != null) s else UShortSet.empty
    }

    /**
     * Returns the program counter(s) of the instruction(s) that is(are) executed next if
     * the evaluation of this instruction may raise an exception.
     *
     * The returned set is always empty for instructions that cannot raise exceptions,
     * such as the `StackManagementInstruction`s.
     *
     * @note The [[org.opalj.br.instructions.ATHROW]] has successors if and only if the
     *      thrown exception is directly handled inside this code block.
     */
    def exceptionHandlerSuccessorsOf(pc: PC): PCs = {
        val s = exceptionHandlerSuccessors(pc)
        if (s != null) s else UShortSet.empty
    }

    /**
     * Returns the set of all instructions executed after the instruction with the
     * given `pc`. If this set is empty, either the instruction belongs to dead code,
     * the instruction is a `return` instruction or the `instruction` throws an exception
     * that is never handled internally.
     *
     * @note The set is recalculated on demand.
     */
    def allSuccessorsOf(pc: PC): PCs =
        regularSuccessorsOf(pc) ++ exceptionHandlerSuccessorsOf(pc)

    /**
     * Returns `true` if the instruction with the given pc has multiple predecessors.
     *
     * @note This function calculates the respective information on demand by traversing
     * the successors.
     */
    def hasMultipleRegularPredecessors(pc: PC): Boolean = {
        var predecessors = 0
        var i = domain.code.instructions.size - 1
        while (i >= 0) {
            val successors = regularSuccessors(i)
            if ((successors ne null) && successors.contains(pc)) {
                predecessors += 1
                if (predecessors > 1)
                    return true;
            }
            i -= 1
        }
        false
    }

    /**
     * @inheritdoc
     *
     * @note This method is called by the abstract interpretation framework.
     */
    abstract override def flow(
        currentPC: PC,
        successorPC: PC,
        isExceptionalControlFlow: Boolean,
        abruptSubroutineTerminationCount: Int,
        wasJoinPerformed: Boolean,
        worklist: List[PC],
        operandsArray: OperandsArray,
        localsArray: LocalsArray,
        tracer: Option[AITracer]): List[PC] = {

        val allSuccessors =
            if (isExceptionalControlFlow)
                domain.exceptionHandlerSuccessors
            else
                domain.regularSuccessors

        val successorsOfPC = allSuccessors(currentPC)
        if (successorsOfPC == null)
            allSuccessors(currentPC) = UShortSet(successorPC)
        else
            allSuccessors(currentPC) = successorPC +≈: successorsOfPC

        super.flow(
            currentPC, successorPC,
            isExceptionalControlFlow, abruptSubroutineTerminationCount,
            wasJoinPerformed,
            worklist,
            operandsArray, localsArray,
            tracer)
    }

    def cfgAsGraph(): DefaultMutableNode[List[PC]] = {
        val code = this.code
        val nodes = new Array[DefaultMutableNode[List[PC]]](code.instructions.size)
        val nodePredecessorsCount = new Array[Int](code.instructions.size)
        // 1. create nodes
        for (pc ← code.programCounters) {
            nodes(pc) = {
                var visualProperties = Map("shape" -> "box", "labelloc" -> "l")

                if (domain.code.instructions(pc).isInstanceOf[ReturnInstruction]) {
                    visualProperties += "fillcolor" -> "green"
                    visualProperties += "style" -> "filled"
                } else if (domain.code.instructions(pc).isInstanceOf[ATHROW.type]) {
                    visualProperties += "fillcolor" -> "yellow"
                    visualProperties += "style" -> "filled"
                } else if (allSuccessorsOf(pc).isEmpty) {
                    visualProperties += "fillcolor" -> "red"
                    visualProperties += "style" -> "filled"
                    visualProperties += "shape" -> "octagon"
                }

                if (code.exceptionHandlersFor(pc).nonEmpty) {
                    visualProperties += "color" -> "orange"
                }

                if (code.exceptionHandlers.exists { eh ⇒ eh.handlerPC == pc }) {
                    visualProperties += "peripheries" -> "2"
                }

                def pcsToString(pcs: List[PC]): String = {
                    def pcToString(pc: PC): String = {
                        val ln = code.lineNumber(pc).map(ln ⇒ s"[ln=$ln]").getOrElse("")
                        pc + ln+": "+domain.code.instructions(pc).toString(pc)
                    }
                    pcs.map(pcToString(_)).mkString("", "\\l\\l", "\\l")
                }

                new DefaultMutableNode(
                    List(pc),
                    pcsToString,
                    visualProperties,
                    List.empty[DefaultMutableNode[List[PC]]])
            }
        }
        // 2. create edges
        for (pc ← code.programCounters; succPC ← allSuccessorsOf(pc)) {
            nodes(pc).addChild(nodes(succPC))
            nodePredecessorsCount(succPC) += 1
        }

        // 3. fold nodes
        // Nodes that have only one successor and where the successor has only one
        // predecessors are merged into one node
        for (pc ← code.programCounters) {
            val currentNode = nodes(pc)
            if (currentNode.hasOneChild) {
                val successorNode = currentNode.firstChild
                val successorNodePC = successorNode.identifier.head
                if (nodePredecessorsCount(successorNodePC) == 1) {
                    currentNode.updateIdentifier(
                        currentNode.identifier ++ currentNode.firstChild.identifier
                    )
                    currentNode.mergeVisualProperties(successorNode.visualProperties)
                    currentNode.removeLastAddedChild() // the only child...
                    currentNode.addChildren(successorNode.children)
                    nodes(successorNodePC) = currentNode
                }
            }
        }

        nodes(0)
    }
}

