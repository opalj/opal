import sbt.Test
import sbtassembly.AssemblyPlugin.autoImport.*
import sbtunidoc.ScalaUnidocPlugin

name := "OPAL Library"

// SNAPSHOT
ThisBuild / version := "5.0.1-SNAPSHOT"
// RELEASED version in ThisBuild := "5.0.0" // January 23rd, 2023
// RELEASED version in ThisBuild := "4.0.0" // May 7th, 2021
// SNAPSHOT version in ThisBuild := "3.0.0-SNAPSHOT" // available since June 7th, 2019
// RELEASED version in ThisBuild := "2.0.1" // October 10th, 2018
// RELEASED version in ThisBuild := "2.0.0" // October 2nd, 2018
// RELEASED version in ThisBuild := "1.0.0" // October 25th, 2017
// RELEASED version in ThisBuild := "0.8.15" // September 7th, 2017
// RELEASED version in ThisBuild := "0.8.14" // June 23rd, 2017
// RELEASED version in ThisBuild := "0.8.13" // MAY 19th, 2017
// RELEASED version in ThisBuild := "0.8.12" // April 28th, 2017
// RELEASED version in ThisBuild := "0.8.11" // April 14th, 2017
// RELEASED version in ThisBuild := "0.8.10"
// RELEASED version in ThisBuild := "0.8.9"

ThisBuild / organization := "de.opal-project"
ThisBuild / homepage := Some(url("https://www.opal-project.de"))
ThisBuild / licenses := Seq("BSD-2-Clause" -> url("https://opensource.org/licenses/BSD-2-Clause"))

usePgpKeyHex("80B9D3FB5A8508F6B4774932E71AFF01E234090C")

ThisBuild / scalaVersion := "2.13.11"

ScalacConfiguration.globalScalacOptions

ThisBuild / resolvers += Resolver.jcenterRepo
ThisBuild / resolvers += "Typesafe Repo" at "https://repo.typesafe.com/typesafe/releases/"
ThisBuild / resolvers += "Eclipse Staging" at "https://repo.eclipse.org/content/repositories/eclipse-staging/"
ThisBuild / resolvers += Resolver.mavenLocal

// OPAL already parallelizes most tests/analyses internally!
ThisBuild / parallelExecution := false
Global / parallelExecution := false

ThisBuild / logBuffered := false

ThisBuild / javacOptions ++= Seq("-encoding", "utf8", "-source", "1.8")

ThisBuild / testOptions := {
  baseDirectory
    .map(bd => Seq(Tests.Argument("-u", bd.getAbsolutePath + "/shippable/testresults")))
    .value
}

ThisBuild / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "2")

ThisBuild / testOptions += Tests.Argument("-o")

// Required to get relative links in the generated source code documentation.
ScalaUnidoc / unidoc / scalacOptions := {
  baseDirectory.map(bd => Seq("-sourcepath", bd.getAbsolutePath)).value
}

ScalaUnidoc / unidoc / scalacOptions ++=
  Opts.doc.sourceUrl(
    "https://raw.githubusercontent.com/stg-tud/opal/" +
      (if (isSnapshot.value) "develop" else "master") +
      "/€{FILE_PATH}.scala"
  )
ScalaUnidoc / unidoc / scalacOptions ++= Opts.doc.version(version.value)
ScalaUnidoc / unidoc / scalacOptions ++= Opts.doc.title("The OPAL Framework")

ThisBuild / javaOptions ++= Seq(
  "-Xmx24G",
  "-Xms4096m",
  "-XX:ThreadStackSize=2048",
  "-Xnoclassgc",
  "-XX:NewRatio=1",
  "-XX:SurvivorRatio=8",
  "-XX:+UseParallelGC"
)

addCommandAlias(
  "compileAll",
    "; copyResources ; " +
    "OPAL / scalafmt ; OPAL / headerCheck ; " +
    "OPAL / Test / scalafmt ; OPAL / Test / headerCheck ; OPAL / Test / compile ; " +
    "OPAL/ IntegrationTest/ scalafmt ; OPAL / IntegrationTest / headerCheck ; OPAL / IntegrationTest / compile "
)

