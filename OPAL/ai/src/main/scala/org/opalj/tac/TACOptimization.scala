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
package tac

import org.opalj.br.cfg.CFG
import org.opalj.tac.TACNaive.IdBasedVar

/**
 * Common interface of all code optimizers that operate on the three-address code
 * representation.
 *
 * @author Michael Eichberg
 */
trait TACOptimization {

    /**
     * Transforms the given code to the target code.
     */
    def apply(tac: TACOptimizationResult): TACOptimizationResult
}

/**
 * Encapsulates the result of an optimization/transformation of some three-address code.
 *
 * @author Michael Eichberg
 */
case class TACOptimizationResult(
    code:           Array[Stmt],
    cfg:            CFG,
    wasTransformed: Boolean
)

/**
 * A very simple peephole optimizer which performs intra-basic block constant and copy propagation.
 *
 * @author Michael Eichberg
 */
object SimplePropagation extends TACOptimization {

    def apply(tac: TACOptimizationResult): TACOptimizationResult = {

        val bbs = tac.cfg.allBBs
        val code = tac.code
        var wasTransformed = false
        bbs.withFilter(bb ⇒ bb.startPC < bb.endPC).foreach { bb ⇒
            var index = bb.startPC
            val max = bb.endPC
            while (index < max) {

                code(index) match {

                    case Assignment(pc, trgtVar, c @ (_: SimpleValueConst | _: Var | _: Param)) ⇒

                        code(index + 1) match {
                            case Throw(nextPC, `trgtVar`) ⇒
                                code(index + 1) = Throw(nextPC, c)

                            case Assignment(nextPC, nextTrgtVar, `trgtVar`) ⇒
                                wasTransformed = true
                                if (nextTrgtVar == trgtVar /*immediate kill*/ ) code(index) = Nop(pc)
                                code(index + 1) = Assignment(nextPC, nextTrgtVar, c)

                            case Assignment(
                                nextPC,
                                nextTrgtVar: IdBasedVar,
                                PrimitiveTypecastExpr(exprPC, targetTpe, `trgtVar`)
                                ) ⇒
                                wasTransformed = true
                                if (nextTrgtVar.hasSameLocation(trgtVar) /*immediate kill*/ )
                                    code(index) = Nop(pc)
                                val newCastExpr = PrimitiveTypecastExpr(exprPC, targetTpe, c)
                                code(index + 1) = Assignment(nextPC, nextTrgtVar, newCastExpr)

                            case Assignment(
                                nextPC,
                                nextTrgtVar,
                                GetField(exprPC, declaringClass, name, `trgtVar`)
                                ) ⇒
                                wasTransformed = true
                                if (nextTrgtVar == trgtVar /*immediate kill*/ ) code(index) = Nop(pc)
                                val newGetfieldExpr = GetField(exprPC, declaringClass, name, c)
                                code(index + 1) = Assignment(nextPC, nextTrgtVar, newGetfieldExpr)

                            case Assignment(
                                nextPC,
                                nextTrgtVar,
                                BinaryExpr(exprPC, cTpe, op, `trgtVar`, right)
                                ) ⇒
                                wasTransformed = true
                                if (nextTrgtVar == trgtVar /*immediate kill*/ ) code(index) = Nop(pc)
                                val newBinaryExpr = BinaryExpr(exprPC, cTpe, op, c, right)
                                code(index + 1) = Assignment(nextPC, nextTrgtVar, newBinaryExpr)

                            case Assignment(
                                nextPC,
                                nextTrgtVar,
                                BinaryExpr(exprPC, cTpe, op, left, `trgtVar`)
                                ) ⇒
                                wasTransformed = true
                                if (nextTrgtVar == trgtVar /*immediate kill*/ ) code(index) = Nop(pc)
                                val newBinaryExpr = BinaryExpr(exprPC, cTpe, op, left, c)
                                code(index + 1) = Assignment(nextPC, nextTrgtVar, newBinaryExpr)

                            case If(nextPC, `trgtVar`, condition, rightVar, target) ⇒
                                wasTransformed = true
                                code(index + 1) = If(nextPC, c, condition, rightVar, target)
                            case If(nextPC, leftVar, condition, `trgtVar`, target) ⇒
                                wasTransformed = true
                                code(index + 1) = If(nextPC, leftVar, condition, c, target)

                            case ReturnValue(nextPC, `trgtVar`) ⇒
                                wasTransformed = true
                                code(index) = Nop(pc) // it is impossible that we have another use..
                                code(index + 1) = ReturnValue(nextPC, c)

                            case _ ⇒ // nothing to do
                        }

                    case _ ⇒ // nothing to do
                }
                index += 1
            }

        }

        new TACOptimizationResult(code, tac.cfg, wasTransformed)
    }
}
