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
 import sbt._
 import sbt.Keys.TaskStreams
 import java.io.File
 import java.io.Writer
 import java.io.PrintWriter
 import java.nio.file.SimpleFileVisitor
 import java.nio.file.Path
 import java.nio.file.Files
 import java.nio.file.FileVisitResult
 import java.nio.file.attribute.FileTime
 import java.nio.file.attribute.BasicFileAttributes
 import scala.io.Source.fromFile

 import FixtureCompilationSpec._

/**
 * This class discovers and compiles test fixtures in the OPAL project.
 * See: Test fixtures as seen in the OPAL/bi project (OPAL/bi/src/test/fixtures-java/Readme.md)
 *
 * @author Michael Eichberg
 * @author Simon Leischnig
 */
object FixtureCompilation {

  /** Returns the produced JAR files, after discovering, compiling and packaging
   *  the OPAL test fixtures. This is the method sbt calls as task implementation.
   */
    def compilationTaskRunner(
      streams:         TaskStreams,
      resourceManaged: File,
      sourceDir:       File
    ): Seq[File] = {

      val s: TaskStreams = streams
      val log = s.log
      val std = new PrintWriter(new LogWriter((s: String) ⇒ log.info(s)))
      val err = new PrintWriter(new LogWriter((s: String) ⇒ log.error(s)))

      val discovery = new OPALTestFixtureDiscovery(resourceManaged, sourceDir)
      val compiler: TestFixtureCompiler = new OPALTestFixtureCompiler()

      val createdJARs = for(
        fixtureTask <- discovery.discoverFixtureTasks()
        if compiler.isCompilationNecessary(fixtureTask)
      ) yield {
        compiler.compile(fixtureTask, std, err, log).outputJar
      }

      val createdFiles = createdJARs.toSeq
      if (createdFiles.nonEmpty)
          log.info(createdFiles.mkString("Created archives:\n\t", "\n\t", "\n"))
      else
          log.info("The test fixtures were already compiled.")

      createdFiles
    }


    /** Returns compilation tasks for test fixtures that were discovered in the
      * OPAL project.
      */
    class OPALTestFixtureDiscovery(
      resourceManaged: File,
      sourceDir:       File
    ) {

      val resourceManagedFolder = resourceManaged
      val projectsFolder = sourceDir / "fixtures-java" / "projects"
      val supportFolder = sourceDir / "fixtures-java" / "support"

      /** Finds and returns all test fixtures in the project. */
      def discoverFixtureTasks(): Seq[TestFixtureCompilationTask] = {
        for {
            sourceFolder ← projectsFolder.listFiles
            if sourceFolder.isDirectory
            configFile = sourceFolder.getAbsoluteFile / "compiler.config"
            (supportLibraries, defaultConfigurationOptions) = parseConfigFile(configFile)
            configurationOptions ← defaultConfigurationOptions
        } yield {

            val fixture = TestFixture(sourceFolder)
            val targetFolder = obtainTargetFolder(configFile, sourceFolder, configurationOptions)
            val targetJAR = new File(targetFolder+".jar")

            TestFixtureCompilationTask(
              fixture,
              targetFolder,
              targetJAR,
              configurationOptions,
              supportLibraries
            )
        }
      }

    /** Returns 'require' specifications, and configuration options for a test
      * fixture as can be (optionally) specified in a "compiler.config" file in the fixture.
      *
      * This involves checking if a config file exists (default values if not),
      * filtering comments out, and partitioning by the 'requires' keyword.
      *
      * @param configFile configuration  file of the test fixture (may or
      * may not exist)
      * @return: Returns a pair of 'requires' specs and config options for the compiler
      */
      def parseConfigFile(configFile: File) = {
        if (configFile.exists) {
            val (requires, configurationOptions) = fromFile(configFile).getLines.
                map(_.trim).
                filter(l ⇒ l.nonEmpty && !l.startsWith("#")).toList.
                partition(_.startsWith("requires"))

            ( // return value: pair of mapped requires specs and config options (rest)
                requires.
                map(librarySpec ⇒ librarySpec.substring(librarySpec.indexOf('=') + 1)). /* support library name */
                map(libraryName ⇒ supportFolder / libraryName). /* support library folder */
                mkString(" "),

                configurationOptions
            )
        } else {
            ("", Seq("-g -8 -parameters -genericsignature"))
        }
      }

     /** Returns the name for the target folder from the configuration options. */
      def obtainTargetFolder(
            configFile: File, // compiler.config file object
            sourceFolder: File, // source folder object
            configurationOptions: String // configuration options
          ) = {

        val selectedOptionsIdentification =
            if (configFile.exists)
                configurationOptions.replace(" ", "").replace(':', '=')
            else
                ""

            resourceManagedFolder.getAbsoluteFile / (
                sourceFolder.getName + selectedOptionsIdentification
            )
      }

    }

