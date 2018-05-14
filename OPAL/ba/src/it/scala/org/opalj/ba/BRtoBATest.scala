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
import java.lang.{Boolean ⇒ JBoolean}
import java.io.File
import java.io.DataInputStream
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory

import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bi.TestResources.allBITestJARs
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java9FrameworkWithCaching
import org.opalj.br.reader.BytecodeOptimizer.SimplifyControlFlowKey

/**
 * Tests if we can convert every class file using the "Bytecode Representation" back to a
 * class file using the naive representation (Bytecode Disassembler).
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BRtoBATest extends FlatSpec with Matchers {

    behavior of "toDA(...br.ClassFile)"

    val ClassFileReader = {
        val testConfig = ConfigFactory.load().
            withValue(SimplifyControlFlowKey, ConfigValueFactory.fromAnyRef(JBoolean.FALSE))

        object Framework extends {
            override val config = testConfig
        } with Java9FrameworkWithCaching(new BytecodeInstructionsCache)
        Framework
    }

    def process(file: File): Unit = {
        val entriesCount = new AtomicInteger(0)

        val Lock = new Object
        var exceptions: List[Throwable] = Nil

        for { (brClassFile1, url) ← ClassFileReader.ClassFiles(file).par } {

            try {
                // PART 1... just serialize the file...
                // this may have - in comparison with the original class file -
                //  - a new (optimal) constant pool,
                //  - reordered fields,
                //  - reordered methods
                val daClassFile1 = toDA(brClassFile1)
                val rawClassFile1 = org.opalj.bc.Assembler(daClassFile1)

                // PART 2... recreate the class file from the serialized file
                val rawClassFileIn = new DataInputStream(new ByteArrayInputStream(rawClassFile1))
                val brClassFile2 = ClassFileReader.ClassFile(rawClassFileIn).head

                // PART 3... compare the class files...
                brClassFile1.findDissimilarity(brClassFile2) should be(None)

                entriesCount.incrementAndGet()
            } catch {
                case e: Exception ⇒
                    Lock.synchronized {
                    Console.err.println(s"reading/writing of $url -> failed: ${e.getMessage}\n")
                    e.printStackTrace(Console.err)
                        val details = e.getMessage +"; " + e.getClass.getSimpleName
                        val message = s"$url(${brClassFile1.thisType.toJava}): "+details
                        val newException = new RuntimeException(message, e)
                        exceptions = newException :: exceptions
                    }

            }
        }

        if (exceptions.nonEmpty) {
            val succeededCount = entriesCount.get
            val message =
                exceptions.mkString(
                    s"generating the naive representation failed ${exceptions.size} times:\n",
                    "\n",
                    s"\n(successfully processed: $succeededCount class files)\n"
                )
            fail(message)
        } else {
            info(s"successfully transformed ${entriesCount.get} class files")
        }
    }

    val jmodsFile = locateTestResources("classfiles/Java9-selected-jmod-module-info.classes.zip","bi")
    for {
        file ← JRELibraryFolder.listFiles() ++ allBITestJARs() ++ List(jmodsFile)
        if file.isFile
        if file.canRead
        if file.length() > 0
        fileName = file.getName
        if fileName.endsWith(".jar") || fileName.endsWith(".zip") || fileName.endsWith(".jmod")
    } {
        it should s"be able to convert every class of $file from br to ba" in { process(file) }
    }
}
