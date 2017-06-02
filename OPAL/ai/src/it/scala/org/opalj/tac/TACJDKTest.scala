/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package tac

import scala.language.existentials

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.opalj.bi.TestSupport.locateTestResources
import org.opalj.bytecode.JRELibraryFolder
import java.io.File

import org.opalj.bi.TestSupport.locateTestResources
import org.opalj.br.analyses.Project
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.Domain
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.br.analyses.SomeProject

/**
 * Tests that all methods of the JDK can be converted to a three address representation.
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class TACJDKTest extends FunSpec with Matchers {

    val jreLibFolder: File = JRELibraryFolder
    val biClassfilesFolder: File = locateTestResources("classfiles", "bi")

    describe("creating the three-address representation") {

        def checkFolder(
            folder:        File,
            domainFactory: Option[(SomeProject, ClassFile, Method) ⇒ Domain with RecordDefUse] = None
        ): Unit = {
            var errors: List[(String, Throwable)] = Nil
            val successfullyCompleted = new java.util.concurrent.atomic.AtomicInteger(0)
            val mutex = new Object
            for {
                file ← folder.listFiles()
                if file.isFile && file.canRead && file.getName.endsWith(".jar")
                project = Project(file)
                ch = project.classHierarchy
                cf ← project.allProjectClassFiles.par
                m ← cf.methods
                body <-  m.body
                aiResult = domainFactory.map { f ⇒ BaseAI(cf, m, f(project, cf, m)) }
            } {
                try {
                    // without using AIResults
                    val (tacNaiveCode, _) = TACNaive(
                        method = m,
                        classHierarchy = project.classHierarchy,
                        optimizations = AllTACNaiveOptimizations
                    )
                    ToJavaLike(tacNaiveCode)

                    // using AIResults (if available)
                    if(aiResult.isDefined) {
                        val (tacAICode, _) = TACAI(m, project.classHierarchy, aiResult.get)(List.empty)
                        ToJavaLike(tacAICode)
                    }
                } catch {
                    case e: Throwable ⇒ this.synchronized {
                        val methodSignature = m.toJava(cf)
                        mutex.synchronized {
                            println(methodSignature+" - size: "+body.instructions.length)
                            e.printStackTrace()
                            if (e.getCause != null) {
                                println("\tcause:")
                                e.getCause.printStackTrace()
                            }
                            println(body.instructions.zipWithIndex.filter(_._1 != null).map(_.swap).mkString("Instructions:\n\t","\n\t","\n"))
                            errors ::= ((file+":"+methodSignature, e))
                        }
                    }
                }
                successfullyCompleted.incrementAndGet()
            }
            if (errors.nonEmpty) {
                val message =
                    errors.
                        map(_.toString()+"\n").
                        mkString(
                            "Errors thrown:\n",
                            "\n",
                            "Number of Successfully completed:"+successfullyCompleted.get+
                                "; Number of Errors: "+errors.size+"\n"
                        )
                fail(message)
            }
        }

        describe("using the results of an abstract interpretation") {

            val domainFactory = Some((p: SomeProject, cf: ClassFile, m: Method) ⇒ {
                new DefaultDomainWithCFGAndDefUse(p, cf, m)
            })

            it("it should be able to create fully typed TAC for the set of collected class files") {
                time {
                    checkFolder(biClassfilesFolder, domainFactory)
                } { t ⇒ info(s"conversion took ${t.toSeconds}") }
            }

            it("it should be able to create fully typed TAC for the JDK") {
                time {
                    checkFolder(jreLibFolder, domainFactory)
                } { t ⇒ info(s"conversion took ${t.toSeconds}") }
            }
        }

        describe("using the naive transformation approach") {

            it("it should be able to convert all methods of the JDK") {
                time {
                    checkFolder(jreLibFolder)
                } { t ⇒ info(s"conversion took ${t.toSeconds}") }
            }

            it("it should be able to convert all methods of the set of collected class files") {
                time {
                    checkFolder(biClassfilesFolder)
                } { t ⇒ info(s"conversion took ${t.toSeconds}") }
            }

        }
    }
}
