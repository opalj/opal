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
package da

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

/**
 * This system test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods. It basically tests if we can load and
 * process a large number of different classes without exceptions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DisassemblerTest extends FlatSpec with Matchers {

    behavior of "the Disassembler"

    //val files = new java.io.File("/users/eichberg/Applications/Scala IDE")
    val files = org.opalj.bi.TestSupport.JRELibraryFolder

    it should (s"be able to process every class of $files") in {

        val Lock = new Object
        var exceptions: List[Exception] = Nil

        val classFiles =
            ClassFileReader.ClassFiles(
                files,
                { (exception: Exception) ⇒
                    Lock.synchronized { exceptions = exception :: exceptions }
                }
            )
        exceptions should be('empty)
        classFiles.isEmpty should be(false)
        info(s"loaded ${classFiles.size} class files")

        val classFilesGroupedByPackage = classFiles.groupBy { e ⇒
            val (classFile, url) = e
            val fqn = classFile.fqn
            if (fqn.contains('.'))
                fqn.substring(0, fqn.lastIndexOf('.'))
            else
                "<default>"
        }
        info(s"identified ${classFilesGroupedByPackage.size} packages")

        val transformationCounter = new java.util.concurrent.atomic.AtomicInteger(0)
        for {
            groupedClassFiles ← classFilesGroupedByPackage
        } yield {
            val (packageName, classFiles) = groupedClassFiles
            info("processing package "+packageName)
            for { (classFile, url) ← classFiles.par } {
                try {
                    classFile.toXHTML.toString.length() should be > (0)
                    transformationCounter.incrementAndGet()
                    // ideally: should be valid HTML
                } catch {
                    case e: Exception ⇒
                        Console.err.println(s"identified an issue: "+e.getMessage())
                        Lock.synchronized {
                            exceptions =
                                new RuntimeException(s"failed: $url; message:"+e.getMessage(), e) ::
                                    exceptions
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
            val file =
                org.opalj.util.writeAndOpenDesktopApplication(
                    new String(out.toByteArray()),
                    "bytecode disassembler - exceptions", ".txt"
                )

            fail(exceptions.map(_.getMessage()).
                mkString("Exceptions:\n", "\n", "Details: "+file))
        }
        info(s"transformed ${transformationCounter.get} class files")
    }
}
