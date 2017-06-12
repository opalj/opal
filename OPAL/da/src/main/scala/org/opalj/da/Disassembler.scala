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
package da

import java.io.File
import java.nio.file.Files

import org.apache.commons.lang3.StringUtils.getLevenshteinDistance
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.{Error ⇒ ErrorLogLevel}

/**
 * Disassembles the specified class file(s).
 *
 * @author Michael Eichberg
 */
object Disassembler {

    OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, ErrorLogLevel))

    private final val Usage = {
        "Usage: java …Disassembler \n"+
            "       [-o <File> the name of the file to which the generated html page should be written]\n"+
            "       [-open the generated html page will be opened in a browser]\n"+
            "       [-source <File> a class or jar file or a directory containg jar or class files]*\n"+
            "       <ClassName> name of the class for which we want to create the HTML page\n"+
            "Example:\n       java …Disassembler -source /Library/jre/lib/rt.jar java.util.ArrayList"
    }

    def handleError(error: String): Nothing = {
        Console.err.println("Error: "+error)
        Console.out.println(Usage)
        sys.exit(1)
    }

    def main(args: Array[String]): Unit = {
        // OPTIONS
        var toStdOut = true
        var toFile: Option[String] = None
        var openHTMLFile: Boolean = false
        var sources: List[String] = List.empty
        var className: String = null

        // PARSING PARAMETERS
        var i = 0
        def readNextArg(): String = {
            i += 1
            if (i < args.length) {
                args(i)
            } else {
                handleError(args.mkString("missing argument: ", " ", ""))
            }
        }
        while (i < args.length) {
            args(i) match {
                case "-o"      ⇒ { toFile = Some(readNextArg()); toStdOut = false }
                case "-open"   ⇒ { openHTMLFile = true; toStdOut = false }
                case "-source" ⇒ sources ::= readNextArg()
                case cName     ⇒ className = cName.replace('/', '.')
            }
            i += 1
        }

        // VALIDATING PARAMETERS
        if (className == null) handleError("missing class name")

        if (sources.isEmpty) sources = List(System.getProperty("user.dir"))
        val sourceFiles = sources map { src ⇒
            val f = new File(src)
            if (!f.exists()) handleError("file does not exist: "+src)
            if (!f.canRead) handleError("cannot read: "+src)
            f
        }

        val classNameAsFileName: String = org.opalj.io.sanitizeFileName(className)

        val targetFile: Option[File] =
            if (openHTMLFile) {
                if (toFile.isEmpty)
                    Some(File.createTempFile(classNameAsFileName, ".html"))
                else {
                    val f = new File(toFile.get)
                    if (f.exists() && !f.canWrite) handleError("cannot update: "+f)
                    Some(f)
                }
            } else if (toFile.isDefined) {
                val f = new File(toFile.get)
                if (f.exists() && !f.canWrite) handleError("cannot update: "+f)
                Some(f)
            } else {
                None
            }

        val classFiles = ClassFileReader.AllClassFiles(sourceFiles)
        if (classFiles.isEmpty) handleError(sources.mkString("cannot find class files in: ", ", ", ""))
        val classFileOption = classFiles find { e ⇒ val (cf, _) = e; cf.thisType == className }
        val classFile: ClassFile = classFileOption match {
            case None ⇒
                val allClassNames: List[(Int, String)] =
                    classFiles.map { cf ⇒
                        (getLevenshteinDistance(className, cf._1.thisType), cf._1.thisType)
                    }.toList

                val mostRelated = allClassNames.sortWith((l, r) ⇒ l._1 < r._1).map(_._2).take(15)
                val ending = if (mostRelated.length > 15) ", ...)" else ")"
                val messageHeader = "can't find: "+className
                val message = mostRelated.mkString(s"$messageHeader (similar: ", ", ", ending)
                handleError(message)

            case Some((cf, _)) ⇒ cf
        }

        // FINAL PROCESSING
        val xHTML = classFile.toXHTML().toString
        targetFile match {
            case Some(f) ⇒
                Files.write(f.toPath, xHTML.toString.getBytes("UTF-8"))
                println("wrote: "+f)
                if (openHTMLFile) org.opalj.io.open(f)
            case None ⇒
                Console.out.println(xHTML)
        }

    }
}
