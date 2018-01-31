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
package org.opalj.javacompilation

import sbt._
import java.io.File
import java.io.PrintWriter

import org.eclipse.jdt.core.compiler.batch.BatchCompiler

/**
 * Defines the classes that define return types of the plugin's tasks and settings.
 *
 * @author Simon Leischnig
 */
object FixtureCompileSpec {

    /**
     * Represents the result of a test fixture compilation.
     * The [[generatedFiles]] method returns a sequence of generated class files.
     */
    case class JavaFixtureCompilationResult(
            task:       JavaFixtureCompilationTask,
            classFiles: Seq[File],
            wasSkipped: Boolean
    ) {
        def generatedFiles: Seq[File] = classFiles
    }

    /**
     * Represents the result of packaing a test fixture.
     * The [[generatedFiles]] method returns a sequence of generated class files and
     * the JAR file.
     */
    case class JavaFixturePackagingResult(
            compilation: JavaFixtureCompilationResult,
            jarFile:     File,
            wasSkipped:  Boolean
    ) {
        def generatedFiles: Seq[File] = compilation.generatedFiles :+ jarFile
    }

    /**
     * Represents a test fixture compilation task.
     *
     * @note    One and the same fixture may be subject to compilation
     *          with different configOptions parameters. For information about the
     *          format of configOptions and supportLibraries strings
     *          see the plugin's README.md document.
     */
    case class JavaFixtureCompilationTask(
            fixture:          TestFixture,
            targetFolder:     File,
            configOptions:    String,
            supportLibraries: String,
            compiler:         TestFixtureCompiler
    )

    /** Represents a test fixture by its source folder. */
    case class TestFixture(sourceFolder: File)

    /** Represents a test fixture compiler abstractly. */
    abstract class TestFixtureCompiler {

        /** Compiles the given task (regardless of isCompilationNecessary(task)) */
        def compile(
            task: JavaFixtureCompilationTask,
            std:  PrintWriter,
            err:  PrintWriter,
            log:  Logger
        ): JavaFixtureCompilationResult

    }

    /** Returns the default compiler implementation for the compilation task. */
    def resolveCompiler(spec: String): TestFixtureCompiler = {
        new OPALTestFixtureCompiler()
    }

    /** This class is the test fixture compiler OPAL uses. */
    class OPALTestFixtureCompiler extends TestFixtureCompiler {

        //TODO: Use org.eclipse.jdt.internal.formatter.DefaultCodeFormatter for formatting code

        /** Compiles a test fixture with the eclipse jdt compiler. */
        def compile(
            task: JavaFixtureCompilationTask,
            std:  PrintWriter,
            err:  PrintWriter,
            log:  Logger
        ): JavaFixtureCompilationResult = {

            val standardConfiguration = s"${task.fixture.sourceFolder} "+
                s"${task.supportLibraries} -d ${task.targetFolder} -Xemacs -encoding utf8 "
            val commandLine = s"$standardConfiguration ${task.configOptions}"

            // Note that we have to recompile the entire project if some file is
            // just deleted to ensure that the project is still consistent.
            // see commit: 818ddcefdbf47e1368d476a8cf0ad32e1a4e4856
            val sourceFiles = (task.fixture.sourceFolder ** ("*.java" || "compiler.config")).get
            val sourceFolderDate = task.fixture.sourceFolder.lastModified()
            val newestSourceFileDate = sourceFiles.map(_.lastModified).foldLeft(sourceFolderDate)(Math.max)
            val classFiles = (task.targetFolder ** "*.class").get
            val targetFolderDate = task.targetFolder.lastModified()
            val newestClassFileDate = classFiles.map(_.lastModified).foldLeft(targetFolderDate)(Math.max)

            val compilationNecessary = newestClassFileDate < newestSourceFileDate

            if (compilationNecessary) {
                // to ensure that we have no lingering class files
                IO.deleteFilesEmptyDirs(Iterable(task.targetFolder))
                IO.createDirectory(task.targetFolder)
                log.info(s"Compiling test fixtures: $commandLine")
                val compilationResult = BatchCompiler.compile(commandLine, std, err, null)
                if (!compilationResult) {
                    throw new IllegalStateException("compiling the Java test fixtures failed")
                }
            }

            val classfileSeq = (task.targetFolder ** "*.class").get.toSeq
            JavaFixtureCompilationResult(task, classfileSeq, !compilationNecessary)
        }
    }

}
