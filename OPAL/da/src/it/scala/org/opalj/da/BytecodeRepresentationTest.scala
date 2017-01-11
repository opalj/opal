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

import java.util.zip.ZipFile
import java.io.DataInputStream
import java.io.BufferedInputStream
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters._

import org.opalj.da.ClassFileReader.{ClassFile ⇒ LoadClassFile}

/**
 * Tests the bytecode representation.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BytecodeRepresentationTest extends FlatSpec with Matchers {

    behavior of "the Disassembler"

    val file = org.opalj.bytecode.RTJar // has to be a single JAR file!

    it should (s"be able to process every class of $file") in {

        val zipFile = new ZipFile(file)
        val entriesCount = new AtomicInteger(0)

        val Lock = new Object
        var exceptions: List[Throwable] = Nil

        zipFile.entries().asScala.filter(_.getName.endsWith(".class")).toList.par.foreach { ze ⇒

            val classFile = LoadClassFile {
                new DataInputStream(new BufferedInputStream(zipFile.getInputStream(ze), ze.getSize().toInt))
            }.head
            try {
                assert(ze.getSize == classFile.size)
                entriesCount.incrementAndGet()
            } catch {
                case e: Exception ⇒ Lock.synchronized {
                    val message = s"failed: $ze(${classFile.fqn}); message:"+e.getMessage()
                    val newException = new RuntimeException(message, e)
                    exceptions = newException :: exceptions
                }
            }
        }

        if (exceptions.nonEmpty) {
            val message =
                exceptions.mkString(
                    s"creating the bytecode representation failed for :\n",
                    "\n",
                    s"\n${exceptions.size}(out of ${entriesCount}) entries\n"
                )
            fail(message)
        } else {
            info(s"sucessfully processed ${entriesCount.get} entries")
        }
    }
}
