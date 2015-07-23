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
package tac

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.ParallelTestExecution
import org.opalj.bi.TestSupport.locateTestResources
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.br.reader

import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.io.DataInputStream
import java.io.ByteArrayInputStream
import org.opalj.bytecode.BytecodeProcessingFailedException

import org.opalj.bi.TestSupport.locateTestResources

/**
 * Tests the conversion of parsed methods to a quadruple representation
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class JDKTestTAC extends FunSpec with Matchers with ParallelTestExecution {

    describe("Behavior of \"OPAL\"") {
        val jreLibFolder: File = JRELibraryFolder
        val biClassfilesFolder: File = locateTestResources("classfiles", "bi")

        it("should be able to convert all classes to three-address code") {
            var errors: Seq[Throwable] = Seq.empty
            for {
                file ← jreLibFolder.listFiles() ++ biClassfilesFolder.listFiles()
                if file.isFile && file.canRead && file.getName.endsWith(".jar")
            } {
                reader.Java8Framework.ClassFiles(file) foreach { cs ⇒
                    val (cf, _) = cs
                    cf.methods.foreach { m ⇒
                        try AsQuadruples(m, None)
                        catch {
                            case e: Throwable ⇒ errors = errors ++ Seq[Throwable](e)
                        }
                    }
                }
            }
            assert(errors.equals(Seq.empty))
        }
    }
}