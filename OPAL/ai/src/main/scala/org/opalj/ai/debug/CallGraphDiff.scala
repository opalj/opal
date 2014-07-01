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
package debug

import java.net.URL

import scala.Console.{ err, RED, RESET }

import org.opalj.util.PerformanceEvaluation.{ time, memory, asMB, ns2sec }

import org.opalj.ai.project._
import org.opalj.br._
import org.opalj.br.analyses._

/**
 *
 * @author Michael Eichberg
 */
object CallGraphDiff extends AnalysisExecutor {

    val analysis = new Analysis[URL, BasicReport] {

        override def title: String = "Identify differences between two call graphs."

        override def description: String = "Identifies methods that do not have the same call graph information."

        override def analyze(project: Project[URL], parameters: Seq[String]) = {
            import CallGraphFactory.defaultEntryPointsForLibraries
            val entryPoints = defaultEntryPointsForLibraries(project)

            val ComputedCallGraph(lessPreciseCG, _, _) = time {
                CallGraphFactory.create(
                    project,
                    entryPoints,
                    new CHACallGraphAlgorithmConfiguration
                //                                    new VTACallGraphAlgorithmConfiguration {
                //                                        override def Domain[Source](
                //                                            theProject: Project[Source],
                //                                            cache: Cache,
                //                                            classFile: ClassFile,
                //                                            method: Method): VTACallGraphDomain =
                //                                            new DefaultVTACallGraphDomain(
                //                                                theProject, cache, classFile, method, 1
                //                                            )
                //                                    }
                )
            } { t ⇒ println("Creating the first call graph took: "+ns2sec(t)) }

            val ComputedCallGraph(morePreciseCG, _, _) = time {
                CallGraphFactory.create(
                    project,
                    entryPoints,
                    new VTACallGraphAlgorithmConfiguration {
                        override def Domain[Source](
                            theProject: Project[Source],
                            cache: Cache,
                            classFile: ClassFile,
                            method: Method): VTACallGraphDomain =
                            new DefaultVTACallGraphDomain(
                                theProject, cache, classFile, method, 4
                            ) with domain.ConstantFieldValuesResolution[Source]
                    })
            } { t ⇒ println("Creating the second call graph took: "+ns2sec(t)) }

            println("Methods that have different call graphs:")
            time {
                lessPreciseCG.foreachCallingMethod { (method, allCalleesLPCG /*: Map[PC, Iterable[Method]]*/ ) ⇒
                    val allCalleesMPCG = morePreciseCG.calls(method)

                    var reports: List[CallGraphDifferenceReport] = Nil
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
                    val (unexpected, additional) = reports.partition(_.isInstanceOf[UnexpectedCallTargets])
                    if (unexpected.nonEmpty || additional.nonEmpty) {
                        println("Differences for "+project.classFile(method).thisType.toJava+" - "+method.descriptor.toJava(method.name))
                        if (additional.nonEmpty)
                            println(additional.mkString("\t", "\n\t", "\n"))
                        if (unexpected.nonEmpty)
                            println(unexpected.mkString("\t", "\n\t", "\n"))
                        println("\n")
                    }
                }
            } { t ⇒ println("Calculting the differences took: "+ns2sec(t)) }

            BasicReport("Finished.")
        }
    }
}

trait CallGraphDifferenceReport {
    val differenceClassifier: String
    val project: SomeProject
    val method: Method
    val pc: PC
    val callTargets: Iterable[Method]
    final override def toString: String = {
        import Console._
        differenceClassifier+
            "PC="+pc+"(Line="+method.body.get.lineNumber(pc).getOrElse("NotAvailable")+"): "+
            (
                callTargets map { method ⇒
                    project.classFile(method).thisType.toJava+"{ "+
                        CYAN + method.descriptor.toJava(method.name) + RESET+
                        " }"
                }
            ).mkString(BOLD+"; "+RESET)
    }
}

import scala.language.existentials

case class AdditionalCallTargets(
        project: SomeProject,
        method: Method,
        pc: PC,
        callTargets: Iterable[Method]) extends CallGraphDifferenceReport {
    import Console._
    val differenceClassifier = BLUE+"[Additional] "+RESET
}

case class UnexpectedCallTargets(
        project: SomeProject,
        method: Method,
        pc: PC,
        callTargets: Iterable[Method]) extends CallGraphDifferenceReport {
    import Console._
    val differenceClassifier = RED+"[Unexpected] "+RESET
}                        
     
