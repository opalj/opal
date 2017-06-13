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

/**
 * contains classes and methods to specify test fixtures
 * as seen in the OPAL/bi project (OPAL/bi/src/test/fixtures-java/Readme.md)
 *
 * @author Simon Leischnig
 */
object FixtureCompilationSpec {

  // result of a test fixture compilation (JAR file)
  case class TestFixtureCompilationResult(outputJar: File)

  // represents a test fixture compilation task
  // note: one and the same fixture may be subject to compilation
  // with different configOptions parameters
  case class TestFixtureCompilationTask(
    fixture: TestFixture,
    targetFolder: File,
    targetJAR: File,
    configOptions: String,
    supportLibraries: String
  )

  // Represents a test fixture
  case class TestFixture(
    sourceFolder: File
  )

  // represents a test fixture compiler abstractly
  abstract class TestFixtureCompiler {

    // do a compilation of the given task (regardless of isCompilationNecessary(task))
    def compile(
      task: TestFixtureCompilationTask,
      std: PrintWriter,
      err: PrintWriter,
      log: Logger): TestFixtureCompilationResult

    // whether this compiler deems it necessary to do a compilation of the task
    // - different compilers may judge this differently
    // - default implementation looks at the source files dates and the date
    //   of the target JAR
    def isCompilationNecessary(task: TestFixtureCompilationTask): Boolean = {
      !task.targetJAR.exists ||
          {
              val targetJARAsPath = Files.getLastModifiedTime(task.targetJAR.toPath)
              val wasUpdatedVisitor = new WasUpdatedFileVisitor(targetJARAsPath)
              Files.walkFileTree(task.fixture.sourceFolder.toPath, wasUpdatedVisitor)
              wasUpdatedVisitor.wasUpdated
          }
    }
  }

  // The test fixture compiler OPAL uses
  class OPALTestFixtureCompiler extends TestFixtureCompiler {
    import org.eclipse.jdt.core.compiler.batch.BatchCompiler

    //compiles a fixture with the eclipse jdt compiler
    //TODO: low; ...that is present in the classpath of this sbt build
    //TODO: legacy; USE org.eclipse.jdt.internal.formatter.DefaultCodeFormatter for formatting code
    def compile(
      task: TestFixtureCompilationTask,
      std: PrintWriter,
      err: PrintWriter,
      log: Logger): TestFixtureCompilationResult = {

      val standardConfiguration = s"${task.fixture.sourceFolder} " +
        s"${task.supportLibraries} -d ${task.targetFolder} -Xemacs -encoding utf8 "
      val commandLine = s"$standardConfiguration ${task.configOptions}"

      IO.createDirectory(task.targetFolder) //TODO: low; failure?
      val compilationResult = BatchCompiler.compile(commandLine, std, err, null);

      log.info(s"Compiling test fixtures: $commandLine")
      if (!compilationResult) {
          throw new IllegalStateException("Compiling the test fixtures failed")
      }

      val targetFolderLength = task.targetFolder.toString.length + 1
      val classFiles: Traversable[(File, String)] =
          (task.targetFolder ** "*.class").get map { classFile ⇒
              ((classFile, classFile.toString.substring(targetFolderLength)))
          }

      log.info(
          classFiles.view.map(_._2).mkString(s"Creating archive ${task.targetJAR} for compiled test fixtrues:\n\t", "\n\t", "\n")
      )
      IO.zip(classFiles, task.targetJAR)

      TestFixtureCompilationResult(task.targetJAR)
    }
  }

  // Utility class for validating a compilation task for necessity
  class WasUpdatedFileVisitor(val archiveDate: FileTime) extends SimpleFileVisitor[Path] {

      var wasUpdated: Boolean = false // will be initialized as a sideeffect

      private def checkDate(nextFileAttributes: BasicFileAttributes): FileVisitResult = {
          if (archiveDate.compareTo(nextFileAttributes.lastModifiedTime) < 0) {
              wasUpdated = true
              FileVisitResult.TERMINATE
          } else {
              FileVisitResult.CONTINUE
          }
      }

      override def visitFile(
          path:           Path,
          fileAttributes: BasicFileAttributes
      ): FileVisitResult = {
          checkDate(fileAttributes)
      }
      override def preVisitDirectory(
          path:           Path,
          fileAttributes: BasicFileAttributes
      ): FileVisitResult = {
          checkDate(fileAttributes)
      }
  }
}
