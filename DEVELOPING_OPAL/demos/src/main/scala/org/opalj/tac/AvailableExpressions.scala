/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.net.URL

import org.opalj.log.OPALLogger.info
import org.opalj.br._
import org.opalj.br.analyses._

/**
 * Computes the available expressions.
 *
 * @author Michael Eichberg
 */
object AvailableExpressions extends DefaultOneStepAnalysis {

    override def description: String = "Computes Available Expressions per Statement"

    override def analysisSpecificParametersDescription: String = {
        "-class=<fully qualified name of the class>\n"+
            "-method=<name and/or parts of the signature>"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        if (parameters.size != 2 || parameters(0).substring(0, 7) == parameters(1).substring(0, 7))
            return List("missing parameters");

        parameters.foldLeft(List.empty[String]) { (notUnderstood, p) ⇒
            if (!p.startsWith("-class=") && !p.startsWith("-method="))
                p :: notUnderstood
            else
                notUnderstood
        }
    }

    override def doAnalyze(
        p:             Project[URL],
        params:        Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        implicit val logContext = p.logContext
        val className = params.find(param ⇒ param.startsWith("-class=")).get.substring(7).replace('.', '/')
        val methodSignature = params.find(param ⇒ param.startsWith("-method=")).get.substring(8)
        info("progress", s"trying to find: $className{ $methodSignature }")

        val cf = p.classFile(ObjectType(className)).get
        val m = cf.methods.find(m ⇒ m.signatureToJava(false).contains(methodSignature)).get
        info("progress", s"analyzing: ${m.toJava}")
        val tacaiKey = p.get(ComputeTACAIKey)
        val taCode = tacaiKey(m)
        val cfg = taCode.cfg

        // Recall that we work on an SSA like representation which additionally does not have
        // simple alias creating statements of the form x_1 = x_0!
        type F = (BinaryArithmeticOperator, Expr[_], Expr[_])
        type Fs = Set[F]
        val seed = Set.empty[F]
        def kill(inFacts: Fs, stmt: Stmt[_]) = {
            // we are SSA like... there is nothing to kill
            inFacts
        }
        def gen(newFacts: Fs, stmt: Stmt[_], completesNormally: Boolean) = {
            stmt match {
                case Assignment(_, _, binExp: BinaryExpr[_]) if completesNormally ⇒
                    newFacts + ((binExp.op, binExp.left, binExp.right))
                case _ ⇒ newFacts
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
            cfg.mfp[Fs](seed, kill, gen, cfJoin)

        "Result: \n"+
            taCode.toString+"\n"+
            stmtFacts
            .map(_.toString)
            .zipWithIndex
            .map(e ⇒ { val (f, index) = e; s"$index: $f" })
            .mkString("", "\n", "\n")+
            "Normal return(s): "+
            (if (normalReturnFacts != null) normalReturnFacts.toString else "N/A")+"\n"+
            "Abnormal return(s): "+
            (if (abnormalReturnFacts != null) abnormalReturnFacts.toString else "N/A")
    }
}