addCommandAlias("buildAll", "; compileAll ; unidoc ;  publishLocal ")

addCommandAlias(
  "cleanAll",
  "; clean ; cleanCache ; cleanLocal ; OPAL / Test / clean ; OPAL / IntegrationTest / clean ; cleanFiles"
)

addCommandAlias("cleanBuild", "; project OPAL ; cleanAll ; buildAll ")

addCommandAlias("buildAllCross", "; compileAll ; unidoc ;  publishLocal ; " +
                        "project LLVM ; compileAll ; unidoc ; publishLocal ;" +
                        "project ValidateCross ; compileAll ; publishLocal ; " +
                        // Add other crosslanguage projects here
                        " project OPAL ;")

addCommandAlias("cleanBuildCross", "; project OPAL ; cleanAll ; buildAll ; " +
                        "project LLVM ; cleanAll; buildAll ;" +
                        "project ValidateCross ; cleanAll ; buildAll ;" +
                        // Add other crosslanguage projects here
                        " project OPAL ;")

lazy val IntegrationTest = config("it") extend Test

// Default settings without scoverage
lazy val buildSettings =
  Defaults.coreDefaultSettings ++
    PublishingOverwrite.onSnapshotOverwriteSettings ++
    Seq(libraryDependencies ++= Dependencies.testlibs) ++
    Seq(inConfig(IntegrationTest)(Defaults.testSettings): _*) ++
    Seq(
      unmanagedSourceDirectories
        .withRank(KeyRanks.Invisible) := (Compile / scalaSource).value :: Nil
    ) ++
    Seq(
      Test / unmanagedSourceDirectories := (Test / javaSource).value :: (Test / scalaSource).value :: Nil
    ) ++
    Seq(
      IntegrationTest / unmanagedSourceDirectories := (Test / javaSource).value :: (IntegrationTest / scalaSource).value :: Nil
    ) ++
    Seq(Compile / console / scalacOptions := Seq("-deprecation")) ++
    // We don't want the build to be aborted by inter-project links that are reported by
    // scaladoc as errors using the standard compiler setting. (This case is only true, when
    // we publish the projects.)
    Seq(Compile / doc / scalacOptions := Opts.doc.version(version.value)) ++
    // Discard module-info files when assembling fat jars
    // see https://github.com/sbt/sbt-assembly/issues/391
    Seq(assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case PathList("META-INF", "versions", xs @ _, "module-info.class") => MergeStrategy.discard
      case PathList("META-INF", "native-image", xs @ _, "jnijavacpp", "jni-config.json") => MergeStrategy.discard
      case PathList("META-INF", "native-image", xs @ _, "jnijavacpp", "reflect-config.json") => MergeStrategy.discard
      case other => (assembly / assemblyMergeStrategy).value(other)
    }) ++
      Seq(
          headerLicense := Some(HeaderLicense.Custom("BSD 2-Clause License - see OPAL/LICENSE for details.")),
          headerEmptyLine := false,
          headerMappings :=
              headerMappings.value ++ Seq(
                  (HeaderFileType.scala -> LicenseHeaderConfig.defaultHeader),
                  (HeaderFileType.java -> LicenseHeaderConfig.defaultHeader)
              )
      )


/*******************************************************************************
 *
 * THE ROOT PROJECT
 *
 ******************************************************************************/
lazy val opal = `OPAL`
lazy val `OPAL` = (project in file("."))
//  .configure(_.copy(id = "OPAL"))
  .settings(Defaults.coreDefaultSettings ++ Seq(publishArtifact := false): _*)
  .enablePlugins(ScalaUnidocPlugin)
  .disablePlugins(HeaderPlugin) // The root project has no sources and no configured license header
  .settings(
    ScalaUnidoc / unidoc / unidocProjectFilter := inAnyProject -- inProjects(
      hermes,
      validate,
      demos,
      tools
    ))
  .aggregate(
    common,
    si,
    bi,
    br,
    da,
    bc,
    ba,
    ai,
    ifds,
    tac,
    xl,
    de,
    av,
    apk,
    framework,
    //  bp, (just temporarily...)
    tools,
    hermes,
    validate, // Not deployed to maven central
    demos // Not deployed to maven central
  )

