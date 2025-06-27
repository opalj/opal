/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.language.postfixOps

import java.io.File

import com.typesafe.config.Config

import org.opalj.br.analyses.Project.JavaClassFileReader
import org.opalj.bytecode.JDKCommand
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.cli.ClassPathCommand
import org.opalj.cli.Command
import org.opalj.cli.ConvertedCommand
import org.opalj.cli.ForwardingCommand
import org.opalj.cli.LibraryCommand
import org.opalj.cli.LibraryDirectoryCommand
import org.opalj.cli.NoJDKCommand
import org.opalj.cli.OPALCommandLineConfig
import org.opalj.cli.ProjectDirectoryCommand
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.log.LogContext

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

trait ProjectBasedCommandLineConfig extends OPALCommandLineConfig { self: ScallopConf =>

    commands(
        ClassPathCommand ^ JDKCommand !,
        LibraryCommand !,
        ProjectDirectoryCommand,
        LibraryDirectoryCommand,
        NoJDKCommand !,
        PropertyStoreThreadsNumCommand
    )

    def setupProject(): SomeProject = {
        setupProject(get(JDKCommand).getOrElse(this(ClassPathCommand).head), this(LibraryCommand))
    }

    def setupProject(cp: File, isLibrary: Boolean): SomeProject = {
        val classFiles = get(ProjectDirectoryCommand) match {
            case Some(dir) => JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      => JavaClassFileReader().ClassFiles(cp)
        }

        val libFiles = get(LibraryDirectoryCommand) match {
            case Some(dir) => JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      => Iterable.empty
        }

        val JDKFiles = if (this(NoJDKCommand)) Iterable.empty
        else JavaClassFileReader().ClassFiles(JRELibraryFolder)

        implicit val config: Config = setupConfig(isLibrary)

        val project = Project(
            classFiles,
            libFiles ++ JDKFiles,
            libraryClassFilesAreInterfacesOnly = false,
            Iterable.empty
        )

        commandsIterator.foreach {
            case command: ProjectBasedCommand[_, _] => command(project, this)
            case _                                  =>
        }

        project
    }
}
