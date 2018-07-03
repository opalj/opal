import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

import sbtassembly.AssemblyPlugin.autoImport._
import sbtunidoc.ScalaUnidocPlugin

name := "OPAL Library"

// SNAPSHOT
version in ThisBuild := "1.1.0-SNAPSHOT"
// RELEASED version 		in ThisBuild := "1.0.0" // October 25th, 2017
// RELEASED version 		in ThisBuild := "0.8.15" // September 7th, 2017
// RELEASED version 		in ThisBuild := "0.8.14" // June 23rd, 2017
// RELEASED version 		in ThisBuild := "0.8.13" // MAY 19th, 2017
// RELEASED version 		in ThisBuild := "0.8.12" // April 28th, 2017
// RELEASED version 		in ThisBuild := "0.8.11" // April 14th, 2017
// RELEASED version 		in ThisBuild := "0.8.10"
// RELEASED version 		in ThisBuild := "0.8.9"

organization in ThisBuild := "de.opal-project"
homepage in ThisBuild := Some(url("http://www.opal-project.de"))
licenses in ThisBuild := Seq("BSD-2-Clause" -> url("http://opensource.org/licenses/BSD-2-Clause"))

scalaVersion  in ThisBuild := "2.12.6"

ScalacConfiguration.globalScalacOptions

resolvers in ThisBuild += Resolver.jcenterRepo
resolvers in ThisBuild += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

// in OPAL most tests/analyses are already parallelized internally
parallelExecution in ThisBuild := false
parallelExecution in Global := false

logBuffered in ThisBuild := false

javacOptions in ThisBuild ++= Seq("-encoding", "utf8", "-source", "1.8")

testOptions in ThisBuild := {
    baseDirectory.map(bd =>
        Seq(Tests.Argument("-u", bd.getAbsolutePath + "/shippable/testresults"))
    ).value
}

testOptions in ThisBuild += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "2")

testOptions in ThisBuild += Tests.Argument("-o")

// Required to get relative links in the generated source code documentation.
scalacOptions in(ScalaUnidoc, unidoc) := {
    baseDirectory.map(bd => Seq("-sourcepath", bd.getAbsolutePath)).value
}

scalacOptions in(ScalaUnidoc, unidoc) ++=
    Opts.doc.sourceUrl(
        "https://bitbucket.org/delors/opal/src/HEAD€{FILE_PATH}.scala?" +
            (if (isSnapshot.value) "at=develop" else "at=master")
    )
scalacOptions in (ScalaUnidoc, unidoc) ++= Opts.doc.version(version.value)
scalacOptions in (ScalaUnidoc, unidoc) ++= Opts.doc.title("The OPAL Framework")

javaOptions in ThisBuild ++= Seq(
    "-Xmx7G", "-Xms1024m", "-XX:ThreadStackSize=2048", "-Xnoclassgc",
    "-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC", "-XX:+AggressiveOpts")

addCommandAlias("compileAll", "; copyResources ; scalastyle ; test:compile ; test:scalastyle ; it:scalariformFormat ; it:scalastyle ; it:compile ")

addCommandAlias("buildAll", "; compileAll ; unidoc ;  publishLocal ")

addCommandAlias("cleanAll", "; clean ; cleanCache ; cleanLocal ; test:clean ; it:clean ; cleanFiles")

addCommandAlias("cleanBuild", "; project OPAL ; cleanAll ; buildAll ")

lazy val IntegrationTest = config("it") extend Test

// Default settings without scoverage
lazy val buildSettings =
    Defaults.coreDefaultSettings ++ scalariformSettings ++
        PublishingOverwrite.onSnapshotOverwriteSettings ++
        Seq(libraryDependencies ++= Dependencies.testlibs) ++
        Seq(Defaults.itSettings: _*) ++
        Seq(unmanagedSourceDirectories := (scalaSource in Compile).value :: Nil) ++
        Seq(unmanagedSourceDirectories in Test := (javaSource in Test).value :: (scalaSource in Test).value :: Nil) ++
        Seq(unmanagedSourceDirectories in IntegrationTest := (javaSource in Test).value :: (scalaSource in IntegrationTest).value :: Nil) ++
        Seq(scalacOptions in(Compile, console) := Seq("-deprecation")) ++
        // We don't want the build to be aborted by inter-project links that are reported by
        // scaladoc as errors using the standard compiler setting. (This case is only true, when
        // we publish the projects.)
        Seq(scalacOptions in(Compile, doc):= Opts.doc.version(version.value))

lazy val scalariformSettings = scalariformItSettings ++
    Seq(ScalariformKeys.preferences := baseDirectory(getScalariformPreferences).value)

def getScalariformPreferences(dir: File) = {
    val formatterPreferencesFile = "Scalariform Formatter Preferences.properties"
    PreferencesImporterExporter.loadPreferences(file(formatterPreferencesFile).getPath)
}

