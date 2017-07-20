package org.opalj.javacompilation

import sbt._
import sbt.Keys._

import java.io.File
import java.io.Writer
import java.io.PrintWriter

import org.opalj.javacompilation.FixtureDiscovery._
import org.opalj.javacompilation.FixtureCompileSpec._

 /** A plugin representing the ability to compile java fixtures against a specific
 * version of the Eclipse JDT Java compiler. The version of the compiler is specified
 *
 *
 *  Core tasks/keys:
 *  - `javaFixtureCompile` (compile a )
 *  - `javaFixturePackage` (compile with JAR packaging)
 *  - `javaFixtureDiscovery` (discover fixture compilation tasks)
 *
 * === Input for javaFixtureCompile
 *  - `javaFixtureTaskDefs` - a sequence of manually-specified tasks
                                - see: class JavaFixtureCompilationTask in FixtureCompileSpec.scala
 *
 * === Input for javaFixtureDiscovery
 *  - javaFixtureProjectsDir - the folder for discovery where each subfolder is a potential java fixture project
 *  - javaFixtureSupportDir - folder containing .jar support libraries
 *  - javaFixtureTargetDir - target folder for compilation and packaging
 */
object JavaFixtureCompiler extends AutoPlugin {

  //override def trigger = allRequirements

  object autoImport {
    // tasks of the plugin
    val javaFixtureCompile = taskKey[Seq[JavaFixtureCompilationResult]]("Compilation of java fixture projects against Eclipse JDT compiler specified statically in the plugin's dependencies")
    val javaFixturePackage = taskKey[Seq[JavaFixturePackagingResult]]("Compilation and packaging of java fixture projects against Eclipse JDT compiler specified statically in the plugin's dependencies")
    val javaFixtureDiscovery = taskKey[Seq[JavaFixtureCompilationTask]]("Discovery of java compilation tasks")

    // will be scoped to the compilation task.
    val javaFixtureTaskDefs = settingKey[Seq[JavaFixtureCompilationTask]]("Java fixture compilation task definitions for the plugin")

    // will be scoped to the discovery task
    val javaFixtureProjectsDir = settingKey[File]("Folder containing java project folders to be discovered by the plugin")
    val javaFixtureSupportDir = settingKey[File]("Folder containing support libraries for use of discovered tasks of the plugin")
    val javaFixtureTargetDir = settingKey[File]("Folder in which subfolders and JAR files for the output of discovered tasks of the plugin will be created")
  }

  import autoImport._


  lazy val baseJavafixtureSettings: Seq[Def.Setting[_]] = Seq(
    javaFixtureProjectsDir in javaFixtureDiscovery := sourceDirectory.value / "fixtures-java" / "projects",
    javaFixtureSupportDir in javaFixtureDiscovery := sourceDirectory.value / "fixtures-java" / "support",
    javaFixtureTargetDir in javaFixtureDiscovery := resourceManaged.value,

    javaFixtureTaskDefs in javaFixtureCompile := Seq(), // default: no manually defined tasks

    javaFixtureCompile := {
      Javacompilation.compileRunner(
        javaFixtureDiscovery.value ++ (javaFixtureTaskDefs in javaFixtureCompile).value,
        streams.value
      )
    },

    javaFixturePackage := {
      Javacompilation.packageRunner(javaFixtureCompile.value, streams.value)
    },

    javaFixtureDiscovery := {
      Javacompilation.discoveryRunner(
        (javaFixtureProjectsDir in javaFixtureDiscovery).value,
        (javaFixtureSupportDir in javaFixtureDiscovery).value,
        (javaFixtureTargetDir in javaFixtureDiscovery).value,
        streams.value
      )
    }

  )

  override lazy val projectSettings = inConfig(Compile)(baseJavafixtureSettings)


  /** Object that contains the task implementations for the fixture compilation
  * plugin and helper methods (e. g. for packaging).
  */
  object Javacompilation {

