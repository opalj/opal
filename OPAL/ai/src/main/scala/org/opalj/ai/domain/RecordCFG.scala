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
import org.opalj.br.instructions.Instruction
import org.opalj.br.PC
import org.opalj.br.Code
import org.opalj.graphs.MutableNode
import scala.collection.BitSet

/**
 * Records the abstract interpretation time control-flow graph (CFG).
 * This CFG is always a sound approximation.
 *
 * ==Usage (Mixin-Composition Order)==
 * This domain overrides the `flow` method and requires that it is mixed in before every
 * other domain that overrides the `flow` method and which may manipulate the `worklist`.
 * E.g., the mixin order should be:
 * {{{ class MyDomain extends Domain with RecordCFG with FlowManipulatingDomain }}}
 * If the mixin order is not correct, the CFG may not be complete.
 *
 * ==Core Properties==
 *  - Thread-safe: '''No'''.
 *  - Reusable: '''Yes'''; all state directly associated with the analyzed code block is
 *          reset by the method `initProperties`.
 *
 * @author Michael Eichberg
 */
trait RecordCFG
        extends CoreDomainFunctionality
        with CustomInitialization { domain: TheCode ⇒

    private[this] var regularSuccessors: Array[UShortSet] = _
    private[this] var exceptionHandlerSuccessors: Array[UShortSet] = _

    abstract override def initProperties(
        code:             Code,
        joinInstructions: BitSet,
        initialLocals:    Locals
    ): Unit = {

        val codeSize = code.instructions.size
        regularSuccessors = new Array[UShortSet](codeSize)
        exceptionHandlerSuccessors = new Array[UShortSet](codeSize)

        super.initProperties(code, joinInstructions, initialLocals)
    }

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
        if (s != null) s else NoPCs
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
        if (s != null) s else NoPCs
    }

    /**
     * Tests if the instruction with the given `pc` has a successor instruction with
     * `pc'` that satisfies the given predicate `p`.
     */
    def hasSuccessor(pc: PC, regularSuccessorsOnly: Boolean, p: PC ⇒ Boolean): Boolean = {
        var visitedSuccessors = UShortSet(pc)
        var successorsToVisit = successorsOf(pc, regularSuccessorsOnly)
        while (successorsToVisit.nonEmpty) {
            if (successorsToVisit.exists { succPC ⇒ p(succPC) })
                return true;

            visitedSuccessors = visitedSuccessors ++ successorsToVisit
            successorsToVisit =
                successorsToVisit.foldLeft(UShortSet.empty) { (l, r) ⇒
                    l ++ (successorsOf(r, regularSuccessorsOnly).filter { pc ⇒
                        !visitedSuccessors.contains(pc)
                    })
                }
        }
        false
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

    final def successorsOf(pc: PC, regularSuccessorOnly: Boolean): PCs =
        if (regularSuccessorOnly)
            regularSuccessorsOf(pc)
        else
            allSuccessorsOf(pc)

    final def foreachSuccessorOf(pc: PC)(f: PC ⇒ Unit): Unit = {
        regularSuccessorsOf(pc).foreach { f }
        exceptionHandlerSuccessorsOf(pc).foreach { f }
    }

    /**
     * Returns `true` if the instruction with the given pc has multiple
     *  predecessors (more than one).
     *
     * @note This function calculates the respective information on demand by traversing
     *      the successors.
     */
    def hasMultipleRegularPredecessors(pc: PC): Boolean = {
        var predecessors = 0
        var i = code.instructions.size - 1
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
     * Tests if the instruction with the given pc is a potential predecessor of the
     * given successor instruction.
     */
    def isRegularPredecessorOf(pc: PC, successorPC: PC): Boolean = {
        var visitedSuccessors = UShortSet(pc)
        var successorsToVisit = regularSuccessorsOf(pc)
        while (successorsToVisit.nonEmpty) {
            if (successorsToVisit.contains(successorPC))
                return true;

            visitedSuccessors = visitedSuccessors ++ successorsToVisit
            successorsToVisit =
                successorsToVisit.foldLeft(UShortSet.empty) { (l, r) ⇒
                    l ++ (regularSuccessorsOf(r).filter { pc ⇒ !visitedSuccessors.contains(pc) })
                }
        }
        false
    }

    /**
     * @inheritdoc
     *
     * @note This method is called by the abstract interpretation framework.
     */
    abstract override def flow(
        currentPC:                        PC,
        currentOperands:                  Operands,
        currentLocals:                    Locals,
        successorPC:                      PC,
        isSuccessorSchedulued:            Answer,
        isExceptionalControlFlow:         Boolean,
        abruptSubroutineTerminationCount: Int,
        wasJoinPerformed:                 Boolean,
        worklist:                         List[PC],
        operandsArray:                    OperandsArray,
        localsArray:                      LocalsArray,
        tracer:                           Option[AITracer]
    ): List[PC] = {

        val successors =
            if (isExceptionalControlFlow)
                domain.exceptionHandlerSuccessors
            else
                domain.regularSuccessors

        val successorsOfPC = successors(currentPC)
        if (successorsOfPC eq null)
            successors(currentPC) = UShortSet(successorPC)
        else {
            val newSuccessorsOfPC = successorPC +≈: successorsOfPC
            if (newSuccessorsOfPC ne successorsOfPC)
                successors(currentPC) = newSuccessorsOfPC
        }

        super.flow(
            currentPC, currentOperands, currentLocals,
            successorPC, isSuccessorSchedulued,
            isExceptionalControlFlow, abruptSubroutineTerminationCount,
            wasJoinPerformed,
            worklist,
            operandsArray, localsArray,
            tracer
        )
    }

    /**
     * Creates a graph representation of the CFG.
     */
    def cfgAsGraph(): DefaultMutableNode[List[PC]] = {
        val instructions = code.instructions
        val codeSize = instructions.size
        val nodes = new Array[DefaultMutableNode[List[PC]]](codeSize)
        val nodePredecessorsCount = new Array[Int](codeSize)
        // 1. create nodes
        for (pc ← code.programCounters) {
            nodes(pc) = {
                var visualProperties = Map("shape" → "box", "labelloc" → "l")

                if (instructions(pc).isInstanceOf[ReturnInstruction]) {
                    visualProperties += "fillcolor" → "green"
                    visualProperties += "style" → "filled"
                } else if (instructions(pc).isInstanceOf[ATHROW.type]) {
                    visualProperties += "fillcolor" → "yellow"
                    visualProperties += "style" → "filled"
                } else if (allSuccessorsOf(pc).isEmpty) {
                    visualProperties += "fillcolor" → "red"
                    visualProperties += "style" → "filled"
                    visualProperties += "shape" → "octagon"
                }

                if (code.exceptionHandlersFor(pc).nonEmpty) {
                    visualProperties += "color" → "orange"
                }

                if (code.exceptionHandlers.exists { eh ⇒ eh.handlerPC == pc }) {
                    visualProperties += "peripheries" → "2"
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
                    List.empty[DefaultMutableNode[List[PC]]]
                )
            }
        }
        // 2. create edges
        for (pc ← code.programCounters; succPC ← allSuccessorsOf(pc)) {
            nodes(pc).addChild(nodes(succPC))
            nodePredecessorsCount(succPC) += 1
        }

        // 3. fold nodes
        // Nodes that have only one successor and where the successor has only one
        // predecessor are merged into one node; basically, we recreate the
        // _effective_ basic blocks; an _effective_ basic block is a block where we do
        // _not observe_ any jumps in and out unless we are at the beginning or end of
        // the block
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

