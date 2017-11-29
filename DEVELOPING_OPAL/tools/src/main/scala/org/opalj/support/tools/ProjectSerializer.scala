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

import java.io.{BufferedOutputStream, File, FileOutputStream}

import org.opalj.ba
import org.opalj.bc.Assembler
import org.opalj.br.analyses.{Project, SomeProject}
import org.opalj.bytecode.RTJar
import org.opalj.io.process

/**
 * Program to export a project loaded with OPAL with all code rewriting, for example INVOKEDYNAMIC
 * resolution.
 *
 * Example execution:
 *     sbt "OPAL-Validate/runMain org.opalj.br.ProjectSerializer -cp DEVELOPING_OPAL/validate/target/scala-2.12/test-classes -o DEVELOPING_OPAL/validate/target"
 *     java -cp DEVELOPING_OPAL/validate/target/OPAL-export org.opalj.br.fixtures.InvokeDynamics
 *
 * @author Andreas Muttscheller
 */
object ProjectSerializer {

    private def showUsage(): Unit = {
        println("OPAL - Project Serializer")
        println("Parameters:")
        println("   -cp <Folder/jar-file> the classes to load into OPAL and export")
        println("   -o <FileName> the folder. Defaults to current folder.")
        println()
        println("java org.opalj.br.ProjectSerializer -cp <classpath or jar file> -o <output folder>")
    }

    def main(args: Array[String]): Unit = {
        var classPath: String = null
        var outFolder: String = "."

        var i = 0
        while (i < args.length) {
            args(i) match {
                case "-cp" ⇒
                    i += 1; classPath = args(i)

                case "-o" ⇒
                    i += 1; outFolder = args(i)

                case "-h" | "--help" ⇒
                    showUsage()
                    System.exit(0)

                case arg ⇒
                    Console.err.println(s"Unknown parameter $arg.")
                    showUsage()
                    System.exit(2)
            }
            i += 1
        }
        if (classPath == null) {
            Console.err.println("Missing classpath information.")
            showUsage()
            System.exit(1)
        }

        val classPathFile = new File(classPath)
        if (!classPathFile.exists()) {
            Console.err.println("Classpath or jar file does not exists.")
            System.exit(1)
        }

        val outFile = new File(s"$outFolder/OPAL-export")
        if (!outFile.exists() && !outFile.mkdir()) {
            Console.out.println("Output folder could not be created!")
            System.exit(1)
        }

        val projectClassFiles = Project.JavaClassFileReader().ClassFiles(classPathFile)
        val libraryClassFiles = Project.JavaClassFileReader().ClassFiles(RTJar)

        val p = Project(
            projectClassFiles,
            libraryClassFiles,
            libraryClassFilesAreInterfacesOnly = false
        )

        serialize(p, outFile)

        Console.out.println(s"Wrote all classfiles to $outFolder")
    }

    def serialize(p: SomeProject, targetFolder: File): Unit = {
        p.allProjectClassFiles.par.foreach { c ⇒
            val b = classToByte(c)
            val targetPackageFolder = new File(s"${targetFolder.getAbsolutePath}/${c.thisType.packageName}")
            targetPackageFolder.mkdirs()
            val targetFile = new File(s"${targetFolder.getAbsolutePath}/${c.fqn}.class")

            process(new BufferedOutputStream(new FileOutputStream(targetFile))) {
                bos ⇒ bos.write(b)
            }
        }
    }

    def classToByte(c: ClassFile): Array[Byte] = {
        Assembler(ba.toDA(c))
    }
}

/**
 * A simple `ClassLoader` that looks-up the available classes from the given Project.
 *
 * @author Andreas Muttscheller
 */
class ProjectBasedInMemoryClassLoader(
        val project: SomeProject,
        parent:      ClassLoader = getClass.getClassLoader
) extends ClassLoader(parent) {

    @throws[ClassNotFoundException]
    override def findClass(name: String): Class[_] = {
        project.allProjectClassFiles.find(_.thisType.toJava == name) match {
            case Some(data) ⇒
                val bytes = ProjectSerializer.classToByte(data)
                defineClass(name, bytes, 0, bytes.length)
            case None ⇒ throw new ClassNotFoundException(name)
        }
    }
}

