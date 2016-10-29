/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

import java.io.File
import org.opalj.io.writeAndOpen
import org.opalj.io.OpeningFileFailedException
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext

/**
 * Disassembles the specified class file(s).
 *
 * @author Michael Eichberg
 */
object Disassembler {

    implicit val logContext = GlobalLogContext

    private final val Usage =
        "Usage: java …Disassembler \n"+
            "(1) <JAR file containing class files> [<Name of classfile (incl. path) contained in the JAR file>+]\n"+
            "(2) <class file>\n"+
            "Example:\n\tjava …Disassembler /Library/jre/lib/rt.jar java/util/ArrayList.class"

    def process(jarName: String, classFileName: String): Unit = {
        val fileName =
            if (classFileName.endsWith(".class"))
                classFileName
            else
                classFileName.replace('.', '/')+".class"

        val classFile = ClassFileReader.ClassFile(jarName, fileName).head
        processClassFile(classFile)
    }

    def processClassFile(classFile: ClassFile): Unit = {
        try {
            val file = writeAndOpen(classFile.toXHTML().toString, classFile.fqn, ".html")
            OPALLogger.info("progress", s"generated the HTML documentation $file")
        } catch {
            case OpeningFileFailedException(file, cause) ⇒ {
                val message = s"Opening the html file $file failed: ${cause.getMessage()}"
                OPALLogger.error("setup", message)
            }
        }
    }

    def main(args: Array[String]): Unit = {

        if (args.length < 1) {
            println(Usage)
            sys.exit(-1)
        }

        val jarName = args(0)
        val jarFile = new File(jarName)
        if (args.length == 1) {
            val classFiles = ClassFileReader.ClassFiles(jarFile)
            if (classFiles.isEmpty) {
                if (jarFile.exists())
                    OPALLogger.error("setup", s"no classfiles found in ${args(0)}")
                else
                    OPALLogger.error("setup", s"the specified file does not exist ${args(0)}")
            } else {
                classFiles.foreach(cfi ⇒ processClassFile(cfi._1))
            }
        } else {
            val classFileNames = args.drop(1) /* drop the name of the jar file */
            classFileNames.foreach(classFileName ⇒ process(jarName, classFileName))
        }
    }
}