/*******************************************************************************
 *
 * THE CORE PROJECTS WHICH CONSTITUTE OPAL
 *
 ******************************************************************************/
lazy val common = `Common`
lazy val `Common` = (project in file("OPAL/common"))
  .settings(buildSettings: _*)
  .settings(
    name := "Common",
    Compile / doc / scalacOptions := Opts.doc.title("OPAL-Common"),
    libraryDependencies ++= Dependencies.common(scalaVersion.value)
  )
  .configs(IntegrationTest)

lazy val si = `StaticAnalysisInfrastructure`
lazy val `StaticAnalysisInfrastructure` = (project in file("OPAL/si"))
  .settings(buildSettings: _*)
  .settings(
    name := "Static Analysis Infrastructure",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Static Analysis Infrastructure"),
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
    Compile / doc / scalacOptions := Opts.doc.title("OPAL - Bytecode Infrastructure"),
    // Test / publishArtifact := true, // Needed to get access to class TestResources
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
            (javaFixtureDiscovery / javaFixtureProjectsDir).value,
            (javaFixtureDiscovery / javaFixtureSupportDir).value
          ),
          resourceGenerators += Def.task {
            (Test / javaFixturePackage).value.flatMap(_.generatedFiles)
          }
        )
    )
  )
  .dependsOn(common % "it->test;test->test;compile->compile")
  .configs(IntegrationTest)
  .enablePlugins(JavaFixtureCompiler)

