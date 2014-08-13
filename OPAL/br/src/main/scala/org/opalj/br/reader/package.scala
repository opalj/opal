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
     * @param classFilesReader A function that – given a file (jar, folder, class file) – 
     *      loads the respective class files and returns an `Iterable`. The second
     *      parameter of the function is a function that should be called back by the
     *      reader whenever the processing of given file fails with an exception.
     *      This design was chosen to enable a reader of jar file to continue processing
     *      class files even if the processing of a class file failed.
     */
    def read(
        args: Iterable[String],
        classFilesReader: (File, (Exception) ⇒ Unit) ⇒ Iterable[(ClassFile, URL)]): (Iterable[(ClassFile, URL)], List[Exception]) = {
        readClassFiles(args.view.map(new File(_)), classFilesReader)
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
            try {
                perFile(file)
                classFilesReader(file, addException)
            } catch {
                case e: Exception ⇒
                    addException(new java.io.IOException("exception occured while processing: "+file, e))
                    Iterable.empty
            }
        }
        (allClassFiles.flatten.seq, exceptions)
    }
}




