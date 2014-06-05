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

import java.net.URL

import scala.Console.{ err, RED, RESET }

import org.opalj.util.PerformanceEvaluation.{ time, memory, asMB, ns2sec }

import br._
import br.analyses._

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

            val ComputedCallGraph(callGraph1, _, _) = time {
                CallGraphFactory.create(
                    project,
                    entryPoints,
                    new CHACallGraphAlgorithmConfiguration
//                    new VTACallGraphAlgorithmConfiguration {
//                        override def Domain(
//                            theProject: SomeProject,
//                            cache: Cache,
//                            classFile: ClassFile,
//                            method: Method): VTACallGraphDomain =
//                            new DefaultVTACallGraphDomain(theProject, cache, classFile, method, 0)
//                    }
                )
            } { t ⇒ println("Creating the first call graph took: "+ns2sec(t)) }

            val ComputedCallGraph(callGraph2, _, _) = time {
                CallGraphFactory.create(
                    project,
                    entryPoints,
                    new VTACallGraphAlgorithmConfiguration {
                        override def Domain(
                            theProject: SomeProject,
                            cache: Cache,
                            classFile: ClassFile,
                            method: Method): VTACallGraphDomain =
                            new DefaultVTACallGraphDomain(theProject, cache, classFile, method, 1)
                    })
            } { t ⇒ println("Creating the second call graph took: "+ns2sec(t)) }

            println("Methods that have different call graphs:")
            time {
                callGraph1.foreachCallingMethod { (method, callees1 /*: Map[PC, Iterable[Method]]*/ ) ⇒
                    val callees2 = callGraph2.calls(method)
                    val diff =
                        (callees1.par.collect {
                            case (pc, callees) if callees2.get(pc).map(_ != callees).getOrElse(true) ⇒
                                (
                                    method.body.get.lineNumber(pc),
                                    callees2.get(pc),
                                    callees2.get(pc).map(callees.toSet -- _).getOrElse(callees)
                                )
                        }).filter(_._3.nonEmpty)

                    diff.seq foreach { callSite ⇒

                        print(project.classFile(method).thisType.toJava+"{ "+method.toJava+"{")
                        print(Console.WHITE + Console.BLACK_B + callSite._1 + Console.RESET)
                        print(Console.BLUE + callSite._2.mkString(", ") + Console.RESET)
                        print(Console.RED + callSite._3.mkString(", ") + Console.RESET)
                        println("} }")
                    }
                }
            } { t ⇒ println("Calculting the differences took: "+ns2sec(t)) }

            BasicReport("Finished.")
        }
    }
}


   
                            
     
