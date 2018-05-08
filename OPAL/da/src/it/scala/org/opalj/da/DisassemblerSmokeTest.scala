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
package da

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers

import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

import org.opalj.concurrent.OPALExecutionContextTaskSupport
import org.opalj.util.PerformanceEvaluation

/**
 * This test(suite) just loads a very large number of class files and creates
 * the xHTML representation of the classes. It basically tests if we can load and
 * process a large number of different classes without exceptions (smoke test).
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DisassemblerSmokeTest extends FunSpec with Matchers {

    describe("the Disassembler") {

        for { file ← bi.TestResources.allBITestJARs ++ Traversable(bytecode.JRELibraryFolder) } {

            describe(s"(when processing $file)") {

                val classFiles: List[(ClassFile, URL)] = {
                    var exceptions: List[(AnyRef, Throwable)] = Nil

                    val classFiles = PerformanceEvaluation.time {
                        val Lock = new Object
                        val exceptionHandler = (source: AnyRef, throwable: Throwable) ⇒ {
                            Lock.synchronized {
                                exceptions ::= ((source, throwable))
                            }
                        }

                        val classFiles = ClassFileReader.ClassFiles(file, exceptionHandler)

                        // Check that we have something to process...
                        if (file.getName() != "Empty.jar" && classFiles.isEmpty) {
                            throw new UnknownError(s"the file/folder $file is empty")
                        }

                        classFiles
                    } { t ⇒ info(s"reading took ${t.toSeconds}") }

                    info(s"loaded ${classFiles.size} class files")

                    it(s"reading should not result in exceptions") {
                        if (exceptions.nonEmpty) {
                            info(exceptions.mkString(s"exceptions while reading $file:\n", "\n", ""))
                            fail(s"reading of $file resulted in ${exceptions.size} exceptions")
                        }
                    }

                    classFiles
                }

                it(s"should be able to create the xHTML representation for every class") {

                    val classFilesGroupedByPackage = classFiles.groupBy { e ⇒
                        val (classFile, _ /*url*/ ) = e
                        val fqn = classFile.thisType.asJava
                        if (fqn.contains('.'))
                            fqn.substring(0, fqn.lastIndexOf('.'))
                        else
                            "<default>"
                    }
                    info(s"identified ${classFilesGroupedByPackage.size} packages")

                    val exceptions: Iterable[(URL, Exception)] =
                        (for { (packageName, classFiles) ← classFilesGroupedByPackage } yield {
                            val transformationCounter = new AtomicInteger(0)
                            info(s"processing $packageName")
                            val parClassFiles = classFiles.par
                            parClassFiles.tasksupport = OPALExecutionContextTaskSupport
                            PerformanceEvaluation.time {
                                val exceptions = (
                                    for { (classFile, url) ← parClassFiles } yield {
                                        var result: Option[(URL, Exception)] = None
                                        try {
                                            classFile.toXHTML(None).label should be("html")
                                            transformationCounter.incrementAndGet()
                                        } catch {
                                            case e: Exception ⇒ result = Some((url, e))
                                        }
                                        result
                                    }
                                ).seq.flatten
                                info(s"transformed ${transformationCounter.get} class files in $packageName")
                                exceptions
                            } { t ⇒ info(s"transformation (parallelized) took ${t.toSeconds}") }
                        }).flatten

                    if (exceptions.nonEmpty) {
                        info(exceptions.mkString(s"exceptions while reading $file:\n", "\n", ""))
                        fail(s"reading of $file resulted in ${exceptions.size} exceptions")
                    }
                }
            }
        }
    }
}
