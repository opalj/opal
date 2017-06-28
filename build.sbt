import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

import sbtassembly.AssemblyPlugin.autoImport._
import sbtunidoc.ScalaUnidocPlugin

name := "OPAL Library"

// SNAPSHOT
version 		in ThisBuild := "0.9.0-SNAPSHOT"
// NEXT version 		in ThisBuild := "0.8.15"
// RELEASED version 		in ThisBuild := "0.8.14" // June 23rd, 2017
// RELEASED version 		in ThisBuild := "0.8.13" // MAY 19th, 2017
// RELEASED version 		in ThisBuild := "0.8.12" // April 28th, 2017
// RELEASED version 		in ThisBuild := "0.8.11" // April 14th, 2017
// RELEASED version 		in ThisBuild := "0.8.10"
// RELEASED version 		in ThisBuild := "0.8.9"

organization    in ThisBuild := "de.opal-project"
homepage        in ThisBuild := Some(url("http://www.opal-project.de"))
licenses        in ThisBuild := Seq("BSD-2-Clause" -> url("http://opensource.org/licenses/BSD-2-Clause"))

// [for sbt 0.13.8 onwards] crossPaths in ThisBuild := false

//scalaVersion    in ThisBuild := "2.11.11"
scalaVersion  in ThisBuild := "2.12.2"

scalacOptions 	in ThisBuild ++= Seq(
    "-Xfuture", "-feature",
    "-target:jvm-1.8", "-opt:l:method",
    "-deprecation", "-unchecked",
    "-Xlint", "-Xfatal-warnings",
    "-Ywarn-numeric-widen", "-Ywarn-nullary-unit", "-Ywarn-nullary-override",
    "-Ywarn-unused:-params,_", "-Ywarn-unused-import", "-Ywarn-infer-any",
    "-Ywarn-dead-code" , "-Ywarn-inaccessible", "-Ywarn-adapted-args"
)

scalacOptions in (ScalaUnidoc, unidoc) ++= Opts.doc.title("OPAL - OPen Analysis Library")
scalacOptions in (ScalaUnidoc, unidoc) ++= Opts.doc.version(version.value)

resolvers in ThisBuild += Resolver.jcenterRepo
resolvers in ThisBuild += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

// the tests/analysis are already parallelized
parallelExecution in ThisBuild := false
parallelExecution in Global := false

logBuffered in ThisBuild := false

javacOptions in ThisBuild ++= Seq("-encoding", "utf8")

testOptions in ThisBuild := {
		baseDirectory.map(bd =>
    		Seq(Tests.Argument("-u",  bd.getAbsolutePath + "/shippable/testresults"))
		).value
	}

testOptions in ThisBuild += Tests.Argument("-o")

// Required to get relative links in the generated source code documentation.
scalacOptions in (ScalaUnidoc, unidoc) :=  {
		baseDirectory.map(bd => Seq ("-sourcepath", bd.getAbsolutePath)).value
	}

scalacOptions in (ScalaUnidoc, unidoc) ++=
	Opts.doc.sourceUrl(
		"https://bitbucket.org/delors/opal/src/HEAD€{FILE_PATH}.scala?"+
			(if (isSnapshot.value) "at=develop" else "at=master")
    )

javaOptions in ThisBuild ++= Seq(
	"-Xmx7G", "-Xms1024m", "-Xnoclassgc",
	"-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC","-XX:+AggressiveOpts")

addCommandAlias("compileAll","; copyResources ; scalastyle ; test:compile ; test:scalastyle ; it:scalariformFormat ; it:scalastyle ; it:compile ")

addCommandAlias("buildAll","; compileAll ; unidoc ;  publishLocal ")

addCommandAlias("cleanAll","; clean ; test:clean ; it:clean ; cleanFiles ; cleanCache ; cleanLocal ")

addCommandAlias("cleanBuild","; project OPAL ; cleanAll ; buildAll ")

lazy val IntegrationTest = config("it") extend Test

// Default settings without scoverage
lazy val buildSettings =
		Defaults.coreDefaultSettings ++
		SbtScalariform.scalariformSettingsWithIt ++
		Seq(ScalariformKeys.preferences := baseDirectory(getScalariformPreferences).value) ++
		Seq(libraryDependencies ++= Dependencies.opalDefaultDependencies) ++
		Seq(Defaults.itSettings : _*) ++
		Seq(unmanagedSourceDirectories := (scalaSource in Compile).value :: Nil) ++
		Seq(unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil) ++
		Seq(unmanagedSourceDirectories in IntegrationTest := (scalaSource in IntegrationTest).value :: Nil)