lazy val opal = `OPAL`
lazy val `OPAL` = (project in file("."))
  //  .configure(_.copy(id = "OPAL"))
  .settings((Defaults.coreDefaultSettings ++ Seq(publishArtifact := false)): _*)
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(
    common,
    si,
    bi,
    br,
    da,
    bc,
    ba,
    ai,
    // DISABLE BUGPICKER      bp,
    de,
    av,
    DeveloperTools,
    Validate,
    demos,
    incubation)


/** ***************************************************************************
  *
  * THE CORE PROJECTS WHICH CONSTITUTE OPAL
  *
  */

lazy val common = `Common`
lazy val `Common` = (project in file("OPAL/common"))
  .settings(buildSettings: _*)
  .settings(
    name := "Common",
    scalacOptions in(Compile, doc) := Opts.doc.title("OPAL-Common"),
    libraryDependencies ++= Dependencies.common(scalaVersion.value)
  )
  .configs(IntegrationTest)

lazy val si = `StaticAnalysisInfrastructure`
lazy val `StaticAnalysisInfrastructure` = (project in file("OPAL/si"))
    .settings(buildSettings: _*)
    .settings(
        name := "StaticAnalysisInfrastructure",
        scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - Static Analysis Infrastructure"),
        libraryDependencies ++= Dependencies.si
    )
    .configs(IntegrationTest)
    .dependsOn(common % "it->test;test->test;compile->compile")

lazy val bi = `BytecodeInfrastructure`
lazy val `BytecodeInfrastructure` = (project in file("OPAL/bi"))
  .settings(buildSettings: _*)
  .settings(
    name := "Bytecode Infrastructure",
    libraryDependencies ++= Dependencies.bi,
    scalacOptions in(Compile, doc) := Opts.doc.title("OPAL - Bytecode Infrastructure"),
    /*
      The following settings relate to the java-fixture-compiler plugin, which
      compiles the java fixture projects in the BytecodeInfrastructure project for testing.
      For information about the java fixtures, see: OPAL/bi/src/test/fixtures-java/Readme.md

      The default settings for the fixture compilations are used.
      For details on the plugin and how to change its settings, see:
      DEVELOPING_OPAL/plugins/sbt-java-fixture-compiler/Readme.md
    */
    inConfig(Test)(
      JavaFixtureCompiler.baseJavaFixtureSettings ++
        Seq(
          unmanagedResourceDirectories ++= Seq(
            (javaFixtureProjectsDir in javaFixtureDiscovery).value,
            (javaFixtureSupportDir in javaFixtureDiscovery).value
          ),
          resourceGenerators += Def.task {
            (javaFixturePackage in Test).value.flatMap(_.generatedFiles)
          }
        )
    )
  )
  .dependsOn(si % "it->test;test->test;compile->compile")
  .configs(IntegrationTest)
  .enablePlugins(JavaFixtureCompiler)

