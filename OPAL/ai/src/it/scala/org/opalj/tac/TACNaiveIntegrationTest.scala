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

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bytecode.JRELibraryFolder
import java.io.File

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.analyses.Project
import org.opalj.util.PerformanceEvaluation.time

/**
 * Tests that all methods of the JDK can be converted to a three address representation.
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class TACNaiveIntegrationTest extends FunSpec with Matchers {

    val jreLibFolder: File = JRELibraryFolder
    val biClassfilesFolder: File = locateTestResources("classfiles", "bi")

    def checkFolder(folder: File): Unit = {
        if (Thread.currentThread().isInterrupted) return ;

        var errors: List[(String, Throwable)] = Nil
        val successfullyCompleted = new java.util.concurrent.atomic.AtomicInteger(0)
        val mutex = new Object
        for {
            file ← folder.listFiles()
            if !Thread.currentThread().isInterrupted
            if file.isFile && file.canRead && file.getName.endsWith(".jar")
            project = Project(file)
            ch = project.classHierarchy
            cf ← project.allProjectClassFiles.par
            if !Thread.currentThread().isInterrupted
            m ← cf.methods
            body ← m.body
        } {
            try {
                // without using AIResults
                val TACode(params, tacNaiveCode,_, cfg, _, _) = TACNaive(
                    method = m,
                    classHierarchy = ch,
                    optimizations = AllTACNaiveOptimizations
                )
                ToTxt(params, tacNaiveCode, cfg, true, true, true)
            } catch {
                case e: Throwable ⇒ this.synchronized {
                    val methodSignature = m.toJava
                    mutex.synchronized {
                        println(methodSignature+" - size: "+body.instructions.length)
                        e.printStackTrace(Console.out)
                        if (e.getCause != null) {
                            println("\tcause:")
                            e.getCause.printStackTrace()
                        }
                        println(
                            body.instructions.
                                zipWithIndex.
                                filter(_._1 != null).
                                map(_.swap).
                                mkString("Instructions:\n\t", "\n\t", "\n")
                        )
                        println(
                            body.exceptionHandlers.mkString("Exception Handlers:\n\t", "\n\t", "\n")
                        )
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
                        "successfully transformed methods: "+successfullyCompleted.get+
                            "; failed methods: "+errors.size+"\n"
                    )
            fail(message)
        }
    }

    describe("creating the three-address representation using the naive transformation approach") {

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
