/* License (BSD Style License):
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved

import java.io.File
import java.net.URL

/**
 * Defines convenience methods related to reading in class files.
 *
 * @author Michael Eichberg
 */
package object reader {

    /**
     * Reads in all class files found in the jar files or jar and class files in the
     * folders specified by `args`. The class files are read in using the specified
     * class file reader. This enables, e.g., to use this method to only read in
     * the public interface of a class file or to read in complete class files.
     *
     * @param args An `Iterable` of file and folder names that refer to jar files
     *      or folders in which jar and class files are found.
     */
    def read(
        args: Iterable[String],
        classFilesReader: (File, (Exception) ⇒ Unit) ⇒ Iterable[(ClassFile, URL)]): (Iterable[(ClassFile, URL)], List[Exception]) = {
        readClassFiles(args.map(new File(_)), classFilesReader)
    }

    def readClassFiles(
        files: Iterable[File],
        classFilesReader: (File, (Exception) ⇒ Unit) ⇒ Iterable[(ClassFile, URL)],
        perFile: File ⇒ Unit = (f: File) ⇒ { /*do nothing*/ }): (Iterable[(ClassFile, URL)], List[Exception]) = {
        val exceptionsMutex = new Object
        var exceptions: List[Exception] = Nil
        def addException(e: Exception) {
            exceptionsMutex.synchronized { exceptions = e :: exceptions }
        }

        val allClassFiles = for (file ← files.par) yield {
            perFile(file)
            classFilesReader(file, addException)
        }
        (allClassFiles.flatten.seq, exceptions)
    }
}