def getScalariformPreferences(dir: File) = {
		val formatterPreferencesFile = "Scalariform Formatter Preferences.properties"
		PreferencesImporterExporter.loadPreferences(file(formatterPreferencesFile).getPath)
}

lazy val opal = Project(
		id = "OPAL",
		base = file("."),
		settings = Defaults.coreDefaultSettings ++ Seq(publishArtifact := false)
).
		enablePlugins(ScalaUnidocPlugin).
		aggregate(
				common,
				bi,
				br,
				da,
				bc,
				ba,
				ai,
				bp,
				de,
				av,
				DeveloperTools,
				Validate,
				demos,
				incubation
		)

/*****************************************************************************
 *
 * THE CORE PROJECTS WHICH CONSTITUTE OPAL
 *
 */

lazy val common = Project(
		id = "Common",
		base = file("OPAL/common"),
		settings = buildSettings ++
		Seq(
			name := "Common",
			// We don't want the build to be aborted by inter-project links that are reported by
			// scaladoc as errors using the standard compiler setting. (This case is only true, when
			// we publish the projects.)
			scalacOptions in (Compile, doc) := Opts.doc.title("OPAL-Common"),
			scalacOptions in (Compile, console) := Seq("-deprecation"),
      //library dependencies
			libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
			libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
			libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0",
			libraryDependencies += "com.iheart" %% "ficus" % "1.4.1"
		)
).configs(IntegrationTest)

