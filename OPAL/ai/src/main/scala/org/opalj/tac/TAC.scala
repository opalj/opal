/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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

import org.opalj.io.writeAndOpen
import org.opalj.io.OpeningFileFailedException
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.reader.Java8Framework
import java.util.jar.JarFile

/**
 * Creates the three-address representation and prints it.
 *
 * @author Michael Eichberg
 */
object TAC {

    private final val Usage =
        "Usage: java …TAC \n"+
            "(1) <JAR file containing class files>\n"+
            "(2) <class file name>\n"+
            "(3) <method name>\n"+
            "Example:\n\tjava …TAC /Library/jre/lib/rt.jar java.util.ArrayList toString"

    def processMethod(classFile: ClassFile, method: Method): Unit = {
        try {
            val tac = ToJavaLike(method)
            val fileNamePrefix = classFile.thisType.toJava+"."+method.name
            val file = writeAndOpen(tac, fileNamePrefix, ".tac.txt")
            println(s"Generated the tac file $file.")
        } catch {
            case OpeningFileFailedException(file, cause) ⇒
                println(s"Opening the tac file $file failed: ${cause.getMessage()}")
        }
    }

    def main(args: Array[String]): Unit = {

        if (args.length != 3) {
            println(Usage)
            sys.exit(-1)
        }

        val jarName = args(0)
        val classFiles = Java8Framework.ClassFiles(new java.io.File(jarName))
        if (classFiles.isEmpty) {
            println(s"No classfiles found in ${args(0)}")
        } else {
            val clazzName = args.drop(1).head
            val methodName = args.drop(2).head

            val classFile = classFiles.find(e ⇒ e._1.thisType.toJava == clazzName).map(_._1).get
            classFile.findMethod(methodName) match {
                case Some(method) ⇒
                    processMethod(classFile, method)
                case _ ⇒
                    println(
                        s"cannot find the method: $methodName "+
                            classFile.methods.map(_.name).mkString("(Available: ", ",", ")")
                    )
            }
        }
    }
}