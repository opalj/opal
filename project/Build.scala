import sbt._
import Keys._

import sbtassembly.Plugin.AssemblyKeys._

import scoverage.ScoverageSbtPlugin

import com.typesafe.sbteclipse.plugin.EclipsePlugin._

object OPALBuild extends Build {

	// Default settings without scoverage
	lazy val buildSettings = Defaults.defaultSettings ++
		Seq(Defaults.itSettings : _*) ++
		Seq(EclipseKeys.configurations := Set(Compile, Test, IntegrationTest)) ++
		Seq(libraryDependencies  ++= Seq(
			"junit" % "junit" % "4.11" % "test,it",
			"org.scalatest" %% "scalatest" % "2.2.0" % "test,it"))

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
		opalDeveloperTools,
		VALIDATE,
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
		settings = buildSettings ++ 
			ScoverageSbtPlugin.instrumentSettings
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
	 
	lazy val opalDeveloperTools = Project(
		id = "OpalDeveloperTools",
		base = file("DEVELOPING_OPAL/tools")
	).dependsOn(de % "test->test;compile->compile")
	 .configs(IntegrationTest)

	// This project validates OPAL's implemented architecture; hence
	// it is not a "project" in the classical sense!
	lazy val VALIDATE = Project(
		id = "VALIDATE_OPAL",
		base = file("DEVELOPING_OPAL/validate"),
		settings = buildSettings ++ 
			Seq(publishArtifact := false)
	).dependsOn(
		opalDeveloperTools % "test->test;compile->compile;it->it",
		av % "test->test;compile->compile;it->it")
	 .configs(IntegrationTest)

	lazy val demos = Project(
		id = "Demos",
		base = file("OPAL/demo"),
		settings = buildSettings ++ 
			Seq(publishArtifact := false)
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
			sbtassembly.Plugin.assemblySettings ++
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
		settings = buildSettings ++ Seq(
			publishArtifact := false
		)
	).dependsOn(av % "it->it;it->test;test->test;compile->compile")
	 .configs(IntegrationTest)

}
