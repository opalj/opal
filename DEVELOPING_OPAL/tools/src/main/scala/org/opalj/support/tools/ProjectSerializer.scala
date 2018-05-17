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
package support
package tools

import java.io.File
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

import org.opalj.io.process
import org.opalj.br.analyses.Project
import org.opalj.bc.Assembler
import org.opalj.br.analyses.SomeProject

/**
 * Exports the class files belonging to the project part of a loaded
 * [[org.opalj.br.analyses.Project]].
 * I.e., all code rewritings (e.g., INVOKEDYNAMIC resolution) and
 * bytecode optimizations become visible.
 *
 * Example execution:
 *    java org.opalj.br.ProjectSerializer -in <MyJar> -out <MyJarRewritten>
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object ProjectSerializer {

    private def showUsage(error: Option[String]): Unit = {
        println("OPAL - Project Serializer")
        println("Writes out the project's rewritten and transformed class files.")
        println("(No INVOKEDYNAMICS and optimized bytecode.)")
        error.foreach { e ⇒ println(); Console.err.println(e); println() }
        println("Parameters:")
        println("   -in <JAR File or Folder> a jar or a folder containing a project's class files")
        println("   -out <FileName> the folder where the class files are stored.")
        println()
        println("Example:")
        println("   java org.opalj.br.ProjectSerializer -jar <JAR File> -o <output folder>")
    }

    def main(args: Array[String]): Unit = {
        var in: String = null
        var out: String = null
        var i = 0
        while (i < args.length) {
            args(i) match {
                case "-in"  ⇒ { i += 1; in = args(i) }
                case "-out" ⇒ { i += 1; out = args(i) }

                case "-h" | "--help" ⇒
                    showUsage(error = None);
                    System.exit(0)
                case arg ⇒
                    showUsage(Some(s"Unsupported: $arg"))
                    System.exit(2)
            }
            i += 1
        }
        if (in == null || out == null) {
            showUsage(Some("Parameters missing."))
            System.exit(1)
        }
        val inFile = new File(in)
        if (!inFile.exists()) {
            Console.err.println(s"$in does not exist.")
            System.exit(1)
        }

        def checkOrCreateOutputFolder(outFolder: File): Unit = {
            if ((!outFolder.exists() && !outFolder.mkdirs()) ||
                (outFolder.exists() && !outFolder.isDirectory)) {
                Console.err.println(s"$out could not be created or is not a folder.")
                System.exit(1)
            }
        }
        val outFolder = new File(out)
        checkOrCreateOutputFolder(outFolder)
        val classesFolder = new File(outFolder.getPath + File.separator+"classes")
        checkOrCreateOutputFolder(classesFolder)

        val p = Project(inFile /* actually, we don't need the RTJar */ )
        serialize(p, classesFolder)
        println(s"Wrote all classfiles to $outFolder.")
    }

    def serialize(p: SomeProject, targetFolder: File): Unit = {
        val zipFile = new File(targetFolder.getAbsolutePath + File.separator+"project.zip")
        val zipOut = new ZipOutputStream(new FileOutputStream(zipFile))
        p.parForeachProjectClassFile() { cf ⇒
            val classFileFolderName = s"${targetFolder.getAbsolutePath}/${cf.thisType.packageName}"
            val classFileFolder = new File(classFileFolderName)
            classFileFolder.mkdirs()
            val classFileName = s"${cf.fqn}.class"
            val targetFile = new File(s"${targetFolder.getAbsolutePath}/$classFileName")

            val b = Assembler(ba.toDA(cf))
            process(new BufferedOutputStream(new FileOutputStream(targetFile))) {
                bos ⇒ bos.write(b)
            }
            zipOut.synchronized {
                val e = new ZipEntry(classFileName)
                zipOut.putNextEntry(e)
                zipOut.write(b, 0, b.length)
                zipOut.closeEntry()
            }
        }
        zipOut.close()
    }

}
