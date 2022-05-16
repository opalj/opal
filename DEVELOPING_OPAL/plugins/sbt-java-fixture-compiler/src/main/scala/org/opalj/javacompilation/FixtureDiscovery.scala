/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.javacompilation

import sbt._
import java.io.File
import java.io.Writer
import scala.io.Source.fromFile

import org.opalj.javacompilation.FixtureCompileSpec._

/**
 * Defines methods to find and compile test fixtures.
 *
 * E.g., some test fixtures are found in the OPAL/bi project (OPAL/bi/src/test/fixtures-java/Readme.md)
 *
 * @author Simon Leischnig
 */
object FixtureDiscovery {

  /**
   * Returns compilation tasks for test fixtures that were found in the configured folders.
   */
  class OPALTestFixtureDiscovery(
      projectsDir: File,
      supportDir: File,
      targetFolder: File, // where the subfolders for the discovered projects will be created
      compiler: TestFixtureCompiler
  ) {

    /** Finds and returns all test fixtures in the project. */
    def discoverFixtureTasks(): Seq[JavaFixtureCompilationTask] = {
      for {
        sourceFolder <- projectsDir.listFiles
        if sourceFolder.isDirectory
        configFile = sourceFolder.getAbsoluteFile / "compiler.config"
        (supportLibraries, defaultConfigurationOptions) = parseConfigFile(configFile)
        configurationOptions <- defaultConfigurationOptions
      } yield {

        val fixture = TestFixture(sourceFolder)
        val outputFolder = obtainTargetFolder(configFile, sourceFolder, configurationOptions)

        JavaFixtureCompilationTask(
          fixture,
          outputFolder,
          configurationOptions,
          supportLibraries,
          compiler
        )
      }
    }

    /**
     * Returns ''require'' specifications, and configuration options for a test
     * fixture that are (optionally) specified in a "compiler.config" file in the fixture.
     *
     * This involves checking if a config file exists (default values if not),
     * filtering comments out, and partitioning by the 'requires' keyword.
     *
     * @param configFile configuration file of the test fixture (may or may not exist)
     * @return Returns a pair of 'requires' specs and config options for the compiler
     */
    def parseConfigFile(configFile: File) = {
      if (configFile.exists) {
        val (requires, configurationOptions) = fromFile(configFile).getLines
          .map(_.trim)
          .filter(l => l.nonEmpty && !l.startsWith("#"))
          .toList
          .partition(_.startsWith("requires"))

        ( // return value: pair of mapped requires specs and config options (rest)
          requires
            .map(librarySpec => librarySpec.substring(librarySpec.indexOf('=') + 1))
            . /* support library name */
            map(libraryName => supportDir / libraryName)
            . /* support library folder */
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

      targetFolder.getAbsoluteFile / (sourceFolder.getName + selectedOptionsIdentification)
    }

  }

  class LogWriter(println: String => Unit) extends Writer {
    override def flush(): Unit = {}
    override def close(): Unit = ??? // not expected to be called
    override def write(chars: Array[Char], offset: Int, length: Int): Unit = {
      println(new String(chars, offset, length))
    }
  }

}
