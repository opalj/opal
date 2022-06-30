/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
        error.foreach { e => println(); Console.err.println(e); println() }
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
                case "-in"  => { i += 1; in = args(i) }
                case "-out" => { i += 1; out = args(i) }

                case "-h" | "--help" =>
                    showUsage(error = None);
                    System.exit(0)
                case arg =>
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
        p.parForeachProjectClassFile() { cf =>
            val classFileFolderName = s"${targetFolder.getAbsolutePath}/${cf.thisType.packageName}"
            val classFileFolder = new File(classFileFolderName)
            classFileFolder.mkdirs()
            val classFileName = s"${cf.fqn}.class"
            val targetFile = new File(s"${targetFolder.getAbsolutePath}/$classFileName")

            val b = Assembler(ba.toDA(cf))
            process(new BufferedOutputStream(new FileOutputStream(targetFile))) {
                bos => bos.write(b)
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
