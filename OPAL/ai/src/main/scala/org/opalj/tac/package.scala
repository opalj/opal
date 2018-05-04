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
import org.opalj.br.ExceptionHandler
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.graphs.Node

/**
 * Common definitions related to the definition and processing of three address code.
 *
 * @author Michael Eichberg
 */
package object tac {

    /**
     * Identifies the implicit `this` reference in the 3-address code representation.
     * -1 always identifies the origin of the self reference(`this`) if the the method is
     * an instance method; if the method is not an instance method the origin -1 is not used.
     */
    final val OriginOfThis /*: ValueOrigin*/ = -1

    final val SelfReferenceParameter = IntTrieSet(OriginOfThis)

    final val AllTACNaiveOptimizations: List[TACOptimization[Param, IdBasedVar]] = {
        List(SimplePropagation)
    }

    def tacToGraph[V <: Var[V]](
        stmts: Array[Stmt[V]],
        cfg:   CFG[Stmt[V], TACStmts[V]]
    ): Iterable[Node] = {
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

    def tacToDot[V <: Var[V]](
        stmts: Array[Stmt[V]],
        cfg:   CFG[Stmt[V], TACStmts[V]]
    ): String = {
        org.opalj.graphs.toDot(
            tacToGraph(stmts, cfg),
            ranksep = "0.4"
        )
    }

    @inline private[tac] def getStartAndEndIndex(
        oldEH:      ExceptionHandler,
        newIndexes: Array[Int]
    )(
        implicit
        aiResult: AIResult { val domain: Domain with RecordDefUse }
    ): (Int, Int) = {
        val oldStartPC = oldEH.startPC
        var newStartIndex = newIndexes(oldStartPC)
        var newEndIndex = newIndexes(oldEH.endPC)
        if (newEndIndex <= 0) {
            // The end of the try-block is dead and therefore the end instruction maps to "0".
            // E.g.,
            // try - start
            //      invoke => ALWAYS THROWS AS IDENTIFIED BY THE AI
            //      if... // DEAD => no mapping for endPC
            // try - end
            /*
               37    aload_3    // <= determined to be NULL (!!!)
               38    invokeinterface (nargs=1) java.sql.ResultSet { void close () }
               43    goto 48
               46    astore 4
               48    return
               try [37-43) catch 46 java.lang.Exception

               Mapping
               37 =>   N/A
               38 =>   call => ALWAYS THROWS EXCEPTION
               43 =>   // DEAD (38 always throws an exception)
               46 =>   N/A
               48 =>   return
            */

            var lastPC = oldEH.endPC
            do {
                newEndIndex = newIndexes(lastPC)
                // it may be the case that an exception handler - which covers the start
                // of a class file collapses; in this case, we have to make sure that
                // lastPC is not negative when whe ask for the new index..., hence,
                // 1) get new end index
                // 2) decrement lastPC
                lastPC -= 1

            } while (newEndIndex <= 0 && lastPC >= oldStartPC)

            if (lastPC < oldStartPC) {
                // the EH is totally dead... i.e., all code in the try block is dead
                assert(
                    (oldEH.startPC until oldEH.endPC) forall { tryPC ⇒
                        aiResult.domain.exceptionHandlerSuccessorsOf(tryPC).isEmpty
                    },
                    s"exception handler collapsed: $oldEH ⇒ $newStartIndex"
                )
                newStartIndex = -1
                newEndIndex = -1
            } else if (newStartIndex == newEndIndex && aiResult.domain.throwsException(lastPC)) {
                newEndIndex += 1
            }
            // else ...
            // the (remaining) eh only encompasses instructions which don't throw exceptions

        }

        assert(
            newEndIndex >= newStartIndex, // both equal => EH is dead!
            s"the end of the try block $newEndIndex is before the start $newStartIndex"
        )

        (newStartIndex, newEndIndex)
    }

    /**
     * Updates the exception handlers by adjusting the start, end and handler index (pc).
     *
     * @note   This method can only be used in cases where the order of instructions remains
     *         the same and/or instructions are deleted. If instructions are reordered this method
     *         cannot be used!
     *
     * @param newIndexes A map that contains for each previous index the new index
     *                   that should be used.
     * @param aiResult The result of the abstract interpretation of the method.
     * @return The new exception handlers.
     */
    def updateExceptionHandlers(
        newIndexes: Array[Int]
    )(
        implicit
        aiResult: AIResult { val domain: Domain with RecordDefUse }
    ): ExceptionHandlers = {
        val code = aiResult.code
        val exceptionHandlers = code.exceptionHandlers

        exceptionHandlers map { oldEH ⇒
            // Recall, that the endPC is not inclusive and - therefore - if the last instruction is
            // included in the handler block, the endPC is equal to `(pc of last instruction) +
            // instruction.size`; however, this is already handled by the caller!
            val (newStartIndex, newEndIndex) = getStartAndEndIndex(oldEH, newIndexes)
            val newEH = oldEH.copy(
                startPC = newStartIndex,
                endPC = newEndIndex,
                handlerPC = newIndexes(oldEH.handlerPC)
            )
            newEH
        } filter { eh ⇒
            // filter dead exception handlers...
            eh.endPC > eh.startPC
        }
    }

}
