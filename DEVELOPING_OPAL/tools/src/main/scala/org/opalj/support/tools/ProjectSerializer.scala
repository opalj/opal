/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package tools

import scala.language.postfixOps

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import scala.Console.out

import org.opalj.bc.Assembler
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.OutputDirArg
import org.opalj.io.process

/**
 * Exports the class files belonging to the project part of a loaded
 * [[org.opalj.br.analyses.Project]].
 * I.e., all code rewritings (e.g., INVOKEDYNAMIC resolution) and
 * bytecode optimizations become visible.
 *
 * Example execution:
 *    java org.opalj.br.ProjectSerializer --cp <MyJar> --outputDir <MyJarRewritten>
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object ProjectSerializer extends ProjectsAnalysisApplication {

    protected class SerializerConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Writes out the project's rewritten and transformed class files"

        args(
            OutputDirArg !
        )
    }

    protected type ConfigType = SerializerConfig

    protected def createConfig(args: Array[String]): SerializerConfig = new SerializerConfig(args)

    override protected def evaluate(
        cp:             Iterable[File],
        analysisConfig: SerializerConfig,
        execution:      Int
    ): Unit = {

        def checkOrCreateOutputFolder(outFolder: File): Unit = {
            if ((!outFolder.exists() && !outFolder.mkdirs()) ||
                (outFolder.exists() && !outFolder.isDirectory)
            ) {
                Console.err.println(s"$out could not be created or is not a folder.")
                System.exit(1)
            }
        }
        val outFolder = analysisConfig(OutputDirArg)
        checkOrCreateOutputFolder(outFolder)
        val classesFolder = new File(outFolder.getPath + File.separator + "classes")
        checkOrCreateOutputFolder(classesFolder)

        val (p, _) = analysisConfig.setupProject(cp)
        serialize(p, classesFolder)
        println(s"Wrote all classfiles to $outFolder.")
    }

    def serialize(p: SomeProject, targetFolder: File): Unit = {
        val zipFile = new File(targetFolder.getAbsolutePath + File.separator + "project.zip")
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
