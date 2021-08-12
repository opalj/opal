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
 * Computes the very busy binary arithmetic expressions.
 *
 * This analysis can, e.g., be run against the demo code using sbt. After starting sbt type:
 *  `; project Demos ; console`
 * then start the analysis:
 * `run -cp=target/scala-2.12/classes/org/opalj/tac -class=org.opalj.tac.VeryBusyExpressionsDemo -method=n_n_h`
 *
 * @author Michael Eichberg
 */
object VeryBusyExpressions extends MethodAnalysisApplication {

    override def description: String = "Identifies very busy binary arithmetic expressions."

    final type TACAICode = TACode[TACMethodParameter, DUVar[ValueInformation]]
    final type Result = (TACAICode, Array[Facts], Facts)

    // We use as a replacement for variable names the IntTrieSets which identify the def-sites.
    final type Fact = (BinaryArithmeticOperator, IntTrieSet /*Def-Sites*/ , IntTrieSet /*Def-Sites*/ )
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
            // Recall that we work on a flat SSA like representation.
            stmt match {
                case Assignment(_, _, expr) =>
                    // Kill... those (binary) expressions where one of the variables is defined
                    //         by this assignment. For that, we simply check the def-sites.
                    val outFacts = inFacts.filter { f =>
                        val (_, leftFacts, rightFacts) = f
                        !leftFacts.contains(index) && !rightFacts.contains(index)
                    }
                    // Gen...
                    expr match {
                        case BinaryExpr(_, _, op, UVar(_, l), UVar(_, r)) => outFacts + ((op, l, r))
                        case _                                            => outFacts
                    }
                case _ => inFacts
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

        val (stmtFacts, initFacts) = cfg.performBackwardDataFlowAnalysis(seed, transfer, join)
        (taCode, stmtFacts, initFacts)
    }

    override def renderResult(r: Result): String = {
        val (taCode, stmtFacts, initFacts) = r

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
            .map({ e => val (f, index) = e; s"$index: $f" })
            .mkString("Very busy expressions (on exit):\n\t", "\n\t", "\n\n")+
            "\tInit: "+factsToString(initFacts)
    }
}
