/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.net.URL

import scala.jdk.CollectionConverters._

// import org.opalj.log.OPALLogger.info
//import org.opalj.br._
//import org.opalj.br.cfg._
import org.opalj.br.analyses._

/**
 * Checks if a resource (an instance of an AutoCloseable) is closed.
 *
 * @author Michael Eichberg
 */
object CloseResources extends ProjectAnalysisApplication {

    override def doAnalyze(
        p:             Project[URL],
        params:        Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        // implicit val logContext = p.logContext
        // val tacaiKey = p.get(ComputeTACAIKey)

        val issues = new java.util.concurrent.ConcurrentLinkedQueue[String]()

        /* p.parForeachMethodWithBody(isInterrupted) { m =>
            val taCode = tacaiKey(m)
            val cfg = taCode.cfg

            // Recall that we work on an SSA like representation which additionally does not have
            // simple alias creating statements of the form x_1 = x_0!
            type F = (BinaryArithmeticOperator, Expr[_], Expr[_])
            type Fs = Set[F]
            val seed = Set.empty[F]
            def t(newFacts: Fs, stmt: Stmt[_], pc: PC, succId: CFG.SuccessorId) = {
                stmt match {
                    case Assignment(_, _, e: BinaryExpr[_]) if succId >= 0 /*completes normally?*/ =>
                        newFacts + ((e.op, e.left, e.right))
                    case _ => newFacts
                }
            }
            def cfJoin(oldFacts: Fs, newFacts: Fs) = {
                val availableFacts = oldFacts.intersect(newFacts)
                if (availableFacts.size == oldFacts.size)
                    oldFacts
                else
                    availableFacts
            }
            val (stmtFacts, normalReturnFacts, abnormalReturnFacts) =
                cfg.computeMOPSolution[Fs](seed, t, cfJoin)
        }
        */

        issues.asScala.mkString("\n")
    }
}
