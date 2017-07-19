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
 *  - `javafixtureCompile` (compile a )
 *  - `javafixturePackage` (compile with JAR packaging)
 *  - `javafixtureDiscovery` (discover fixture compilation tasks)
 *
 * === Input for javafixtureCompile
 *  - `javafixtureTaskDefs` - a sequence of manually-specified tasks
                                - see: class JavaFixtureCompilationTask in FixtureCompileSpec.scala
 *
 * === Input for javafixtureDiscovery
 *  - javafixtureProjectsDir - the folder for discovery where each subfolder is a potential java fixture project
 *  - javafixtureSupportDir - folder containing .jar support libraries
 *  - javafixtureTargetDir - target folder for compilation and packaging
 */
object JavaFixtureCompiler extends AutoPlugin {

  //override def trigger = allRequirements

  object autoImport {
    // tasks of the plugin
    val javafixtureCompile = taskKey[Seq[JavaFixtureCompilationResult]]("Compilation of java fixture projects against Eclipse JDT compiler specified statically in the plugin's dependencies")
    val javafixturePackage = taskKey[Seq[JavaFixturePackagingResult]]("Compilation and packaging of java fixture projects against Eclipse JDT compiler specified statically in the plugin's dependencies")
    val javafixtureDiscovery = taskKey[Seq[JavaFixtureCompilationTask]]("Discovery of java compilation tasks")

    // will be scoped to the compilation task.
    val javafixtureTaskDefs = settingKey[Seq[JavaFixtureCompilationTask]]("Java fixture compilation task definitions for the plugin")

    // will be scoped to the discovery task
    val javafixtureProjectsDir = settingKey[File]("Folder containing java project folders to be discovered by the plugin")
    val javafixtureSupportDir = settingKey[File]("Folder containing support libraries for use of discovered tasks of the plugin")
    val javafixtureTargetDir = settingKey[File]("Folder in which subfolders and JAR files for the output of discovered tasks of the plugin will be created")
  }

  import autoImport._


  lazy val baseJavafixtureSettings: Seq[Def.Setting[_]] = Seq(
    javafixtureProjectsDir in javafixtureDiscovery := sourceDirectory.value / "fixtures-java" / "projects",
    javafixtureSupportDir in javafixtureDiscovery := sourceDirectory.value / "fixtures-java" / "support",
    javafixtureTargetDir in javafixtureDiscovery := resourceManaged.value,

    javafixtureTaskDefs in javafixtureCompile := Seq(), // default: no manually defined tasks

    javafixtureCompile := {
      Javacompilation.compileRunner(
        javafixtureDiscovery.value ++ (javafixtureTaskDefs in javafixtureCompile).value,
        streams.value
      )
    },

    javafixturePackage := {
      Javacompilation.packageRunner(javafixtureCompile.value)
    },

    javafixtureDiscovery := {
      Javacompilation.discoveryRunner(
        (javafixtureProjectsDir in javafixtureDiscovery).value,
        (javafixtureSupportDir in javafixtureDiscovery).value,
        (javafixtureTargetDir in javafixtureDiscovery).value,
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

      val results = for(
        fixtureTask <- tasks.par
      ) yield {
        fixtureTask.compiler.compile(fixtureTask, std, err, log)
      }

      results.seq
    }

    /**
    * Packages a sequence of compilation results as individual JAR files in the
    * target folder of the compilation (where the .class files are).
    */
    def packageRunner(compilationResults: Seq[JavaFixtureCompilationResult]): Seq[JavaFixturePackagingResult] = {
      val results = for(
        compilationResult <- compilationResults
      ) yield {
        packageRoutine(compilationResult, new File(compilationResult.task.targetFolder+".jar"))
      }

      results.toSeq
    }

    /**
    * Packages the result of the compilation of a java fixture. The resulting .jar
    * file will reside in the target folder of the compilation.
    */
    def packageRoutine(compilationResult: JavaFixtureCompilationResult, targetJar: File): JavaFixturePackagingResult = {
      val targetFolderLength = compilationResult.task.targetFolder.toString.length + 1
      val classFiles: Traversable[(File, String)] =
          (compilationResult.task.targetFolder ** "*.class").get map { classFile ⇒
              ((classFile, classFile.toString.substring(targetFolderLength)))
          }

      IO.zip(classFiles, targetJar)

      JavaFixturePackagingResult(compilationResult, targetJar)
    }

  }






}
