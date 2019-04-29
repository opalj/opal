/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.net.URL

import org.opalj.log.OPALLogger.info
import org.opalj.value._
import org.opalj.br._
import org.opalj.br.cfg._
import org.opalj.br.analyses._

/**
 * Computes the available arithmetic binary expressions for a specified method.
 *
 * @author Michael Eichberg
 */
object AvailableExpressions extends DefaultOneStepAnalysis {

    override def description: String = "Computes the available arithmetic expressions."

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

        // Find the class that we want to analyze.
        // (Left as an exercise: error handling...)
        val className = params.find(_.startsWith("-class=")).get.substring(7).replace('.', '/')
        val methodSignature = params.find(_.startsWith("-method=")).get.substring(8)
        info("progress", s"trying to find: $className{ $methodSignature }")

        val cf = p.classFile(ObjectType(className)) match {
            case Some(cf) ⇒ cf
            case None     ⇒ return s"Class $className could not be found!";
        }
        val m = cf.methods.find(_.signatureToJava(false).contains(methodSignature)) match {
            case Some(m) ⇒ m
            case None    ⇒ return s"Method $methodSignature could not be found!";
        }
        info("progress", s"analyzing: ${m.toJava}")

        // Run analysis
        renderResult(analyzeMethod(p, m))
    }

    // ---------------------------------------------------------------------------------------------
    //
    // The implementation of the "Available Expressions Analysis"
    //
    // ---------------------------------------------------------------------------------------------

    final type TACAICode = TACode[TACMethodParameter, DUVar[ValueInformation]]
    final type Result = (TACAICode, Array[Facts], Facts, Facts)
    final type Fact = (BinaryArithmeticOperator, Expr[_], Expr[_])
    final type Facts = Set[Fact]

    def analyzeMethod(p: Project[URL], m: Method): Result = {

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

    def renderResult(r: Result): String = {
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
