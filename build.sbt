import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

import sbtassembly.AssemblyPlugin.autoImport._
import sbtunidoc.ScalaUnidocPlugin

name := "OPAL Library"

// SNAPSHOT
version 		in ThisBuild := "0.9.0-SNAPSHOT"
// NEXT version 		in ThisBuild := "0.8.14"
// RELEASED version 		in ThisBuild := "0.8.13" // MAY 19th, 2017
// RELEASED version 		in ThisBuild := "0.8.12" // April 28th, 2017
// RELEASED version 		in ThisBuild := "0.8.11" // April 14th, 2017
// RELEASED version 		in ThisBuild := "0.8.10"
// RELEASED version 		in ThisBuild := "0.8.9"

organization 	in ThisBuild := "de.opal-project"
homepage 		in ThisBuild := Some(url("http://www.opal-project.de"))
licenses 		in ThisBuild := Seq("BSD-2-Clause" -> url("http://opensource.org/licenses/BSD-2-Clause"))

// [for sbt 0.13.8 onwards] crossPaths in ThisBuild := false

scalaVersion 	in ThisBuild := "2.11.11"
//scalaVersion 	in ThisBuild := "2.12.2"

scalacOptions 	in ThisBuild ++= Seq(
		"-target:jvm-1.8",
		"-deprecation", "-feature", "-unchecked",
		"-Xlint", "-Xfuture", "-Xfatal-warnings",
		"-Ywarn-numeric-widen", "-Ywarn-nullary-unit", "-Ywarn-nullary-override",
		"-Ywarn-unused", "-Ywarn-unused-import", "-Ywarn-dead-code"
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
	"-Xmx3G", "-Xms1024m", "-Xnoclassgc",
	"-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC","-XX:+AggressiveOpts")

addCommandAlias("compileAll","; copyResources ; scalastyle ; test:compile ; test:scalastyle ; it:scalariformFormat ; it:scalastyle ; it:compile ")

addCommandAlias("buildAll","; compileAll ; unidoc ;  publishLocal ")

addCommandAlias("cleanAll","; clean ; test:clean ; it:clean ; cleanFiles ; cleanCache ; cleanLocal ")

addCommandAlias("cleanBuild","; project OPAL ; cleanAll ; buildAll ")

lazy val IntegrationTest = config("it") extend(Test)

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
		PreferencesImporterExporter.loadPreferences((file(formatterPreferencesFile).getPath))
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
		settings = buildSettings
).configs(IntegrationTest)

lazy val bi = Project(
		id = "BytecodeInfrastructure",
		base = file("OPAL/bi"),
		settings = buildSettings
).dependsOn(common % "it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val br = Project(
		id = "BytecodeRepresentation",
		base = file("OPAL/br"),
		settings = buildSettings
).dependsOn(bi % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val da = Project(
		id = "BytecodeDisassembler",
		base = file("OPAL/da"),
		settings = buildSettings
).dependsOn(bi % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val bc = Project(
		id = "BytecodeCreator",
		base = file("OPAL/bc"),
		settings = buildSettings
).dependsOn(da % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val ba = Project(
		id = "BytecodeAssembler",
		base = file("OPAL/ba"),
		settings = buildSettings
).dependsOn(
		bc % "it->it;it->test;test->test;compile->compile",
		br % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val ai = Project(
		id = "AbstractInterpretationFramework",
		base = file("OPAL/ai"),
		settings = buildSettings
).dependsOn(br % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

// The project "DependenciesExtractionLibrary" depends on
// the abstract interpretation framework to be able to
// resolve calls using MethodHandle/MethodType/"invokedynamic"/...
lazy val de = Project(
		id = "DependenciesExtractionLibrary",
		base = file("OPAL/de"),
		settings = buildSettings
).dependsOn(ai % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val bp = Project(
		id = "BugPicker",
		base = file("OPAL/bp"),
		settings = buildSettings
).dependsOn(ai % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val av = Project(
		id = "ArchitectureValidation",
		base = file("OPAL/av"),
		settings = buildSettings
).dependsOn(de % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

lazy val DeveloperTools = Project(
		id = "OPAL-DeveloperTools",
		base = file("DEVELOPING_OPAL/tools"),
		settings = buildSettings
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
		settings = buildSettings ++ Seq(publishArtifact := false)
).dependsOn(
		DeveloperTools % "compile->compile;test->test;it->it;it->test")
 .configs(IntegrationTest)

lazy val demos = Project(
		id = "Demos",
		base = file("OPAL/demos"),
		settings = buildSettings ++ Seq(publishArtifact := false)
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
		settings = buildSettings ++ Seq(publishArtifact := false)
).dependsOn(
		av % "it->it;it->test;test->test;compile->compile",
		ba % "it->it;it->test;test->test;compile->compile")
 .configs(IntegrationTest)

 /*****************************************************************************
  *
  * TASKS, etc
  *
  */

// To run the task: OPAL/publish::generateSite
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

publishTo in ThisBuild := {
	MavenPublishing.publishTo(isSnapshot.value)
}

pomExtra in ThisBuild := MavenPublishing.pomNodeSeq()
