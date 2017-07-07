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

import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ExceptionHandlers
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.graphs.Node

/**
 * Common definitions related to the definition and processing of three address code.
 *
 * @author Michael Eichberg
 */
package object tac {

    final val AllTACNaiveOptimizations: List[TACOptimization[Param, IdBasedVar]] = {
        List(SimplePropagation)
    }

    def tacToGraph[V <: Var[V]](stmts: Array[Stmt[V]], cfg: CFG): Iterable[Node] = {
        cfg.toDot { bb: BasicBlock ⇒
            val pcRange = bb.startPC to bb.endPC
            val bbStmts = stmts.slice(bb.startPC, bb.endPC + 1).zip(pcRange)
            val txtStmts = bbStmts.map { stmtPC ⇒
                val (stmt, pc) = stmtPC
                pc+": "+ToTxt.toTxtStmt[V](stmt, false)
            }
            txtStmts.mkString("", "\\l\\l", "\\l")
        }
    }

    def tacToDot[V <: Var[V]](stmts: Array[Stmt[V]], cfg: CFG): String = {
        org.opalj.graphs.toDot(
            tacToGraph(stmts, cfg),
            ranksep = "0.4"
        )
    }

    /**
     * Updates the exception handlers by adjusting the start, end and handler index (pc).
     *
     * @note   This method can only be used in cases where the order of instructions remains
     *         the same and/or instructions are deleted. If instructions are reordered this method
     *         cannot be used!
     *
     * @param aiResult The result of the abstract interpretation of the method. (We use the aiResult
     *                 for verification purposes only (`assert`s)).
     * @param newIndexes A map that contains for each previous index the new index
     *                   that should be used.
     * @return The new exception handlers.
     */
    def updateExceptionHandlers(
        aiResult:   AIResult { val domain: Domain with RecordDefUse },
        newIndexes: Array[Int]
    ): ExceptionHandlers = {
        val code = aiResult.code
        val exceptionHandlers = code.exceptionHandlers

        exceptionHandlers map { oldEH ⇒
            // Recall, that the endPC is not inclusive and - therefore - if the last instruction is
            // included in the handler block, the endPC is equal to `(pc of last instruction) +
            // instruction.size`; however, this is already handled by the caller!
            val newStartPC = newIndexes(oldEH.startPC)
            var newEndPC = newIndexes(oldEH.endPC)
            if (newEndPC == 0) {
                // The end of the try-block is dead and therefore the end instruction maps to "0"
                // E.g.,
                // try - start
                //      invoke => ALWAYS THROWS AS IDENTIFIED BY THE AI
                //      if... // DEAD => no mapping for endPC
                // try - end
                var lastPC = oldEH.endPC - 1
                while (lastPC >= newStartPC && newEndPC == 0) {
                    newEndPC = newIndexes(lastPC)
                    lastPC -= 1
                }
            }
            val newEH = oldEH.copy(
                startPC = newStartPC,
                endPC = newEndPC,
                handlerPC = newIndexes(oldEH.handlerPC)
            )
            assert(
                newEH.endPC >= newEH.startPC,
                s"the end of the try block ${newEH.endPC} is before the start ${newEH.startPC}"
            )
            assert(
                {
                    newEH.endPC > newEH.startPC || {
                        (oldEH.startPC until oldEH.endPC) forall { tryPC ⇒
                            aiResult.domain.exceptionHandlerSuccessorsOf(tryPC).isEmpty
                        }
                    }
                },
                s"exception handler collapsed: $oldEH ⇒ $newEH"
            )
            newEH
        } filter { eh ⇒
            eh.endPC > eh.startPC
        }
    }

    /**
     * Updates the exception handlers by adjusting the start, end and handler index (pc).
     *
     * This method can only be used in simple cases where the order of instructions remains
     * the same and the start and end still map to valid exception handlers -
     * deleting/adding instructions is supported.
     *
     * @note You should use `updateExceptionHandlers(AIResult,Array[Int])` whenever possible
     *       since that method performs additional checks!
     *
     * @param exceptionHandlers The code's exception handlers.
     * @param newIndexes A map that contains for each previous index the new index
     *                   that should be used.
     * @return The new exception handler.
     */
    def updateExceptionHandlers(
        exceptionHandlers: ExceptionHandlers,
        newIndexes:        Array[Int]
    ): ExceptionHandlers = {
        exceptionHandlers map { old ⇒
            // Recall, that the endPC is not inclusive and - therefore - if the last instruction is
            // included in the handler block, the endPC is equal to `(pc of last instruction) +
            // instruction.size`; however, this is already handled by the caller!

            val newEH = old.copy(
                startPC = newIndexes(old.startPC),
                endPC = newIndexes(old.endPC),
                handlerPC = newIndexes(old.handlerPC)
            )
            assert(newEH.startPC <= newEH.endPC)
            newEH
        }
    }
}
