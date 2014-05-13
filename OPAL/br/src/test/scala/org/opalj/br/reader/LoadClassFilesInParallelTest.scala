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
package br

import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.io.DataInputStream
import java.io.ByteArrayInputStream

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.ParallelTestExecution

/**
 * This test(suite) just loads a very large number of class files to make sure the library
 * can handle them and to test the "corner" cases. Basically, we test for NPEs,
 * ArrayIndexOutOfBoundExceptions and similar issues.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class LoadClassFilesInParallelTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    private def commonValidator(classFile: ClassFile, source: java.net.URL): Unit = {
        classFile.thisType.fqn should not be null
    }

    private def interfaceValidator(classFile: ClassFile, source: java.net.URL): Unit = {
        commonValidator(classFile, source)
        // the body of no method should be available
        classFile.methods.forall(m ⇒ m.body.isEmpty)
    }

    behavior of "OPAL"

    for {
        file ← TestSupport.locateTestResources("classfiles","bi").listFiles
        if (file.isFile && file.canRead && file.getName.endsWith(".jar"))
    } {
        it should ("be able to completely read all classes in the jar file"+file.getPath+" in parallel") in {
            reader.Java8Framework.ClassFiles(file) foreach { cs ⇒
                val (cf, s) = cs
                commonValidator(cf, s)
            }
        }
        it should ("be able to read the public interface of all classes in the jar file"+file.getPath+" in parallel") in {
            reader.Java8LibraryFramework.ClassFiles(file) foreach { cs ⇒
                val (cf, s) = cs
                interfaceValidator(cf, s)
            }
        }
    }
}
