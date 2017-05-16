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
package ba

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import java.io.File
import java.io.DataInputStream
import java.io.ByteArrayInputStream
import java.io.BufferedInputStream
import java.util.zip.ZipFile
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.JavaConverters._

import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bi.TestSupport.allBITestJARs
import org.opalj.br.reader.Java8Framework

/**
 * Tests if we can convert every class file using the "Bytecode Representation" back to a
 * class file using the naive representation (Bytecode Disassembler).
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BRtoBATest extends FlatSpec with Matchers {

    behavior of "toDA(...br.ClassFile)"

    def process(file: File): Unit = {
        val zipFile = new ZipFile(file)
        val entriesCount = new AtomicInteger(0)

        val Lock = new Object
        var exceptions: List[Throwable] = Nil

        zipFile.entries().asScala.filter(_.getName.endsWith(".class")).toList.par.foreach { ze ⇒

            val brClassFile1 = {
                val file = zipFile.getInputStream(ze)
                val classFileSize = ze.getSize.toInt
                val raw = new Array[Byte](classFileSize)
                val bin = new BufferedInputStream(file, classFileSize)
                val bytesRead = bin.read(raw, 0, classFileSize)
                assert(bytesRead == classFileSize, "reading the zip file failed")
                Java8Framework.ClassFile(new DataInputStream(new ByteArrayInputStream(raw))).head
            }

            try {
                // PART 1... just serialize the file...
                // this may have - in comparison with the original class file:
                //  - a new (optimal) constant pool,
                //  - reordered fiels,
                //  - reordered methods
                val daClassFile1 = toDA(brClassFile1)
                val rawClassFile1 = org.opalj.bc.Assembler(daClassFile1)

                // PART 2... recreate the class file from the serialized file
                val rawClassFileIn = new DataInputStream(new ByteArrayInputStream(rawClassFile1))
                /*val brClassFile2 =*/ Java8Framework.ClassFile(rawClassFileIn).head

                // PART 3... compare the class files...
                brClassFile1.findStructuralInequality(brClassFile2) should be(None)

                entriesCount.incrementAndGet()
            } catch {
                case e: Exception ⇒
                    println(" -> failed: "+e.getMessage)
                    Lock.synchronized {
                        val details = e.getMessage + e.getClass.getSimpleName
                        val message = s"failed: $ze(${brClassFile1.thisType}); message:"+details
                        val newException = new RuntimeException(message, e)
                        exceptions = newException :: exceptions
                    }
            }
        }

        if (exceptions.nonEmpty) {
            val succeededCount = entriesCount.get
            val message =
                exceptions.mkString(
                    s"generating the naive representation failed for :\n",
                    "\n",
                    s"\n${exceptions.size} class files (and succeeded for: $succeededCount)\n"
                )
            fail(message)
        } else {
            info(s"sucessfully transformed ${entriesCount.get} class files")
        }
    }

    for {
        file ← JRELibraryFolder.listFiles() ++ allBITestJARs()
        if file.isFile && file.canRead && file.getName.endsWith(".jar") && file.length() > 0
    } {
        it should (s"be able to process every class of $file") in { process(file) }
    }
}
