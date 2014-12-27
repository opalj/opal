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
package domain
package l1

import java.io.File
import java.net.URL

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.util.control.ControlThrowable
import org.opalj.io.writeAndOpen
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.MethodWithBody
import org.opalj.ai.util.XHTML

/**
 * This system test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods. It basically tests if we can load and
 * process a large number of different classes without exceptions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultDomainTest extends FlatSpec with Matchers {

    behavior of "the l1.DefaultDomain"

    it should ("be able to perform an abstract interpretation of the JRE's classes") in {
        val project = org.opalj.br.TestSupport.createJREProject
        val maxEvaluationFactor: Double = 3.5d

        import org.opalj.util.PerformanceEvaluation.ns2sec
        val performanceEvaluationContext = new org.opalj.util.PerformanceEvaluation
        import performanceEvaluationContext.{ time, getTime }
        val methodsCount = new java.util.concurrent.atomic.AtomicInteger(0)

        def analyzeClassFile(
            source: String,
            classFile: ClassFile): Seq[(String, ClassFile, Method, Throwable)] = {

            val collectedExceptions =
                for (method @ MethodWithBody(body) ← classFile.methods) yield {
                    try {
                        time('AI) {
                            val ai = new InstructionCountBoundedAI[l1.DefaultDomain[URL]](body, maxEvaluationFactor)
                            val result =
                                ai.apply(
                                    classFile,
                                    method,
                                    new l1.DefaultDomain(project, classFile, method)
                                )
                            if (result.wasAborted) {
                                throw new InterruptedException(
                                    "evaluation bound (max="+ai.maxEvaluationCount+
                                        ") exceeded")
                            }
                        }
                        methodsCount.incrementAndGet()
                        None
                    } catch {
                        case ct: ControlThrowable ⇒ throw ct
                        case t: Throwable ⇒
                            // basically, we want to catch everything!
                            val source = project.source(classFile.thisType).get.toString
                            Some((source, classFile, method, t))
                    }
                }

            collectedExceptions.filter(_.isDefined).map(_.get)
        }

        val collectedExceptions = time('OVERALL) {
            val result = (
                for { (classFile, source) ← project.classFilesWithSources.par }
                    yield analyzeClassFile(source.toString, classFile)
            ).flatten.seq.toSeq
            result.size //to force the evaluation
            result
        }

        if (collectedExceptions.nonEmpty) {
            val body =
                for ((exResource, exInstances) ← collectedExceptions.groupBy(e ⇒ e._1)) yield {
                    val exDetails =
                        exInstances.map { ex ⇒
                            val (_, classFile, method, throwable) = ex
                            <div>
                                <b>{ classFile.thisType.fqn }</b>
                                <i>"{ method.toJava }"</i><br/>
                                { "Length: "+method.body.get.instructions.length }
                                <div>{ XHTML.throwableToXHTML(throwable) }</div>
                            </div>
                        }

                    <section>
                        <h1>{ exResource }</h1>
                        <p>Number of thrown exceptions: { exInstances.size }</p>
                        { exDetails }
                    </section>
                }

            val node =
                util.XHTML.createXHTML(
                    Some("Exceptions Thrown During Interpretation"),
                    scala.xml.NodeSeq.fromSeq(body.toSeq))
            val file = writeAndOpen(node, "CrashedAbstractInterpretationsReport", ".html")

            fail(
                "During the interpretation of "+
                    methodsCount.get+" methods (of "+project.methodsCount+") in "+
                    project.classFilesCount+" classes (real time: "+ns2sec(getTime('OVERALL))+
                    "secs., ai (∑CPU Times): "+ns2sec(getTime('AI))+
                    "secs.)"+collectedExceptions.size+
                    " exceptions occured (details: "+file.toString+")."
            )
        } else {
            info(
                "No exceptions occured during the interpretation of "+
                    methodsCount.get+" methods (of "+project.methodsCount+") in "+
                    project.classFilesCount+" classes (real time: "+ns2sec(getTime('OVERALL))+
                    "secs., ai (∑CPU Times): "+ns2sec(getTime('AI))+
                    "secs.)"
            )
        }
    }

    // TODO Add a test to test that we can analyze "more" projects!
    //    it should ("be able to perform an abstract interpretation of the OPAL snapshot") in {
    //        val reader = new Java8FrameworkWithCaching(new BytecodeInstructionsCache)
    //        import reader.AllClassFiles
    //        val classFilesFolder = org.opalj.bi.TestSupport.locateTestResources("classfiles", "bi")
    //        val opalJARs = classFilesFolder.listFiles(new java.io.FilenameFilter() {
    //            def accept(dir: java.io.File, name: String) = name.startsWith("OPAL-")
    //        })
    //        info(opalJARs.mkString("analyzing the following jars: ", ", ", ""))
    //        opalJARs.size should not be (0)
    //        val project = Project(AllClassFiles(opalJARs))
    //
    //        val (message, source) =
    //            interpret(project, classOf[DefaultDomain[_]], false, None, 10)
    //
    //        if (source.nonEmpty)
    //            fail(message+" (details: "+source+")")
    //        else
    //            info(message)
    //    }
}
