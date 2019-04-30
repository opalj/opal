/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.net.URL

import org.opalj.value._
import org.opalj.br._
import org.opalj.br.cfg._
import org.opalj.br.analyses._

/**
 * Computes the very busy binary arithmetic expressions.
 *
 * @author Michael Eichberg
 */
object VeryBusyExpressions extends MethodAnalysisApplication {

    override def description: String = "Identifies the very busy arithmetic expressions."

    final type TACAICode = TACode[TACMethodParameter, DUVar[ValueInformation]]
    final type Result = (TACAICode, Array[Facts], Facts, Facts)
    final type Fact = (BinaryArithmeticOperator, Expr[_], Expr[_])
    final type Facts = Set[Fact]

    override def analyzeMethod(p: Project[URL], m: Method): Result = {
        // 2. Get the SSA-like three-address code.
        //    (Using the naive three-address code wouldn't be useful given that all
        //    operands based variables are always immediately redefined! )
        val tacaiKey = p.get(ComputeTACAIKey)
        val taCode = tacaiKey(m)
        val cfg = taCode.cfg

        // Recall that we work on an SSA like representation which typically does not have
        // simple alias creating statements of the form x_1 = x_0!

        val seed = Set.empty[Fact]

        def t(inFacts: Facts, stmt: Stmt[_], pc: PC, succId: CFG.SuccessorId) = {
            stmt match {
                case Assignment(_, _, e: BinaryExpr[_]) if succId >= 0 ⇒
                    inFacts + ((e.op, e.left, e.right))

                case _ ⇒ inFacts
            }
        }

        def join(oldFacts: Facts, newFacts: Facts) = {
            val availableFacts = oldFacts.intersect(newFacts)
            // The following test is required, because Set.intersect is not implemented
            // appropriately.
            if (availableFacts.size == oldFacts.size)
                oldFacts
            else
                availableFacts
        }

        val (stmtFacts, normalRetFacts, abnormalRetFacts) = {
            cfg.performForwardDataFlowAnalysis(seed, t, join)
        }

        (taCode, stmtFacts, normalRetFacts, abnormalRetFacts)
    }

    override def renderResult(r: Result): String = {
        val (taCode, stmtFacts, normalRetFacts, abnormalRetFacts) = r

        ToTxt(taCode).mkString("Code:\n", "\n", "\n") +
            stmtFacts
            .map(_.toString)
            .zipWithIndex
            .map(e ⇒ { val (f, index) = e; s"$index: $f" })
            .mkString("Available expressions:\n\t", "\n\t", "\n\n")+
            "\tNormal return(s): "+
            (if (normalRetFacts != null) normalRetFacts.toString else "N/A")+"\n"+
            "\tAbnormal return(s): "+
            (if (abnormalRetFacts != null) abnormalRetFacts.toString else "N/A")
    }
}
