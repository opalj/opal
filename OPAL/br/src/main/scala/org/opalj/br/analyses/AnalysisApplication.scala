/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.net.URL
import java.io.File
import scala.util.control.ControlThrowable
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import org.opalj.br.reader.Java9LibraryFramework
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.info
import org.opalj.log.OPALLogger.error
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.LogMessage

import scala.collection.immutable.ArraySeq

/**
 * Provides the necessary infrastructure to easily execute a given analysis that
 * generates some analysis result that can be printed on the command line.
 *
 * To facilitate the usage of this trait several implicit conversions are defined that
 * wrap standard analyses ([[org.opalj.br.analyses]]) such that they report
 * results that are reportable.
 *
 * This class distinguishes between class files belonging to the code base under
 * analysis and those that belong to the libraries. Those belonging to the libraries
 * are loaded using the `ClassFileReader` for library classes (basically, all method
 * bodies are skipped [[org.opalj.br.reader.Java8LibraryFramework]]).
 * The parameter to specify library classes is `-libcp=`, the parameter to specify
 * the "normal" classpath is `-cp=`.
 *
 * ==Control Flow==
 *  1. The standard parameters are checked.
 *  1. The analysis is called to let it verify the analysis specific parameters.
 *  1. The [[Project]] is created.
 *  1. The `analyze` method of the [[Analysis]] is called with the project and the parameters.
 *  1. The results are printed.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
trait AnalysisApplication {

    /**
     * The analysis that will be executed.
     *
     * The `analyze` method implemented by the analysis will be called after loading
     * all class files and creating a `Project`. Additionally,
     * all specified (additional) parameters are passed to the analyze method.
     */
    val analysis: Analysis[URL, ReportableAnalysisResult]

    /**
     * Describes the analysis specific parameters. An analysis specific parameter
     * has to start with a dash ("-") and has to contain an equals sign ("=").
     *
     * @note The parameter `-cp=` is already predefined (see general documentation).
     * @note The parameter `-library=` is already predefined (see general documentation).
     */
    def analysisSpecificParametersDescription: String = ""

    /**
     * Checks if the (additional) parameters are understood by the analysis.
     * If an error is found, a list of issues is returned and the analysis will not be executed.
     *
     * This method '''must be''' overridden if the analysis defines additional
     * parameters. A method that overrides this method should `return` the list of
     * issues if it can't validate all arguments.
     * The default behavior is to check that there are no additional parameters.
     */
    def checkAnalysisSpecificParameters(parameters: Seq[String]): Iterable[String] = {
        if (parameters.isEmpty) Nil else parameters.map("unknown parameter: "+_)
    }

    /**
     * Prints out general information how to use this analysis. Printed whenever
     * the set of specified parameters is not valid.
     */
    protected def printUsage(implicit logContext: LogContext): Unit = {
        info(
            "usage",
            "java "+
                this.getClass.getName+"\n"+
                "[-help (prints this help and exits)]\n"+
                "[-renderConfig (prints the configuration)]\n"+
                "[-cp=<Directories or JAR/class files> (Default: the current folder.)]\n"+
                "[-libcp=<Directories or JAR/class files>]\n"+
                "[-projectConfig=<project type specific configuration options)>]\n"+
                "[-completelyLoadLibraries (the bodies of library methods are loaded)]\n"+
                analysisSpecificParametersDescription
        )
        info("general", "description: "+analysis.description)
        info("general", "copyright: "+analysis.copyright)
    }

    def main(args: Array[String]): Unit = {

        implicit val logContext: LogContext = GlobalLogContext

        def showError(message: String): Unit = error("project configuration", message)

        //
        // Categories of args
        //
        var unknownArgs = List.empty[String]
        var cp = IndexedSeq.empty[String]
        var libcp = IndexedSeq.empty[String]
        var projectConfig: Option[String] = None
        var completelyLoadLibraries = false
        var renderConfig = false

        //
        // 1. Process args
        //
        def splitCPath(path: String) = path.substring(path.indexOf('=') + 1).split(File.pathSeparator)
        def splitLibCPath(path: String) = path.substring(path.indexOf('=') + 1).split(File.pathSeparator)
        args.foreach { arg =>
            if (arg == "-help") {
                printUsage
                sys.exit(0)
            } else if (arg.startsWith("-cp=")) {
                cp ++= splitCPath(arg)
            } else if (arg.startsWith("-libcp=")) {
                libcp ++= splitLibCPath(arg)
            } else if (arg == "-completelyLoadLibraries") {
                completelyLoadLibraries = true
            } else if (arg.startsWith("-projectConfig=")) {
                projectConfig = Some(arg.substring(arg.indexOf('=') + 1))
            } else if (arg == "-renderConfig") {
                renderConfig = true
            } else {
                unknownArgs ::= arg
            }

        }

        //
        // 2. Check parsed args
        //
        // Input files must be either directories, or class/jar files.
        //
        def verifyFile(filename: String): Option[File] = {
            val file = new File(filename)

            def workingDirectory: String = {
                s"(working directory: ${System.getProperty("user.dir")})"
            }

            if (!file.exists) {
                showError(s"File does not exist: $file $workingDirectory.")
                None
            } else if (!file.canRead) {
                showError(s"Cannot read: $file $workingDirectory.")
                None
            } else if (!file.isDirectory &&
                !filename.endsWith(".jar") &&
                !filename.endsWith(".ear") &&
                !filename.endsWith(".war") &&
                !filename.endsWith(".zip") &&
                !filename.endsWith(".jmod") &&
                !filename.endsWith(".class")) {
                showError(s"Input file is neither a directory nor a class or JAR/JMod file: $file.")
                None
            } else
                Some(file)
        }

        def verifyFiles(filenames: IndexedSeq[String]): Seq[File] = filenames.flatMap(verifyFile)

        if (cp.isEmpty) cp = ArraySeq.unsafeWrapArray(Array(System.getProperty("user.dir")))
        info("project configuration", s"the classpath is ${cp.mkString}")
        val cpFiles = verifyFiles(cp)
        if (cpFiles.isEmpty) {
            showError("Nothing to analyze.")
            printUsage
            sys.exit(1)
        }

        val libcpFiles = verifyFiles(libcp)

        if (unknownArgs.nonEmpty)
            info("project configuration", "analysis specific parameters: "+unknownArgs.mkString(", "))
        val issues = checkAnalysisSpecificParameters(unknownArgs)
        if (issues.nonEmpty) {
            issues.foreach { i => error("project configuration", i) }
            printUsage
            sys.exit(2)
        }

        //
        // 3. Setup project context
        //
        val project: Project[URL] = try {
            val config =
                if (projectConfig.isEmpty)
                    ConfigFactory.load()
                else
                    ConfigFactory.load(projectConfig.get)
            setupProject(cpFiles, libcpFiles, completelyLoadLibraries, config)
        } catch {
            case ct: ControlThrowable => throw ct;
            case t: Throwable =>
                error("fatal", "setting up the project failed", t)
                printUsage
                sys.exit(2)
        }

        //
        // 4. execute analysis
        //

        if (renderConfig) {
            val effectiveConfiguration =
                "Effective configuration:\n"+org.opalj.util.renderConfig(project.config)
            info("project configuration", effectiveConfiguration)
        }

        info("info", "executing analysis: "+analysis.title+".")
        // TODO Add progressmanagement.
        val result = analysis.analyze(project, unknownArgs.toSeq, ProgressManagement.None)
        OPALLogger.log(LogMessage.plainInfo(result.toConsoleString))
    }

    protected def handleParsingExceptions(
        project:    SomeProject,
        exceptions: Iterable[Throwable]
    ): Unit = {
        if (exceptions.isEmpty)
            return ;

        implicit val logContext: LogContext = project.logContext
        for (exception <- exceptions) {
            error("creating project", "ignoring invalid class file", exception)
        }
    }

    def setupProject(
        cpFiles:                 Iterable[File],
        libcpFiles:              Iterable[File],
        completelyLoadLibraries: Boolean,
        configuredConfig:        Config
    )(
        implicit
        initialLogContext: LogContext
    ): Project[URL] = {
        info("creating project", "reading project class files")
        val JavaClassFileReader = Project.JavaClassFileReader(initialLogContext, configuredConfig)

        val (classFiles, exceptions1) =
            reader.readClassFiles(
                cpFiles,
                JavaClassFileReader.ClassFiles,
                file => info("creating project", "\tfile: "+file)
            )

        val (libraryClassFiles, exceptions2) = {
            if (libcpFiles.nonEmpty) {
                info("creating project", "reading library class files")
                reader.readClassFiles(
                    libcpFiles,
                    if (completelyLoadLibraries) {
                        JavaClassFileReader.ClassFiles
                    } else {
                        Java9LibraryFramework.ClassFiles
                    },
                    file => info("creating project", "\tfile: "+file)
                )
            } else {
                (Iterable.empty[(ClassFile, URL)], List.empty[Throwable])
            }
        }
        val project =
            Project(
                classFiles,
                libraryClassFiles,
                libraryClassFilesAreInterfacesOnly = !completelyLoadLibraries,
                Iterable.empty
            )(config = configuredConfig)
        handleParsingExceptions(project, exceptions1 ++ exceptions2)

        val statistics =
            project
                .statistics.map(kv => "- "+kv._1+": "+kv._2)
                .toList.sorted.reverse
                .mkString("project statistics:\n\t", "\n\t", "\n")
        info("project", statistics)(project.logContext)
        project
    }
}
