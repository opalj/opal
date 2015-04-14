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

import java.io.File
import java.net.URL
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.util.control.ControlThrowable
import org.opalj.io.writeAndOpen
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.MethodWithBody
import org.opalj.ai.common.XHTML
import org.opalj.br.analyses.Project
import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.log.GlobalLogContext

/**
 * Infrastructure to just load a very large number of class files and performs
 * an abstract interpretation of all methods. It basically tests if we can load and
 * process a large number of different classes without exceptions.
 *
 * @author Michael Eichberg
 */
abstract class DomainTestInfrastructure(domainName: String) extends FlatSpec with Matchers {

    private[this] implicit val logContext = GlobalLogContext

    def Domain(project: Project[URL], classFile: ClassFile, method: Method): Domain

    def analyzeProject(
        projectName: String,
        project: Project[URL],
        maxEvaluationFactor: Double): Unit = {

        val performanceEvaluationContext = new org.opalj.util.PerformanceEvaluation
        import performanceEvaluationContext.{ time, getTime }
        val methodsCount = new java.util.concurrent.atomic.AtomicInteger(0)

        def analyzeClassFile(
            source: String,
            classFile: ClassFile,
            method: Method): Option[(String, ClassFile, Method, Throwable)] = {

            val body = method.body.get
            try {
                time('AI) {
                    val ai =
                        new InstructionCountBoundedAI[Domain](
                            body,
                            maxEvaluationFactor)
                    val result =
                        ai.apply(classFile, method, Domain(project, classFile, method))
                    if (result.wasAborted) {
                        throw new InterruptedException(
                            "evaluation bound (max="+ai.maxEvaluationCount +
                                s") exceeded (maxStack=${body.maxStack}; maxLocals=${body.maxLocals})")
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

        val collectedExceptions = time('OVERALL) {
            val exceptions = new java.util.concurrent.ConcurrentLinkedQueue[(String, ClassFile, Method, Throwable)]()
            project.parForeachMethodWithBody() { (m) ⇒
                val (source, classFile, method) = m
                analyzeClassFile(source.toString, classFile, method) foreach {
                    exceptions.add(_)
                }
            }
            import scala.collection.JavaConverters._
            exceptions.asScala
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
                XHTML.createXHTML(
                    Some("Exceptions Thrown During Interpretation"),
                    scala.xml.NodeSeq.fromSeq(body.toSeq))
            val file =
                writeAndOpen(
                    node,
                    "CrashedAbstractInterpretationsReportFor"+projectName, ".html")

            fail(
                projectName+": "+
                    "During the interpretation of "+
                    methodsCount.get+" methods (of "+project.methodsCount+") in "+
                    project.classFilesCount+" classes (real time: "+getTime('OVERALL)+
                    ", ai (∑CPU Times): "+getTime('AI)+
                    ")"+collectedExceptions.size+
                    " exceptions occured (details: "+file.toString+")."
            )
        } else {
            info(
                projectName+": "+
                    "No exceptions occured during the interpretation of "+
                    methodsCount.get+" methods (of "+project.methodsCount+") in "+
                    project.classFilesCount+" classes (real time: "+getTime('OVERALL)+
                    ", ai (∑CPU Times): "+getTime('AI)+")"
            )
        }
    }

    val reader = new Java8FrameworkWithCaching(new BytecodeInstructionsCache)

    behavior of domainName

    it should ("be able to perform an abstract interpretation of the JRE's classes") in {
        val project = org.opalj.br.TestSupport.createJREProject

        analyzeProject("JDK", project, 4d)
    }

    it should ("be able to perform an abstract interpretation of the OPAL 0.3 snapshot") in {
        val classFiles = org.opalj.bi.TestSupport.locateTestResources("classfiles/OPAL-SNAPSHOT-0.3.jar", "bi")
        val project = Project(reader.ClassFiles(classFiles), Traversable.empty)

        analyzeProject("OPAL-0.3", project, 2.5d)
    }

    it should ("be able to perform an abstract interpretation of the project OPAL-08-14-2014 snapshot") in {

        import reader.AllClassFiles
        val classFilesFolder = org.opalj.bi.TestSupport.locateTestResources("classfiles", "bi")
        val opalJARs = classFilesFolder.listFiles(new java.io.FilenameFilter() {
            def accept(dir: java.io.File, name: String) =
                name.startsWith("OPAL-") && name.contains("SNAPSHOT-08-14-2014")
        })
        info(opalJARs.mkString("analyzing the following jars: ", ", ", ""))
        opalJARs.size should not be (0)
        val project = Project(AllClassFiles(opalJARs), Traversable.empty)

        analyzeProject("OPAL-SNAPSHOT-08-14-2014", project, 1.5d)
    }

}

