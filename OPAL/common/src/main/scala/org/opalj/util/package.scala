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

import java.io.File
import java.io.IOException

import scala.xml.Node
import scala.util.control.ControlThrowable

/**
 * Various helper methods.
 *
 * @author Michael Eichberg
 */
package object util {

    /**
     * Writes the XML document to a temporary file and opens the file in the
     * OS's default application.
     *
     * @param filenamePrefix A string the identifies the content of the file. (E.g.,
     *      "ClassHierarchy" or "CHACallGraph")
     * @param filenameSuffix The suffix of the file that identifies the used file format.
     *      (E.g., ".xhtml")
     * @return The name of the file if it was possible to write the file and open
     *      the native application.
     */
    @throws[IOException]("if it is not possible to create a temporary file")
    @throws[OpeningFileFailedException]("if it is not possible to open the file")
    def writeAndOpen(
        node: Node,
        filenamePrefix: String,
        filenameSuffix: String): File = {

        val data = node.toString
        writeAndOpen(data, filenamePrefix, filenameSuffix)
    }

    /**
     * Writes the given string (`data`) to a temporary file using the given prefix and suffix.
     * Afterwards the system's native application that claims to be able to handle
     * files with the given suffix is opened. If this fails, the string is printed to
     * the console.
     *
     * The string is always written using UTF-8 as the encoding.
     *
     * @param filenamePrefix A string the identifies the content of the file. (E.g.,
     *      "ClassHierarchy" or "CHACallGraph")
     * @param filenameSuffix The suffix of the file that identifies the used file format.
     *      (E.g., ".txt")
     * @return The name of the file if it was possible to write the file and open
     *      the native application.
     * @example
     *      Exemplary usage:
     *      {{{
     *      try {
     *          util.writeAndOpen("The Message", "Result", ".txt")
     *      } catch {
     *          case OpeningFileFailedException(file, _) ⇒
     *              Console.err.println("Details can be found in: "+file.toString)
     *      }}}
     */
    @throws[IOException]("if it is not possible to create a temporary file")
    @throws[OpeningFileFailedException]("if it is not possible to open the file")
    def writeAndOpen(
        data: String,
        filenamePrefix: String,
        filenameSuffix: String): File = {

        val file = File.createTempFile(filenamePrefix, filenameSuffix)
        process { new java.io.FileOutputStream(file) } { fos ⇒
            fos.write(data.getBytes("UTF-8"))
        }

        try {
            java.awt.Desktop.getDesktop().open(file)
        } catch {
            case ct: ControlThrowable ⇒ throw ct
            case t: Throwable         ⇒ new OpeningFileFailedException(file, t)
        }

        file
    }
}
