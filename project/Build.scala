import sbt._
import Keys._

import sbtassembly.AssemblyPlugin.autoImport._

import scoverage.ScoverageSbtPlugin

import com.typesafe.sbteclipse.plugin.EclipsePlugin._

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

object OPALBuild extends Build {
	// Default settings without scoverage
	lazy val buildSettings = Defaults.defaultSettings ++
		SbtScalariform.scalariformSettingsWithIt ++
		Seq(ScalariformKeys.preferences <<= baseDirectory.apply(getScalariformPreferences)) ++
		Seq(Defaults.itSettings : _*) ++
		Seq(EclipseKeys.configurations := Set(Compile, Test, IntegrationTest)) ++
		Seq(libraryDependencies  ++= Seq(
			"junit" % "junit" % "4.12" % "test,it",
			"org.scalatest" %% "scalatest" % "2.2.1" % "test,it"))

	def getScalariformPreferences(dir: File) = PreferencesImporterExporter.loadPreferences(
		(file("Scalariform Formatter Preferences.properties").getPath))

	lazy val opal = Project(
		id = "OPAL",
		base = file("."),
		settings = Defaults.defaultSettings ++ 
			sbtunidoc.Plugin.unidocSettings ++ 
			Seq(publishArtifact := false)
	).aggregate(
		common,
		bi,
		br,
		ai,
		da,
		de,
		av,
		DeveloperTools,
		Validate,
		demos,
		findRealBugsAnalyses,
		findRealBugsCLI,
		incubation)

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
	).dependsOn(de % "test->test;compile->compile")
	 .configs(IntegrationTest)

	// This project validates OPAL's implemented architecture; hence
	// it is not a "project" in the classical sense!
	lazy val Validate = Project(
		id = "OPAL-Validate",
		base = file("DEVELOPING_OPAL/validate"),
		settings = buildSettings ++ Seq(publishArtifact := false)
	).dependsOn(
		DeveloperTools % "test->test;compile->compile;it->it",
		av % "test->test;compile->compile;it->it")
	 .configs(IntegrationTest)

	lazy val demos = Project(
		id = "Demos",
		base = file("OPAL/demos"),
		settings = buildSettings ++ Seq(publishArtifact := false)
	).dependsOn(av)
	 .configs(IntegrationTest)

	/*****************************************************************************
	 *
	 * PROJECTS BELONGING TO THE OPAL ECOSYSTEM
	 *
 	 */

	lazy val findRealBugsAnalyses = Project(
		id = "FindRealBugsAnalyses",
		base = file("OPAL/frb/analyses"),
		settings = buildSettings
	).dependsOn(ai % "test->test;compile->compile;it->it")
	 .configs(IntegrationTest)

	lazy val findRealBugsCLI = Project(
		id = "FindRealBugsCLI",
		base = file("OPAL/frb/cli"),
		settings =
			buildSettings ++
			Seq (
				test in assembly := {},
				jarName in assembly := "FindREALBugs-" + version.value+".jar",
				mainClass in assembly := Some("org.opalj.frb.cli.FindRealBugsCLI")
			)
	).dependsOn(findRealBugsAnalyses % "test->test;compile->compile;it->it")
	 .configs(IntegrationTest)

	lazy val incubation = Project(
		id = "Incubation",
		base = file("OPAL/incubation"),
		settings = buildSettings ++ Seq(publishArtifact := false)
	).dependsOn(av % "it->it;it->test;test->test;compile->compile")
	 .configs(IntegrationTest)

}