lazy val br = `BytecodeRepresentation`
lazy val `BytecodeRepresentation` = (project in file("OPAL/br"))
  .settings(buildSettings: _*)
  .settings(
    name := "Bytecode Representation",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - Bytecode Representation"),
    libraryDependencies ++= Dependencies.br
  )
  .dependsOn(bi % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val da = `BytecodeDisassembler`
lazy val `BytecodeDisassembler` = (project in file("OPAL/da"))
  .settings(buildSettings: _*)
  .settings(
    name := "Bytecode Disassembler",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - Bytecode Disassembler"),
    //[currently we can only use an unversioned version] assemblyJarName
    //in assembly := "OPALBytecodeDisassembler.jar-" + version.value
    assemblyJarName in assembly := "OPALDisassembler.jar",
    mainClass in assembly := Some("org.opalj.da.Disassembler")
  )
  .dependsOn(bi % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val bc = `BytecodeCreator`
lazy val `BytecodeCreator` = (project in file("OPAL/bc"))
  .settings(buildSettings: _*)
  .settings(
    name := "Bytecode Creator",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - Bytecode Creator")
  )
  .dependsOn(da % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val ai = `AbstractInterpretationFramework`
lazy val `AbstractInterpretationFramework` = (project in file("OPAL/ai"))
  .settings(buildSettings: _*)
  .settings(
    name := "Abstract Interpretation Framework",
    scalacOptions in(Compile, doc) := (Opts.doc.title("OPAL - Abstract Interpretation Framework") ++ Seq("-groups", "-implicits")),
    fork in run := true
  )
  .dependsOn(br % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val ba = `BytecodeAssembler`
lazy val `BytecodeAssembler` = (project in file("OPAL/ba"))
  .settings(buildSettings: _*)
  .settings(
      name := "Bytecode Assembler",
      scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - Bytecode Assembler")
  )
  .dependsOn(
      bc % "it->it;it->test;test->test;compile->compile",
      ai % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

// The project "DependenciesExtractionLibrary" depends on
// the abstract interpretation framework to be able to
// resolve calls using MethodHandle/MethodType/"invokedynamic"/...
lazy val de = `DependenciesExtractionLibrary`
lazy val `DependenciesExtractionLibrary` = (project in file("OPAL/de"))
  .settings(buildSettings: _*)
  .settings(
    name := "Dependencies Extraction Library",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - Dependencies Extraction Library")
  )
  .dependsOn(ai % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

/* TEMPORARILY DISABLED THE BUGPICKER UNTIL WE HAVE A CG ANALYSIS AGAIN!
lazy val bp = `BugPicker`
lazy val `BugPicker` = (project in file("OPAL/bp"))
  .settings(buildSettings: _*)
  .settings(
    name := "BugPicker - Core",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("BugPicker - Core"),
    fork := true
  )
  .dependsOn(ai % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)
*/

lazy val av = `ArchitectureValidation`
lazy val `ArchitectureValidation` = (project in file("OPAL/av"))
  .settings(buildSettings: _*)
  .settings(
    name := "Architecture Validation",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - Architecture Validation")
  )
  .dependsOn(de % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val DeveloperTools = `OPAL-DeveloperTools`
lazy val `OPAL-DeveloperTools` = (project in file("DEVELOPING_OPAL/tools"))
  .settings(buildSettings: _*)
  .settings(
    name := "OPAL-Developer Tools",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - Developer Tools"),
    scalacOptions in(Compile, console) := Seq("-deprecation"),
    //library dependencies
    libraryDependencies ++= Dependencies.developertools,
    assemblyJarName in assembly := "OPALLambdaRectifier.jar",
    mainClass in assembly := Some("org.opalj.support.tools.ProjectSerializer"),
    // Required by Java/ScalaFX
    fork := true
  )
  .dependsOn(
    av % "test->test;compile->compile",
    // DISABLED BUGPICKER      bp % "test->test;compile->compile",
    ba % "test->test;compile->compile;it->it")
  .configs(IntegrationTest)

// This project validates OPAL's implemented architecture and
// contains overall integration tests; hence
// it is not a "project" in the classical sense!
lazy val Validate = `OPAL-Validate`
lazy val `OPAL-Validate` = (project in file("DEVELOPING_OPAL/validate"))
  .settings(buildSettings: _*)
  .settings(
    publishArtifact := false,
    name := "OPAL-Validate",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - Validate"),
    libraryDependencies ++= Dependencies.validate,
    compileOrder in Test := CompileOrder.Mixed,
    forkedJvmOptions in Perf := Seq("-Xmx10240M")
  )
  .dependsOn(DeveloperTools % "compile->compile;test->test;it->it;it->test")
  .configs(IntegrationTest)

lazy val demos = `Demos`
lazy val `Demos` = (project in file("OPAL/demos"))
  .settings(buildSettings: _*)
  .settings(
    name := "Demos",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - Demos"),
    unmanagedSourceDirectories in Compile := (javaSource in Compile).value :: (scalaSource in Compile).value :: Nil,
    fork in run := true
  )
  .dependsOn(av, ba)
  .configs(IntegrationTest)

/** ***************************************************************************
  *
  * PROJECTS BELONGING TO THE OPAL ECOSYSTEM
  *
  */
lazy val incubation = `Incubation`
lazy val `Incubation` = (project in file("OPAL/incubation"))
  .settings(buildSettings: _*)
  .settings(
    name := "Incubation",
    // INCUBATION CODE IS NEVER EVEN CONSIDERED TO BE ALPHA QUALITY
    version := "ALWAYS-SNAPSHOT",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("Incubation"),
    fork in run := true,
    publishArtifact := false
  )
  .dependsOn(
    av % "it->it;it->test;test->test;compile->compile",
    ba % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

/** ***************************************************************************
 *
 * TASKS, etc
 *
 */

// To run the task: OPAL/publish::generateSite or compile:generateSite
val generateSite = taskKey[File]("creates the OPAL website") in Compile
generateSite := {
    lazy val disassemblerJar = (assembly in da).value
    lazy val projectSerializerJar = (assembly in DeveloperTools).value
    val runUnidoc = (unidoc in Compile).value

    SiteGeneration.generateSite(
        sourceDirectory.value,
        resourceManaged.value,
        streams.value,
        disassemblerJar,
        projectSerializerJar
    )
}

compile := {
    val r = (compile in Compile).value
    (generateSite in Compile).value
    r
}

//
//
// SETTINGS REQUIRED TO PUBLISH OPAL ON MAVEN CENTRAL
//
//

publishMavenStyle in ThisBuild := true
publishArtifact in Test := false
publishTo in ThisBuild := MavenPublishing.publishTo(isSnapshot.value)
pomExtra in ThisBuild := MavenPublishing.pomNodeSeq()
