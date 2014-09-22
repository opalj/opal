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

import org.opalj.util.writeAndOpen
import org.opalj.util.OpeningFileFailedException

/**
 * Disassembles the specified class file(s).
 *
 * @author Michael Eichberg
 */
object Disassembler {

    def main(args: Array[String]) {

        if (args.length < 1) {
            println("Usage: java …Disassembler "+
                "<JAR file containing class files> "+
                "[<Name of classfile (incl. path) contained in the JAR file>+]") //
            println("Example:\n\tjava …Disassembler /Library/Java/JavaVirtualMachines/jdk1.7.0_51.jdk/Contents/Home/jre/lib/rt.jar java/util/ArrayList.class")
            sys.exit(-1)
        }

        def process(classFileName: String) {
            val classFile = ClassFileReader.ClassFile(args(0), classFileName).head
            processClassFile(classFile)
        }

        def processClassFile(classFile: ClassFile) {
            try {
                writeAndOpen(classFile.toXHTML.toString, classFile.fqn, ".html")
            } catch {
                case OpeningFileFailedException(file, cause) ⇒
                    println(
                        s"Opening the html file $file failed: ${cause.getMessage()}"
                    )
                case ex: Throwable ⇒ throw ex
            }
        }

        if (args.length == 1) {
            for ((classFile, _) ← ClassFileReader.ClassFiles(new java.io.File(args(0)))) {
                processClassFile(classFile)
            }
        } else
            for (classFileName ← args.drop(1) /* drop the name of the jar file */ ) {
                process(classFileName)
            }
    }
}
