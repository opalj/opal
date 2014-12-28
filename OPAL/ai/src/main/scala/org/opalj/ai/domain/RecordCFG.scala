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
     * @inheritdoc
     *
     * @note This method is called by the abstract interpretation framework.
     */
    abstract override def flow(
        currentPC: PC,
        successorPC: PC,
        isExceptionalControlFlow: Boolean,
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
            isExceptionalControlFlow, wasJoinPerformed,
            worklist,
            operandsArray, localsArray,
            tracer)
    }
}

