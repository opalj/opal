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
package bc

import java.io.File

import org.opalj.io.write
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import org.opalj.da._

/**
 * Command-line application which writes out some class files where some methods are filtered.
 *
 * @author Michael Eichberg
 */
object MethodFilter {

    implicit val logContext = GlobalLogContext

    private final val Usage =
        "Usage: java …MethodFilter \n"+
            "(1) <JAR file containing class files>\n"+
            "(2) <name of the class in binary notation (e.g. java/lang/Object>\n"+
            "(3) (+|-)<name of methods to keep/remove>\n"+
            "Example:\n\tjava …Disassembler /Library/jre/lib/rt.jar java/util/ArrayList +toString"

    def main(args: Array[String]): Unit = {

        if (args.length != 3) {
            println(Usage)
            sys.exit(-1)
        }

        val jarName = args(0)
        val className = args(1)
        val methodName = args(2).substring(1)
        val keepMethod = args(2).charAt(0) == '+'
        val classFiles = ClassFileReader.ClassFiles(new File(jarName)).map(_._1)
        if (classFiles.isEmpty) {
            OPALLogger.error("setup", s"no class files found in ${args(0)}")
        } else {
            classFiles.filter(_.thisType.asJVMType == className) foreach { cf ⇒
                val filteredMethods = cf.methods.filter { m ⇒
                    implicit val cp = cf.constant_pool
                    val matches = m.name == methodName
                    if (keepMethod)
                        matches
                    else
                        !matches
                }
                val filteredCF = cf.copy(methods = filteredMethods)
                val path = new File(cf.thisType+".class").toPath
                write(Assembler(filteredCF), path)
                OPALLogger.info("info", s"created new class file: $path")
            }
        }
    }
}
