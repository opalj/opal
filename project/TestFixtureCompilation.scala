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

import TestFixtureCompilationSpecification.TestFixtureCompiler
import TestFixtureCompilationSpecification.EclipseTestFixtureCompiler
import TestFixtureCompilationSpecification.TestFixtureCompilationTask
import TestFixtureCompilationSpecification.TestFixture

import scala.io.Source.fromFile

/**
 * This class discovers and compiles test fixtures in the OPAL project.
 *
 * Example test fixtures are found in the OPAL/bi project. Go to
 * "OPAL/bi/src/test/fixtures-java/Readme.md" for further information about the configuration of
 * the compiler.
 *
 * @author Michael Eichberg
 * @author Simon Leischnig
 */
object TestFixtureCompilation {

    /**
     * Returns the produced JAR files, after discovering, compiling and packaging
     * the OPAL test fixtures. This is the method sbt calls when the respective task
     * is invoked.
     */
    def run(
        streams:         TaskStreams,
        resourceManaged: File,
        sourceDir:       File
    ): Seq[File] = {

        val s: TaskStreams = streams
        val log = s.log
        val std = new PrintWriter(new LogWriter((s: String) ⇒ log.info(s)))
        val err = new PrintWriter(new LogWriter((s: String) ⇒ log.error(s)))

        val discovery = new TestFixturesDiscovery(resourceManaged, sourceDir)
        val compiler: TestFixtureCompiler = new EclipseTestFixtureCompiler()

        val createdJARs = for {
            fixtureTask ← discovery.discoverFixtureTasks()
            if compiler.isCompilationNecessary(fixtureTask)
        } yield {
            compiler.compile(fixtureTask, std, err, log).outputJar
        }

        if (createdJARs.nonEmpty)
            log.info(createdJARs.mkString("Created archives:\n\t", "\n\t", "\n"))
        else
            log.info("The test fixtures were already compiled.")

        createdJARs
    }

    /**
     * Returns compilation tasks for test fixtures that were discovered in the
     * OPAL project.
     */
    class TestFixturesDiscovery(resourceManaged: File, sourceDir: File) {

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

        /**
         * Returns the required libraries, and configuration options for a test
         * fixture. Both can optionally be specified in a "compiler.config" file in the fixture.
         *
         * This involves checking if a config file exists,
         * filtering comments out, and partitioning by the 'requires' keyword.
         *
         * @param configFile The text fixtures optional compiler/environment configuration file.
         * @return Returns a pair of 'requires' specs and config options for the compiler.
         */
        def parseConfigFile(configFile: File): (String, Seq[String]) = {
            if (configFile.exists) {
                val (requireDefs, configurationOptions) =
                    fromFile(configFile).getLines.map(_.trim).
                        filter(l ⇒ l.nonEmpty && !l.startsWith("#")).toList.
                        partition(_.startsWith("requires"))

                val requiredLibs =
                    requireDefs.
                        // support lib name:
                        map(libSpec ⇒ libSpec.substring(libSpec.indexOf('=') + 1)).
                        // support lib folder:
                        map(libName ⇒ supportFolder / libName).
                        mkString(" ")

                (requiredLibs, configurationOptions)
            } else {
                ("", Seq("-g -8 -parameters -genericsignature"))
            }
        }

        /** Returns the name for the target folder from the configuration options. */
        def obtainTargetFolder(
            configFile:           File, // compiler.config file object
            sourceFolder:         File, // source folder object
            configurationOptions: String // configuration options
        ): File = {

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