lazy val br = `BytecodeRepresentation`
lazy val `BytecodeRepresentation` = (project in file("OPAL/br"))
  .settings(buildSettings: _*)
  .settings(
    name := "Bytecode Representation",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Bytecode Representation"),
    libraryDependencies ++= Dependencies.br,
    // Test / publishArtifact := true // Needed to get access to class TestResources and TestSupport
   )
  .dependsOn(si % "it->it;it->test;test->test;compile->compile")
  .dependsOn(bi % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val da = `BytecodeDisassembler`
lazy val `BytecodeDisassembler` = (project in file("OPAL/da"))
  .settings(buildSettings: _*)
  .settings(
    name := "Bytecode Disassembler",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Bytecode Disassembler"),
    //[currently we can only use an unversioned version] assemblyJarName
    //in assembly := "OPALBytecodeDisassembler.jar-" + version.value
    assembly / assemblyJarName := "OPALDisassembler.jar",
    assembly / mainClass := Some("org.opalj.da.Disassembler")
  )
  .dependsOn(bi % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val bc = `BytecodeCreator`
lazy val `BytecodeCreator` = (project in file("OPAL/bc"))
  .settings(buildSettings: _*)
  .settings(
    name := "Bytecode Creator",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Bytecode Creator")
  )
  .dependsOn(da % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val ai = `AbstractInterpretationFramework`
lazy val `AbstractInterpretationFramework` = (project in file("OPAL/ai"))
  .settings(buildSettings: _*)
  .settings(
    name := "Abstract Interpretation Framework",
    Compile / doc / scalacOptions := (Opts.doc
      .title("OPAL - Abstract Interpretation Framework") ++ Seq("-groups", "-implicits")),
    run / fork := true
  )
  .dependsOn(br % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val ifds = `IFDS`
lazy val `IFDS` = (project in file("OPAL/ifds"))
  .settings(buildSettings: _*)
  .settings(
    name := "IFDS",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - IFDS"),
    fork := true,
    libraryDependencies ++= Dependencies.ifds
  )
  .dependsOn(si % "it->it;it->test;test->test;compile->compile")
  .dependsOn(br % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val tac = `ThreeAddressCode`
lazy val `ThreeAddressCode` = (project in file("OPAL/tac"))
  .settings(buildSettings: _*)
  .settings(
    name := "Three Address Code",
    Compile / doc / scalacOptions := (Opts.doc
      .title("OPAL - Three Address Code") ++ Seq("-groups", "-implicits")),
    assembly / assemblyJarName := "OPALTACDisassembler.jar",
    assembly / mainClass := Some("org.opalj.tac.TAC"),
    run / fork := true
  )
  .dependsOn(ai % "it->it;it->test;test->test;compile->compile")
  .dependsOn(ifds % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val xl = `XLanguage`
lazy val `XLanguage` = (project in file("OPAL/xl"))
    .settings(buildSettings: _*)
    .settings(
        name := "Cross Language",
        libraryDependencies += "dk.brics.tajs" % "XLTAJSprivate" % "1.0.1" from "https://www.brics.dk/TAJS/dist/tajs-all.jar",
        Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Cross Language"),
        libraryDependencies += "tud" % "svf_xl" % "1.0.0-SNAPSHOT",
      Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Cross Language"),
        run / fork := true
    )
    .dependsOn(tac % "it->it;it->test;test->test;compile->compile")
    .configs(IntegrationTest)

lazy val ba = `BytecodeAssembler`
lazy val `BytecodeAssembler` = (project in file("OPAL/ba"))
  .settings(buildSettings: _*)
  .settings(
    name := "Bytecode Assembler",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Bytecode Assembler")
  )
  .dependsOn(
    bc % "it->it;it->test;test->test;compile->compile",
    ai % "it->it;it->test;test->test;compile->compile"
  )
  .configs(IntegrationTest)

// The project "DependenciesExtractionLibrary" depends on the abstract interpretation framework to
// be able to resolve calls using MethodHandle/MethodType/"invokedynamic"/...
lazy val de = `DependenciesExtractionLibrary`
lazy val `DependenciesExtractionLibrary` = (project in file("OPAL/de"))
  .settings(buildSettings: _*)
  .settings(
    name := "Dependencies Extraction Library",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Dependencies Extraction Library")
  )
  .dependsOn(ai % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val av = `ArchitectureValidation`
lazy val `ArchitectureValidation` = (project in file("OPAL/av"))
  .settings(buildSettings: _*)
  .settings(
    name := "Architecture Validation",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Architecture Validation"),
    // Test / publishArtifact := true
  )
  .dependsOn(de % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val ll = `LLVM`
lazy val `LLVM` = (project in file("OPAL/ll"))
  .settings(buildSettings: _*)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    name := "LLVM",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - LLVM"),
    fork := true,
    libraryDependencies ++= Dependencies.llvm
  )
  .dependsOn(tac % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val apk = `APK`
lazy val `APK` = (project in file("OPAL/apk"))
  .settings(buildSettings: _*)
  .settings(
    name := "APK",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - APK"),
    libraryDependencies ++= Dependencies.apk,
  )
  .dependsOn(
    br % "it->it;it->test;test->test;compile->compile",
    ll % "it->it;it->test;test->test;compile->compile"
  )
  .configs(IntegrationTest)

lazy val framework = `Framework`
lazy val `Framework` = (project in file("OPAL/framework"))
  .settings(buildSettings: _*)
  .settings(
    name := "Framework",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Framework"),
    run / fork := true
  )
  .dependsOn(
    ba  % "it->it;it->test;test->test;compile->compile",
    av  % "it->it;it->test;test->test;compile->compile",
    tac % "it->it;it->test;test->test;compile->compile",
    xl % "it->it;it->test;test->test;compile->compile"
  )
  .configs(IntegrationTest)

/* TEMPORARILY DISABLED THE BUGPICKER UNTIL WE HAVE A CG ANALYSIS AGAIN!
lazy val bp = `BugPicker`
lazy val `BugPicker` = (project in file("TOOLS/bp"))
  .settings(buildSettings: _*)
  .settings(
    name := "BugPicker",
    scalacOptions in(Compile, doc) ++= Opts.doc.title("OPAL - BugPicker"),
    fork := true
  )
  .dependsOn(framework % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)
 */

lazy val hermes = `Hermes`
lazy val `Hermes` = (project in file("TOOLS/hermes"))
  .settings(buildSettings: _*)
  .settings(
    name := "Hermes",
    libraryDependencies ++= Dependencies.hermes,
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Hermes")
  )
  .dependsOn(framework % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

lazy val tools = `Tools`
lazy val `Tools` = (project in file("DEVELOPING_OPAL/tools"))
  .settings(buildSettings: _*)
  .settings(
    name := "Tools",
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Developer Tools"),
    Compile / console / scalacOptions := Seq("-deprecation"),
    //library dependencies
    libraryDependencies ++= Dependencies.tools,
    assembly / assemblyJarName := "OPALInvokedynamicRectifier.jar",
    assembly / mainClass := Some("org.opalj.support.tools.ProjectSerializer"),
    // Required by Java/ScalaFX
    fork := true
  )
  .dependsOn(framework % "it->it;it->test;test->test;compile->compile")
  .configs(IntegrationTest)

/** ***************************************************************************
 *
 * PROJECTS BELONGING TO THE OPAL ECOSYSTEM
 * (Not Deployed to Maven Central!)
 *
 */
// This project validates OPAL's implemented architecture and
// contains overall integration tests; hence
// it is not a "project" in the classical sense!
lazy val validate = `Validate`
lazy val `Validate` = (project in file("DEVELOPING_OPAL/validate"))
  .settings(buildSettings: _*)
  .settings(
    name := "Validate",
    publishArtifact := false,
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Validate"),
    Test / compileOrder := CompileOrder.Mixed
  )
  .dependsOn(
    tools  % "it->it;it->test;test->test;compile->compile",
    hermes % "it->it;test->test;compile->compile"
  )
  .configs(IntegrationTest)

lazy val validateCross = `ValidateCross`
lazy val `ValidateCross` = (project in file("DEVELOPING_OPAL/validateCross"))
  .settings(buildSettings: _*)
  .settings(
    name := "Validate Cross",
    publishArtifact := false,
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Validate"),
    Test / compileOrder := CompileOrder.Mixed
  )
  .dependsOn(
      ll % "it->it;it->test;test->test;compile->compile",
      validate % "it->it;it->test;test->test;compile->compile"
  )
  .configs(IntegrationTest)


lazy val demos = `Demos`
lazy val `Demos` = (project in file("DEVELOPING_OPAL/demos"))
  .settings(buildSettings: _*)
  .settings(
    name := "Demos",
    publishArtifact := false,
    Compile / doc / scalacOptions ++= Opts.doc.title("OPAL - Demos"),
    Compile / unmanagedSourceDirectories := (Compile / javaSource).value :: (Compile / scalaSource).value :: Nil,
    run / fork := true
  )
  .dependsOn(framework)
  .configs(IntegrationTest)

/* ***************************************************************************
 *
 * TASKS, etc
 *
 */
// To run the task: compile:generateSite
val generateSite = Compile / taskKey[File]("creates the OPAL website")
generateSite := {
  lazy val disassemblerJar = (da / assembly).value
  lazy val projectSerializerJar = (Tools / assembly).value
  val runUnidoc = (Compile / unidoc).value
  SiteGeneration.generateSite(
    sourceDirectory.value,
    resourceManaged.value,
    streams.value,
    disassemblerJar,
    projectSerializerJar
  )
}

compile := {
  val r = (Compile / compile).value
  (Compile / generateSite).value
  r
}

lazy val runProjectDependencyGeneration =  ThisBuild / taskKey[Unit] ("Regenerates the Project Dependencies Graphics")

runProjectDependencyGeneration := {
  import scala.sys.process.*
  val s: TaskStreams = streams.value
  val uid = "id -u".!!.stripSuffix("\n")
  val gid = "id -g".!!.stripSuffix("\n")
  val baseCommand = s"docker run --userns=host --rm -u $uid:$gid -v ${baseDirectory.value.getAbsolutePath}/:/data minlag/mermaid-cli -i OPAL/ProjectDependencies.mmd -c mermaid-config.json"
  s.log.info("Regenerating ProjectDependencies.svg")
  baseCommand + " -o OPAL/ProjectDependencies.svg" ! s.log
  s.log.info("Regenerating ProjectDependencies.pdf")
  baseCommand + "  -o OPAL/ProjectDependencies.pdf" ! s.log
}
//
//
// SETTINGS REQUIRED TO PUBLISH OPAL ON MAVEN CENTRAL
//
//

ThisBuild / publishMavenStyle.withRank(KeyRanks.Invisible) := true
Test / publishArtifact := false
ThisBuild / publishTo := MavenPublishing.publishTo(isSnapshot.value)
ThisBuild / pomExtra := MavenPublishing.pomNodeSeq()
