/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.language.postfixOps
import scala.reflect.internal.util.NoPosition.showError

import java.io.File
import java.net.URL
import java.util.Calendar
import scala.util.control.ControlThrowable

import com.typesafe.config.Config

import org.opalj.br.reader.Source
import org.opalj.bytecode.JDKCommand
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.cli.ClassPathCommand
import org.opalj.cli.Command
import org.opalj.cli.ConvertedCommand
import org.opalj.cli.ForwardingCommand
import org.opalj.cli.LibrariesAsInterfacesCommand
import org.opalj.cli.LibraryClassPathCommand
import org.opalj.cli.LibraryCommand
import org.opalj.cli.LibraryDirectoryCommand
import org.opalj.cli.MultiProjectsCommand
import org.opalj.cli.NoJDKCommand
import org.opalj.cli.OPALCommandLineConfig
import org.opalj.cli.ProjectDirectoryCommand
import org.opalj.cli.RenderConfigCommand
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info

import org.rogach.scallop.ScallopConf
import org.rogach.scallop.intConverter

trait ProjectBasedCommand[T, R] extends Command[T, R] {

    final def apply(project: SomeProject, cliConfig: OPALCommandLineConfig): Unit = {
        this(project, cliConfig.get(this))
    }

    def apply(project: SomeProject, value: Option[R]): Unit = {}
}

object PropertyStoreThreadsNumCommand extends ConvertedCommand[Int, Int] with ForwardingCommand[Int, Int, Int]
    with ProjectBasedCommand[Int, Int] {
    val command = org.opalj.cli.ThreadsNumCommand

    override def apply(project: SomeProject, value: Option[Int]): Unit = {
        val numThreads = value.get
        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) => {
                implicit val lg: LogContext = project.logContext
                if (numThreads == 0) {
                    org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                } else {
                    org.opalj.fpcf.par.PKECPropertyStore.MaxThreads = numThreads
                    org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                }
            }
        )
    }
}

trait ProjectBasedCommandLineConfig extends OPALCommandLineConfig {
    self: ScallopConf =>

    generalCommands(
        ClassPathCommand ^ JDKCommand !,
        LibraryCommand !,
        LibraryClassPathCommand,
        ProjectDirectoryCommand,
        LibraryDirectoryCommand,
        NoJDKCommand !,
        LibrariesAsInterfacesCommand,
        PropertyStoreThreadsNumCommand
    )

    def setupProject(
        cp:    Iterable[File] = apply(JDKCommand).getOrElse(apply(ClassPathCommand)),
        libCP: Iterable[File] = get(LibraryClassPathCommand).getOrElse(Iterable.empty)
    )(
        isLibrary:             Boolean = get(LibraryCommand).getOrElse(cp.head eq JRELibraryFolder),
        librariesAsInterfaces: Boolean = get(LibrariesAsInterfacesCommand).getOrElse(false)
    )(implicit initialLogContext: LogContext = GlobalLogContext): SomeProject = {

        implicit val config: Config = setupConfig(isLibrary)

        info("creating project", "reading project class files")
        implicit val JavaClassFileReader: (File, (Source, Throwable) => Unit) => Iterable[(ClassFile, URL)] =
            Project.JavaClassFileReader(initialLogContext, config).ClassFiles

        info("project configuration", s"the classpath is ${cp.mkString}")

        val cpFiles = resolveDirToCP(get(ProjectDirectoryCommand), cp, cp)
        if (cpFiles.isEmpty) {
            showError("Nothing to analyze.")
            printHelp()
            sys.exit(1)
        }
        val (classFiles, exceptions1) = readClassFiles("project", cpFiles)

        val libcpFiles = resolveDirToCP(get(LibraryDirectoryCommand), if (libCP.isEmpty) cp else libCP, libCP)
        val (libraryClassFiles, exceptions2) = readClassFiles("library", libcpFiles, !librariesAsInterfaces)

        val jdkFiles = if (this(NoJDKCommand)) Iterable.empty else Iterable(JRELibraryFolder)
        val (jdkClassFiles, exceptions3) = readClassFiles("JDK", jdkFiles, !librariesAsInterfaces)

        val project =
            try {
                Project(
                    classFiles,
                    libraryClassFiles ++ jdkClassFiles,
                    libraryClassFilesAreInterfacesOnly = librariesAsInterfaces,
                    Iterable.empty
                )(config = config)
            } catch {
                case ct: ControlThrowable => throw ct;
                case t: Throwable =>
                    error("fatal", "setting up the project failed", t)
                    printHelp()
                    sys.exit(2)
            }
        handleParsingExceptions(project, exceptions1 ++ exceptions2 ++ exceptions3)

        val statistics =
            project
                .statistics.map(kv => "- " + kv._1 + ": " + kv._2)
                .toList.sorted.reverse
                .mkString("project statistics:\n\t", "\n\t", "\n")
        info("project", statistics)(project.logContext)

        commandsIterator.foreach {
            case command: ProjectBasedCommand[_, _] => command(project, this)
            case _                                  =>
        }

        if (get(RenderConfigCommand).getOrElse(false)) {
            val effectiveConfiguration =
                "Effective configuration:\n" + org.opalj.util.renderConfig(project.config)
            info("project configuration", effectiveConfiguration)
        }

        project
    }

    protected def resolveDirToCP(
        dirOption: Option[String],
        root:      Iterable[File],
        cp:        Iterable[File]
    ): Iterable[File] = {
        dirOption match {
            case Some(dir) =>
                root.flatMap { cpEntry =>
                    val projectEntry = cpEntry.toPath.resolve(dir).toFile
                    if (projectEntry.exists()) Some(projectEntry)
                    else Option.empty[File]
                }
            case None => cp
        }
    }

    protected def handleParsingExceptions(
        project:    SomeProject,
        exceptions: Iterable[Throwable]
    ): Unit = {
        if (exceptions.isEmpty)
            return;

        implicit val logContext: LogContext = project.logContext
        for (exception <- exceptions) {
            error("creating project", "ignoring invalid class file", exception)
        }
    }

    protected def readClassFiles(kind: String, classFilePaths: Iterable[File], loadCompletely: Boolean = true)(
        implicit
        classFilesReader: (File, (Source, Throwable) => Unit) => Iterable[(ClassFile, URL)],
        logContext:       LogContext
    ): (Iterable[(ClassFile, URL)], Iterable[Throwable]) = {
        if (classFilePaths.nonEmpty) {
            info("creating project", s"reading $kind class files")
            reader.readClassFiles(
                classFilePaths,
                if (loadCompletely) {
                    classFilesReader
                } else {
                    Project.JavaLibraryClassFileReader.ClassFiles
                },
                file => info("creating project", s"\tfile: $file")
            )
        } else {
            (Iterable.empty[(ClassFile, URL)], List.empty[Throwable])
        }
    }
}

trait MultiProjectAnalysisConfig[T <: ScallopConf] extends ProjectBasedCommandLineConfig { self: T =>

    generalCommands(MultiProjectsCommand)

    /**
     * Executes a function for every project directory of a muti-project analysis
     */
    def foreachProject(f: (Iterable[File], T) => Unit): Unit = {
        if (apply(MultiProjectsCommand)) {
            for {
                cpEntry <- apply(ClassPathCommand)
                subProject <- cpEntry.listFiles()
                if subProject.isDirectory
            } {
                println(s"${subProject.getName}: ${Calendar.getInstance().getTime}")
                f(Iterable(subProject), this)
            }
        } else {
            val cp = apply(JDKCommand).getOrElse(apply(ClassPathCommand))
            f(cp, this)
        }
    }

}