    /** Discovers java fixture projects using the specified directories to find
    * their implementation, configuration and support libraries.
    */
    def discoveryRunner(
      projectsDir: File,
      supportDir: File,
      targetDir: File,
      streams: TaskStreams
    ): Seq[JavaFixtureCompilationTask] = {
      val compiler: TestFixtureCompiler = resolveCompiler("default")
      if(! projectsDir.exists) {
        streams.log.warn("The sbt-java-fixture-compiler projects directory does not exist; no java fixtures will be discovered automatically.")
        return Seq()
      }
      val discovery = new OPALTestFixtureDiscovery(projectsDir, supportDir, targetDir, compiler)
      discovery.discoverFixtureTasks()
    }

    /** Compiles a given sequence of compilation tasks.
    */
    def compileRunner(
      tasks: Seq[JavaFixtureCompilationTask],
      streams: TaskStreams
    ): Seq[JavaFixtureCompilationResult] = {
      val log = streams.log
      val std = new PrintWriter(new LogWriter((s: String) ⇒ log.info(s)))
      val err = new PrintWriter(new LogWriter((s: String) ⇒ log.error(s)))

      val results = (for(
        fixtureTask <- tasks.par
      ) yield {
        fixtureTask.compiler.compile(fixtureTask, std, err, log)
      }).seq

      val (skipped, notSkipped) = results.toSeq.partition(_.wasSkipped)
      if(results.isEmpty) {
        streams.log.debug("No java fixtures found for compiling.")
      } else if(notSkipped.isEmpty) {
        streams.log.debug("All java fixtures were already found to be compiled.")
      } else {
        streams.log.info(s"${notSkipped.size}/${results.size} java fixtures have been compiled, the rest was already found to be compiled.");
      }

      results
    }

    /**
    * Packages a sequence of compilation results as individual JAR files in the
    * target folder of the compilation (where the .class files are).
    */
    def packageRunner(
      compilationResults: Seq[JavaFixtureCompilationResult],
      streams: TaskStreams
    ): Seq[JavaFixturePackagingResult] = {
      val results = (for(
        compilationResult <- compilationResults.par
      ) yield {
        packageRoutine(compilationResult, new File(compilationResult.task.targetFolder+".jar"), streams)
      }).seq

      val (skipped, notSkipped) = results.toSeq.partition(_.wasSkipped)
      if(results.isEmpty) {
        streams.log.debug("No java fixtures found for packaging.")
      } else if(notSkipped.isEmpty) {
        streams.log.debug("All java fixtures were already found to be packaged.")
      } else {
        streams.log.info(s"${notSkipped.size}/${results.size} java fixtures have been packaged, the rest was already found to be packaged.");
      }

      results
    }

    /**
    * Packages the result of the compilation of a java fixture. The resulting .jar
    * file will reside in the target folder of the compilation.
    */
    def packageRoutine(
      compilationResult: JavaFixtureCompilationResult,
      targetJar: File,
      streams: TaskStreams
    ): JavaFixturePackagingResult = {
      val inputFiles = (compilationResult.task.fixture.sourceFolder ** ("*.java" || "compiler.config")).get

      val newestOutputDate = if (targetJar.exists) targetJar.lastModified else 0L
      val newestInputDate = inputFiles.map(_.lastModified).foldLeft(0L)(Math.max(_,_))
      val packagingNecessary = newestOutputDate < newestInputDate;

      if(packagingNecessary) {
        val targetFolderLength = compilationResult.task.targetFolder.toString.length + 1
        val classFiles: Traversable[(File, String)] =
            (compilationResult.task.targetFolder ** "*.class").get map { classFile ⇒
                ((classFile, classFile.toString.substring(targetFolderLength)))
            }

        streams.log.info(s"Packaging test fixture into ${targetJar.getPath}")
        IO.zip(classFiles, targetJar)
      }

      JavaFixturePackagingResult(compilationResult, targetJar, !packagingNecessary)
    }

  }

}