// For the bytecode infrastructure project, the OPAL/bi/build.sbt file
// contains the task and settings that are responsible for java test fixture compilation
lazy val bi = Project(
		id = "BytecodeInfrastructure",
		base = file("OPAL/bi"),
		settings = buildSettings ++
    Seq(
      name := "Bytecode Infrastructure",
      scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Infrastructure"),
      scalacOptions in (Compile, console) := Seq("-deprecation"),
      //library dependencies
      //libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.6"
		libraryDependencies += "org.apache.commons" % "commons-text" % "1.1"
    )
).dependsOn(common % "it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val br = Project(
		id = "BytecodeRepresentation",
		base = file("OPAL/br"),
		settings = buildSettings ++
		Seq(
			name := "Bytecode Representation",
			// We don't want the build to be aborted by inter-project links that are reported by
			// scaladoc as errors if we publish the projects; hence, we do not use the
			// standard compiler settings!
			scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Representation"),
			scalacOptions in (Compile, console) := Seq("-deprecation"),
      		//library dependencies
			libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6"
		)
).dependsOn(bi % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val da = Project(
		id = "BytecodeDisassembler",
		base = file("OPAL/da"),
		settings = buildSettings ++
		Seq(
			name := "Bytecode Disassembler",
			scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Disassembler"),
			scalacOptions in (Compile, console) := Seq("-deprecation"),
			//[currently we can only use an unversioned version] assemblyJarName
			//in assembly := "OPALBytecodeDisassembler.jar-" + version.value
			assemblyJarName in assembly := "OPALDisassembler.jar",
			mainClass in assembly := Some("org.opalj.da.Disassembler")
		)
).dependsOn(bi % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val bc = Project(
		id = "BytecodeCreator",
		base = file("OPAL/bc"),
		settings = buildSettings ++
		Seq(
			name := "Bytecode Creator",
			scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Creator"),
			scalacOptions in (Compile, console) := Seq("-deprecation")
		)
).dependsOn(da % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val ba = Project(
		id = "BytecodeAssembler",
		base = file("OPAL/ba"),
		settings = buildSettings ++
		Seq(
			name := "Bytecode Assembler",
			scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Assembler"),
			scalacOptions in (Compile, console) := Seq("-deprecation")
		)
).dependsOn(
		bc % "it->it;it->test;test->test;compile->compile",
		br % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val ai = Project(
		id = "AbstractInterpretationFramework",
		base = file("OPAL/ai"),
		settings = buildSettings ++
		Seq(
			name := "Abstract Interpretation Framework",
			scalacOptions in (Compile, doc) := (Opts.doc.title("OPAL - Abstract Interpretation Framework") ++ Seq("-groups", "-implicits")),
			scalacOptions in (Compile, console) := Seq("-deprecation"),
			unmanagedSourceDirectories in Test := ((javaSource in Test).value :: (scalaSource in Test).value :: Nil),
			fork in run := true
		)
).dependsOn(br % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

// The project "DependenciesExtractionLibrary" depends on
// the abstract interpretation framework to be able to
// resolve calls using MethodHandle/MethodType/"invokedynamic"/...
lazy val de = Project(
		id = "DependenciesExtractionLibrary",
		base = file("OPAL/de"),
		settings = buildSettings ++
		Seq(
			name := "Dependencies Extraction Library",
			scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Dependencies Extraction Library"),
			scalacOptions in (Compile, console) := Seq("-deprecation")
		)
).dependsOn(ai % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val bp = Project(
		id = "BugPicker",
		base = file("OPAL/bp"),
		settings = buildSettings ++
		Seq(
			name := "BugPicker - Core",
			// We don't want the build to be aborted by inter-project links that are reported by
			// scaladoc as errors if we publish the projects; hence, we do not use the
			// standard compiler settings!
			scalacOptions in (Compile, doc) := Opts.doc.title("BugPicker - Core"),
			scalacOptions in (Compile, console) := Seq("-deprecation")
		)
).dependsOn(ai % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val av = Project(
		id = "ArchitectureValidation",
		base = file("OPAL/av"),
		settings = buildSettings ++
		Seq(
			name := "Architecture Validation",
			scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Architecture Validation"),
			scalacOptions in (Compile, console) := Seq("-deprecation")
		)
).dependsOn(de % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val DeveloperTools = Project(
		id = "OPAL-DeveloperTools",
		base = file("DEVELOPING_OPAL/tools"),
		settings = buildSettings ++
    Seq(
      name := "OPAL-Developer Tools",
      scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Developer Tools"),
      scalacOptions in (Compile, console) := Seq("-deprecation"),
      //library dependencies
      libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.102-R11" withSources() withJavadoc(),
      libraryDependencies += "org.controlsfx" % "controlsfx" % "8.40.12" withJavadoc(),
      libraryDependencies += "es.nitaur.markdown" % "txtmark" % "0.16" withJavadoc(),
      libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % "2.8.9" withJavadoc(),
      libraryDependencies += "org.choco-solver" % "choco-solver" % "4.0.4" withSources() withJavadoc(),
      // Required by Java/ScalaFX
      fork := true
    )
).dependsOn(
		av % "test->test;compile->compile",
		bp % "test->test;compile->compile",
		ba % "test->test;compile->compile;it->it")
 .configs(IntegrationTest)

// This project validates OPAL's implemented architecture and
// contains overall integration tests; hence
// it is not a "project" in the classical sense!
lazy val Validate = Project(
		id = "OPAL-Validate",
		base = file("DEVELOPING_OPAL/validate"),
		settings = buildSettings ++ Seq(
      publishArtifact := false,
      name := "OPAL-Validate",
      scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Validate"),
      scalacOptions in (Compile, console) := Seq("-deprecation")
    )
).dependsOn(
		DeveloperTools % "compile->compile;test->test;it->it;it->test")
 .configs(IntegrationTest)

lazy val demos = Project(
		id = "Demos",
		base = file("OPAL/demos"),
		settings = buildSettings ++ Seq(publishArtifact := false) ++
		Seq(
			name := "Demos",
			scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Demos"),
			scalacOptions in (Compile, console) := Seq("-deprecation"),
			unmanagedSourceDirectories in Compile :=  (javaSource in Compile).value :: (scalaSource in Compile).value :: Nil,
			fork in run := true
		)
).dependsOn(av,ba)
 .configs(IntegrationTest)

/*****************************************************************************
 *
 * PROJECTS BELONGING TO THE OPAL ECOSYSTEM
 *
 */

lazy val incubation = Project(
		id = "Incubation",
		base = file("OPAL/incubation"),
		settings = buildSettings ++ Seq(
      name := "Incubation",
      // INCUBATION CODE IS NEVER EVEN CONSIDERED TO BE ALPHA QUALITY
      version := "ALWAYS-SNAPSHOT",
      scalacOptions in (Compile, doc) := Opts.doc.title("Incubation"),
      scalacOptions in (Compile, console) := Seq("-deprecation"),
      fork in run := true,
      publishArtifact := false
    )
).dependsOn(
		av % "it->it;it->test;test->test;compile->compile",
		ba % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

 /*****************************************************************************
  *
  * TASKS, etc
  *
  */

// To run the task: OPAL/publish::generateSite or compile:generateSite
val generateSite = taskKey[File]("creates the OPAL website") in Compile
generateSite := {
	lazy val disassemblerJar = (assembly in da).value
	val runUnidoc = (unidoc in Compile).value

    SiteGeneration.generateSite(
		sourceDirectory.value,
		resourceManaged.value,
		streams.value,
		disassemblerJar
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
