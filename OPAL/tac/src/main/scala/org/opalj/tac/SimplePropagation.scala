/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

/**
 * A very simple peephole optimizer which performs intra-basic block constant and copy propagation
 * for the naive representation (in case of the ai-based representation these steps are already
 * done at abstract-interpretation time).
 *
 * @author Michael Eichberg
 */
object SimplePropagation extends TACOptimization[Param, IdBasedVar, NaiveTACode[Param]] {

    def apply(
        tac: TACOptimizationResult[Param, IdBasedVar, NaiveTACode[Param]]
    ): TACOptimizationResult[Param, IdBasedVar, NaiveTACode[Param]] = {
        val cfg = tac.code.cfg
        val bbs = cfg.allBBs
        val code = tac.code.stmts
        var wasTransformed = false
        bbs.withFilter(bb => bb.startPC < bb.endPC).foreach { bb =>
            var index = bb.startPC
            val max = bb.endPC
            while (index < max) {

                code(index) match {

                    case Assignment(pc, trgtVar, c @ (_: SimpleValueConst | _: IdBasedVar | _: Param)) =>

                        code(index + 1) match {
                            case Throw(nextPC, `trgtVar`) =>
                                code(index + 1) = Throw(nextPC, c)

                            case Assignment(nextPC, nextTrgtVar, `trgtVar`) =>
                                wasTransformed = true
                                if (nextTrgtVar == trgtVar /*immediate kill*/ ) code(index) = Nop(pc)
                                code(index + 1) = Assignment(nextPC, nextTrgtVar, c)

                            case Assignment(
                                nextPC,
                                nextTrgtVar: IdBasedVar,
                                PrimitiveTypecastExpr(exprPC, targetTpe, `trgtVar`)
                                ) =>
                                wasTransformed = true
                                if (nextTrgtVar.hasSameLocation(trgtVar) /*immediate kill*/ )
                                    code(index) = Nop(pc)
                                val newCastExpr = PrimitiveTypecastExpr(exprPC, targetTpe, c)
                                code(index + 1) = Assignment(nextPC, nextTrgtVar, newCastExpr)

                            case Assignment(
                                nextPC,
                                nextTrgtVar,
                                GetField(exprPC, declaringClass, name, declaredFieldType, `trgtVar`)
                                ) =>
                                wasTransformed = true
                                if (nextTrgtVar == trgtVar /*immediate kill*/ ) code(index) = Nop(pc)
                                val newGetfieldExpr = GetField(
                                    exprPC, declaringClass, name, declaredFieldType, c
                                )
                                code(index + 1) = Assignment(nextPC, nextTrgtVar, newGetfieldExpr)

                            case Assignment(
                                nextPC,
                                nextTrgtVar,
                                BinaryExpr(exprPC, cTpe, op, `trgtVar`, right)
                                ) =>
                                wasTransformed = true
                                if (nextTrgtVar == trgtVar /*immediate kill*/ ) code(index) = Nop(pc)
                                val newBinaryExpr = BinaryExpr(exprPC, cTpe, op, c, right)
                                code(index + 1) = Assignment(nextPC, nextTrgtVar, newBinaryExpr)

                            case Assignment(
                                nextPC,
                                nextTrgtVar,
                                BinaryExpr(exprPC, cTpe, op, left, `trgtVar`)
                                ) =>
                                wasTransformed = true
                                if (nextTrgtVar == trgtVar /*immediate kill*/ ) code(index) = Nop(pc)
                                val newBinaryExpr = BinaryExpr(exprPC, cTpe, op, left, c)
                                code(index + 1) = Assignment(nextPC, nextTrgtVar, newBinaryExpr)

                            case If(nextPC, `trgtVar`, condition, rightVar, target) =>
                                wasTransformed = true
                                code(index + 1) = If(nextPC, c, condition, rightVar, target)
                            case If(nextPC, leftVar, condition, `trgtVar`, target) =>
                                wasTransformed = true
                                code(index + 1) = If(nextPC, leftVar, condition, c, target)

                            case ReturnValue(nextPC, `trgtVar`) =>
                                wasTransformed = true
                                code(index) = Nop(pc) // it is impossible that we have another use..
                                code(index + 1) = ReturnValue(nextPC, c)

                            case _ => // nothing to do
                        }

                    case _ => // nothing to do
                }
                index += 1
            }

        }
        val taCode = tac.code
        val newTACode = new NaiveTACode(
            taCode.params,
            code, // <= mutated
            taCode.pcToIndex, // <= we only introduce nops => no need to update the mapping
            cfg,
            taCode.exceptionHandlers
        )
        new TACOptimizationResult(newTACode, wasTransformed)
    }
}
