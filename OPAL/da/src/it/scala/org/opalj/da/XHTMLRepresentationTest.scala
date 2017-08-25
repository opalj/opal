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
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.opalj.io.writeAndOpen

/**
 * This test(suite) just loads a very large number of class files and creates
 * the xHTML representation of the classes. It basically tests if we can load and
 * process a large number of different classes without exceptions (smoke test).
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class XHTMLRepresentationTest extends FlatSpec with Matchers {

    behavior of "the Disassembler"

    for { file ← bi.TestResources.allBITestJARs ++ Traversable(bytecode.JRELibraryFolder) } {

        it should s"be able to create the xHTML representation of every class of $file" in {

            val Lock = new Object
            var exceptions: List[Throwable] = Nil
            val exceptionHandler = (source: AnyRef, exception: Throwable) ⇒ {
                Lock.synchronized { exceptions ::= exception }
            }

            val classFiles = ClassFileReader.ClassFiles(file, exceptionHandler)

            exceptions should be('empty)
            if (file.getName() != "Empty.jar") { classFiles.isEmpty should be(false) }
            info(s"loaded ${classFiles.size} class files")

            val classFilesGroupedByPackage = classFiles.groupBy { e ⇒
                val (classFile, _ /*url*/ ) = e
                val fqn = classFile.thisType.asJava
                if (fqn.contains('.'))
                    fqn.substring(0, fqn.lastIndexOf('.'))
                else
                    "<default>"
            }
            info(s"identified ${classFilesGroupedByPackage.size} packages")

            val transformationCounter = new java.util.concurrent.atomic.AtomicInteger(0)
            for (groupedClassFiles ← classFilesGroupedByPackage) {
                val (packageName, classFiles) = groupedClassFiles
                info("processing package "+packageName)
                val parClassFiles = classFiles.par
                parClassFiles.tasksupport = org.opalj.concurrent.OPALExecutionContextTaskSupport
                parClassFiles.foreach { e ⇒
                    val (classFile, url) = e
                    try {
                        classFile.toXHTML(None).toString.length() should be > (0)
                        transformationCounter.incrementAndGet()
                        // ideally: should be valid HTML
                    } catch {
                        case e: Exception ⇒ Lock.synchronized {
                            val message = s"failed: $url; message:"+e.getMessage()
                            val newException = new RuntimeException(message, e)
                            exceptions = newException :: exceptions
                        }
                    }
                }
            }
            if (exceptions.nonEmpty) {
                val out = new java.io.ByteArrayOutputStream
                val writer = new java.io.PrintWriter(out)
                exceptions.foreach { e ⇒
                    writer.println(e.getMessage())
                    e.getCause().printStackTrace(writer)
                    writer.println("\n")
                }
                writer.flush()
                val exceptionsAsText = new String(out.toByteArray())
                val fileName = "bytecode disassembler - exceptions"
                val file = writeAndOpen(exceptionsAsText, fileName, ".txt")

                fail(exceptions.map(_.getMessage()).
                    mkString("Exceptions:\n", "\n", "Details: "+file))
            }
            info(s"transformed ${transformationCounter.get} class files")
        }
    }
}