    class LogWriter(println: String ⇒ Unit) extends Writer {
        override def flush(): Unit = {}
        override def close(): Unit = ??? // not expected to be called
        override def write(chars: Array[Char], offset: Int, length: Int): Unit = {
            println(new String(chars, offset, length))
        }
    }

}




// Doc of the eclipse compiler:

// Eclipse Compiler for Java(TM) v20160829-0950, 3.12.1
// Copyright IBM Corp 2000, 2015. All rights reserved.
//
//    Usage: <options> <source files | directories>
//    If directories are specified, then their source contents are compiled.
//    Possible options are listed below. Options enabled by default are prefixed
//    with '+'.
//
//    Classpath options:
//         -cp -classpath <directories and ZIP archives separated by :>
//                                            specify location for application classes and sources.
//                                            Each directory or file can specify access rules for
//                                            types between '[' and ']' (e.g. [-X] to forbid
//                                            access to type X, [~X] to discourage access to type X,
//                                            [+p/X:-p/*] to forbid access to all types in package p
//                                            but allow access to p/X)
//         -bootclasspath <directories and ZIP archives separated by :>
//                                            specify location for system classes. Each directory or
//                                            file can specify access rules for types between '['
//                                            and ']'
//         -sourcepath <directories and ZIP archives separated by :>
//                                            specify location for application sources. Each directory
//                                            or file can specify access rules for types between '['
//                                            and ']'. Each directory can further specify a specific
//                                            destination directory using a '-d' option between '['
//                                            and ']'; this overrides the general '-d' option.
//                                            .class files created from source files contained in a
//                                            jar file are put in the user.dir folder in case no
//                                            general '-d' option is specified. ZIP archives cannot
//                                            override the general '-d' option
//         -extdirs <directories separated by :>
//                                            specify location for extension ZIP archives
//         -endorseddirs <directories separated by :>
//                                            specify location for endorsed ZIP archives
//         -d <dir>                           destination directory (if omitted, no directory is
//                                            created); this option can be overridden per source
//                                            directory
//         -d none                            generate no .class files
//         -encoding <enc>                    specify default encoding for all source files. Each
//                                            file/directory can override it when suffixed with
//                                            '['<enc>']' (e.g. X.java[utf8]).
//                                            If multiple default encodings are specified, the last
//                                            one will be used.
//
//    Compliance options:
//         -1.3                               use 1.3 compliance (-source 1.3 -target 1.1)
//         -1.4                               + use 1.4 compliance (-source 1.3 -target 1.2)
//         -1.5 -5 -5.0                       use 1.5 compliance (-source 1.5 -target 1.5)
//         -1.6 -6 -6.0                       use 1.6 compliance (-source 1.6 -target 1.6)
//         -1.7 -7 -7.0                       use 1.7 compliance (-source 1.7 -target 1.7)
//         -1.8 -8 -8.0                       use 1.8 compliance (-source 1.8 -target 1.8)
//         -source <version>                  set source level: 1.3 to 1.8 (or 5, 5.0, etc)
//         -target <version>                  set classfile target: 1.1 to 1.8 (or 5, 5.0, etc)
//                                            cldc1.1 can also be used to generate the StackMap
//                                            attribute
//
//    Warning options:
//         -deprecation                       + deprecation outside deprecated code (equivalent to
//                                            -warn:+deprecation)
//         -nowarn -warn:none disable all warnings
//         -nowarn:[<directories separated by :>]
//                                            specify directories from which optional problems should
//                                            be ignored
//         -?:warn -help:warn display advanced warning options
//
//    Error options:
//         -err:<warnings separated by ,> convert exactly the listed warnings to be reported as errors
//         -err:+<warnings separated by ,> enable additional warnings to be reported as errors
//         -err:-<warnings separated by ,> disable specific warnings to be reported as errors
//
//    Setting warning or error options using properties file:
//         -properties <file>                 set warnings/errors option based on the properties
//                                            file contents. This option can be used with -nowarn,
//                                            -err:.. or -warn:.. options, but the last one on the
//                                            command line sets the options to be used.
//
//    Debug options:
//         -g[:lines,vars,source]             custom debug info
//         -g:lines,source                    + both lines table and source debug info
//         -g                                 all debug info
//         -g:none                            no debug info
//         -preserveAllLocals preserve unused local vars for debug purpose
//
//    Annotation processing options:
//        These options are meaningful only in a 1.6 environment.
//         -Akey[=value]                      options that are passed to annotation processors
//         -processorpath <directories and ZIP archives separated by :>
//                                            specify locations where to find annotation processors.
//                                            If this option is not used, the classpath will be
//                                            searched for processors
//         -processor <class1[,class2,...]>
//                                            qualified names of the annotation processors to run.
//                                            This bypasses the default annotation discovery process
//         -proc:only                         run annotation processors, but do not compile
//         -proc:none                         perform compilation but do not run annotation
//                                            processors
//         -s <dir>                           destination directory for generated source files
//         -XprintProcessorInfo print information about which annotations and elements
//                                            a processor is asked to process
//         -XprintRounds                      print information about annotation processing rounds
//         -classNames <className1[,className2,...]>
//                                            qualified names of binary classes to process
//
//    Advanced options:
//         @<file>                            read command line arguments from file
//         -maxProblems <n>                   max number of problems per compilation unit (100 by
//                                            default)
//         -log <file>                        log to a file. If the file extension is '.xml', then
//                                            the log will be a xml file.
//         -proceedOnError[:Fatal]
//                                            do not stop at first error, dumping class files with
//                                            problem methods
//                                            With ":Fatal", all optional errors are treated as fatal
//         -verbose                           enable verbose output
//         -referenceInfo                     compute reference info
//         -progress                          show progress (only in -log mode)
//         -time                              display speed information
//         -noExit                            do not call System.exit(n) at end of compilation (n==0
//                                            if no error)
//         -repeat <n>                        repeat compilation process <n> times for perf analysis
//         -inlineJSR                         inline JSR bytecode (implicit if target >= 1.5)
//         -enableJavadoc                     consider references in javadoc
//         -parameters                        generate method parameters attribute (for target >= 1.8)
//         -genericsignature                  generate generic signature for lambda expressions
//         -Xemacs                            used to enable emacs-style output in the console.
//                                            It does not affect the xml log output
//         -missingNullDefault    report missing default nullness annotation
//         -annotationpath <directories and ZIP archives separated by :>
//                                            specify locations where to find external annotations
//                                            to support annotation-based null analysis.
//                                            The special name CLASSPATH will cause lookup of
//                                            external annotations from the classpath and sourcepath.
//
//         -? -help                           print this help message
//         -v -version                        print compiler version
//         -showversion                       print compiler version and continue
