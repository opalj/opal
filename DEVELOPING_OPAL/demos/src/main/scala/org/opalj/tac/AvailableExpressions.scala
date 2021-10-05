/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.net.URL

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value._
import org.opalj.br._
import org.opalj.br.cfg._
import org.opalj.br.analyses._

/**
 * Computes the available arithmetic binary expressions.
 *
 * Potential extensions:
 *  - Basic:
 *      - support unary expressions
 *      - support commutative operators (e.g., +, *)
 *  - Advanced:
 *      - support "getField/getStatic" for (effectively final fields)
 *      - support invocations of pure methods
 *
 * @author Michael Eichberg
 */
object AvailableExpressions extends MethodAnalysisApplication {

    override def description: String = "Identifies the available arithmetic expressions."

    final type TACAICode = TACode[TACMethodParameter, DUVar[ValueInformation]]
    final type Result = (TACAICode, Array[Facts], Facts, Facts)
    final type Fact = (BinaryArithmeticOperator, IntTrieSet, IntTrieSet)
    final type Facts = Set[Fact]

    override def analyzeMethod(p: Project[URL], m: Method): Result = {
        // Get the SSA-like three-address code.
        // Please note that using the naive three-address code wouldn't be useful given that all
        // operands based variables are always immediately redefined!
        val tacaiKey = p.get(ComputeTACAIKey)
        val taCode = tacaiKey(m)
        val cfg = taCode.cfg

        val seed = Set.empty[Fact]

        def transfer(inFacts: Facts, stmt: Stmt[_], index: PC, succId: CFG.SuccessorId): Facts = {
            // Recall that we work on an SSA like representation which typically does not have
            // simple alias creating statements of the form x_1 = x_0!
            stmt match {
                case Assignment(_, _, BinaryExpr(_, _, op, UVar(_, l), UVar(_, r))) if succId >= 0 =>
                    inFacts + ((op, l, r))

                case _ => inFacts
            }
        }

        def join(oldFacts: Facts, newFacts: Facts) = {
            val availableFacts = oldFacts.intersect(newFacts)
            // The following test is required, because Set.intersect will not return the "left"
            // set if it actually represents the result of the intersection.
            // (see `CFG.performForwardDataFlowAnalysis` for further information.)
            if (availableFacts.size == oldFacts.size)
                oldFacts
            else
                availableFacts
        }

        val (stmtFacts, normalRetFacts, abnormalRetFacts) = {
            cfg.performForwardDataFlowAnalysis(seed, transfer, join)
        }

        (taCode, stmtFacts, normalRetFacts, abnormalRetFacts)
    }

    override def renderResult(r: Result): String = {
        val (taCode, stmtFacts, normalRetFacts, abnormalRetFacts) = r

        def factsToString(f: Facts): Iterable[String] = {
            f.map { e =>
                val (op, l, r) = e
                val lUVar = DefSites.toString(l).mkString("{", ",", "}")
                val rUVar = DefSites.toString(r).mkString("{", ",", "}")
                s"($lUVar $op $rUVar)"
            }
        }

        ToTxt(taCode).mkString("Code:\n", "\n", "\n") +
            stmtFacts
            .map(factsToString)
            .zipWithIndex
            .map(e => { val (f, index) = e; s"$index: $f" })
            .mkString("Available expressions:\n\t", "\n\t", "\n\n")+
            "\tNormal return(s): "+
            (if (normalRetFacts != null) factsToString(normalRetFacts) else "N/A")+"\n"+
            "\tAbnormal return(s): "+
            (if (abnormalRetFacts != null) factsToString(abnormalRetFacts) else "N/A")
    }
}
