/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cli

import scala.language.postfixOps
import scala.reflect.internal.util.NoPosition.showError

import java.io.File
import java.net.URL
import java.util.Calendar
import scala.util.control.ControlThrowable

import com.typesafe.config.Config

import org.rogach.scallop.ScallopConf

import org.opalj.br.ClassFile
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.reader
import org.opalj.br.reader.Source
import org.opalj.bytecode.JDKArg
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.cli.Arg
import org.opalj.cli.ClassPathArg
import org.opalj.cli.ClosedWorldArg
import org.opalj.cli.ExecutionsArg
import org.opalj.cli.LibrariesAsInterfacesArg
import org.opalj.cli.LibraryArg
import org.opalj.cli.LibraryClassPathArg
import org.opalj.cli.LibraryDirectoryArg
import org.opalj.cli.MultiProjectsArg
import org.opalj.cli.NoDynamicConstantRewriteArg
import org.opalj.cli.NoInvokeDynamicRewriteArg
import org.opalj.cli.NoJDKArg
import org.opalj.cli.NoRewriteArg
import org.opalj.cli.OPALCommandLineConfig
import org.opalj.cli.ProjectDirectoryArg
import org.opalj.cli.RenderConfigArg
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

trait ProjectBasedArg[T, R] extends Arg[T, R] {

    final def apply(project: SomeProject, cliConfig: OPALCommandLineConfig): Unit = {
        this(project, cliConfig.get(this))
    }

    def apply(project: SomeProject, value: Option[R]): Unit = {}
}

trait ProjectBasedCommandLineConfig extends OPALCommandLineConfig {
    self: ScallopConf =>

    generalArgs(
        ClassPathArg ^ JDKArg !,
        LibraryArg !,
        LibraryClassPathArg,
        ProjectDirectoryArg,
        LibraryDirectoryArg,
        NoJDKArg !,
        LibrariesAsInterfacesArg,
        ClosedWorldArg,
        ExecutionsArg !,
        NoInvokeDynamicRewriteArg ^ NoDynamicConstantRewriteArg ^ NoRewriteArg
    )

    def setupProject(
        cp:    Iterable[File] = apply(JDKArg).getOrElse(apply(ClassPathArg)),
        libCP: Iterable[File] = get(LibraryClassPathArg, Iterable.empty)
    )(implicit initialLogContext: LogContext = GlobalLogContext): (Project[URL], Seconds) = {
        setupProject(
            cp,
            libCP,
            get(LibraryArg, false) || (cp.head eq JRELibraryFolder),
            get(LibrariesAsInterfacesArg, false)
        )
    }

    def setupProject(
        cp:                    Iterable[File],
        libCP:                 Iterable[File],
        isLibrary:             Boolean,
        librariesAsInterfaces: Boolean
    )(implicit initialLogContext: LogContext): (Project[URL], Seconds) = {
        var projectTime: Seconds = Seconds.None
        var project: Project[URL] = null
        time {
            implicit val config: Config = setupConfig(isLibrary)

            info("creating project", "reading project class files")
            implicit val JavaClassFileReader: (File, (Source, Throwable) => Unit) => Iterable[(ClassFile, URL)] =
                Project.JavaClassFileReader(initialLogContext, config).ClassFiles

            info("project configuration", s"the classpath is ${cp.mkString}")

            val cpFiles = resolveDirToCP(get(ProjectDirectoryArg), cp, cp)
            if (cpFiles.isEmpty) {
                showError("Nothing to analyze.")
                printHelp()
                sys.exit(1)
            }
            val (classFiles, exceptions1) = readClassFiles("project", cpFiles)

            val libcpFiles = resolveDirToCP(get(LibraryDirectoryArg), if (libCP.isEmpty) cp else libCP, libCP)
            val (libraryClassFiles, exceptions2) = readClassFiles("library", libcpFiles, !librariesAsInterfaces)

            val jdkFiles = if (this(NoJDKArg)) Iterable.empty else Iterable(JRELibraryFolder)
            val (jdkClassFiles, exceptions3) = readClassFiles("JDK", jdkFiles, !librariesAsInterfaces)

            try {
                project = Project(
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

            argsIterator.foreach {
                case arg: ProjectBasedArg[_, _] => arg(project, this)
                case _                          =>
            }

            if (get(RenderConfigArg, false)) {
                val effectiveConfiguration =
                    "Effective configuration:\n" + org.opalj.util.renderConfig(project.config)
                info("project configuration", effectiveConfiguration)
            }
        } { t =>
            OPALLogger.info("analysis progress", s"setting up project took ${t.toSeconds} ")(project.logContext)
            projectTime = t.toSeconds
        }
        (project, projectTime)
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

abstract class MultiProjectAnalysisConfig(args: Array[String]) extends ScallopConf(args)
    with ProjectBasedCommandLineConfig { self =>

    generalArgs(MultiProjectsArg)

    /**
     * Executes a function for every project directory of a muti-project analysis
     */
    def foreachProject(f: (Iterable[File], self.type, Int) => Unit, execution: Int): Unit = {
        if (apply(MultiProjectsArg)) {
            for {
                cpEntry <- apply(ClassPathArg)
                subProject <- cpEntry.listFiles()
                if subProject.isDirectory
            } {
                println(s"${subProject.getName}: ${Calendar.getInstance().getTime}")
                f(Iterable(subProject), this, execution)
            }
        } else {
            val cp = apply(JDKArg).getOrElse(apply(ClassPathArg))
            f(cp, this, execution)
        }
    }

}
