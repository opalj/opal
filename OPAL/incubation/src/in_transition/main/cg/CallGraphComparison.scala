/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import Console._

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

/**
 * Helper functionality to compare two call graphs.
 *
 * @author Michael Eichberg
 */
object CallGraphComparison {

    def apply(
        project:       SomeProject,
        lessPreciseCG: CallGraph,
        morePreciseCG: CallGraph
    ): (List[CallGraphDifferenceReport], List[CallGraphDifferenceReport]) = {
        var reports: List[CallGraphDifferenceReport] = Nil
        lessPreciseCG.foreachCallingMethod { (method, allCalleesLPCG /*: Map[PC, Iterable[Method]]*/ ) ⇒
            val allCalleesMPCG = morePreciseCG.calls(method)

            allCalleesLPCG foreach { callSiteLPCG ⇒
                val (pc, calleesLPCG) = callSiteLPCG
                val callSiteMPCGOption = allCalleesMPCG.get(pc)
                if (callSiteMPCGOption.isDefined) {
                    val calleesMPCG = scala.collection.mutable.HashSet() ++ (callSiteMPCGOption.get)
                    val additionalCallTargetsInLPCG = for {
                        calleeLPCG ← calleesLPCG
                        if !calleesMPCG.remove(calleeLPCG)
                    } yield {
                        calleeLPCG
                    }
                    if (additionalCallTargetsInLPCG.nonEmpty)
                        reports = AdditionalCallTargets(project, method, pc, additionalCallTargetsInLPCG) :: reports

                    if (calleesMPCG.nonEmpty)
                        reports = UnexpectedCallTargets(project, method, pc, calleesMPCG) :: reports

                } else {
                    reports = AdditionalCallTargets(project, method, pc, calleesLPCG) :: reports
                }
            }
        }
        reports.partition(_.isInstanceOf[UnexpectedCallTargets])
    }
}

sealed trait CallGraphDifferenceReport {
    val differenceClassifier: String
    val project: SomeProject
    val method: Method
    val pc: PC
    val callTargets: Iterable[Method]

    final override def toString: String = {
        val thisClassType = method.classFile.thisType
        differenceClassifier+" "+
            project.source(thisClassType).getOrElse("<Source File Not Available>")+": "+
            method.toJava+
            "pc="+pc+"(line="+method.body.get.lineNumber(pc).getOrElse("NotAvailable")+"): "+
            callTargets.map(method ⇒ BLUE + method.toJava + RESET).mkString(BOLD+"; "+RESET)
    }
}

case class AdditionalCallTargets(
        project:     SomeProject,
        method:      Method,
        pc:          PC,
        callTargets: Iterable[Method]
) extends CallGraphDifferenceReport {

    final val differenceClassifier = BLUE+"[Additional]"+RESET
}

case class UnexpectedCallTargets(
        project:     SomeProject,
        method:      Method,
        pc:          PC,
        callTargets: Iterable[Method]
) extends CallGraphDifferenceReport {

    final val differenceClassifier = RED+"[Unexpected]"+RESET
}
