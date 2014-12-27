/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package ai
package project

import scala.language.existentials

import java.net.URL
import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.CYAN
import scala.Console.RED
import scala.Console.RESET
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.domain
import org.opalj.ai.project.CallGraphFactory.defaultEntryPointsForLibraries
import org.opalj.br.analyses.SomeProject

/**
 * Helper functionality to compare two call graphs.
 *
 * @author Michael Eichberg
 */
object CallGraphComparison {

    def apply(
        project: SomeProject,
        lessPreciseCG: CallGraph,
        morePreciseCG: CallGraph): (List[CallGraphDifferenceReport], List[CallGraphDifferenceReport]) = {
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

        val thisClassType = project.classFile(method).thisType
        differenceClassifier+" "+
            project.source(thisClassType).getOrElse("<Source File Not Available>")+": "+
            thisClassType.toJava+"{ "+method.toJava+"{ "+
            "pc="+pc+"(line="+method.body.get.lineNumber(pc).getOrElse("NotAvailable")+"): "+
            (
                callTargets map { method ⇒
                    BLUE + project.classFile(method).thisType.toJava+"{ "+
                        CYAN + method.descriptor.toJava(method.name) + RESET+
                        " }"
                }
            ).mkString(BOLD+"; "+RESET)+" } }"
    }
}

case class AdditionalCallTargets(
    project: SomeProject,
    method: Method,
    pc: PC,
    callTargets: Iterable[Method])
        extends CallGraphDifferenceReport {

    final val differenceClassifier = BLUE+"[Additional]"+RESET
}

/**
 *
 */
case class UnexpectedCallTargets(
    project: SomeProject,
    method: Method,
    pc: PC,
    callTargets: Iterable[Method])
        extends CallGraphDifferenceReport {

    final val differenceClassifier = RED+"[Unexpected]"+RESET
}