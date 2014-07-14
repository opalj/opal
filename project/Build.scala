import sbt._
import Keys._

import sbtassembly.Plugin.AssemblyKeys._

import scoverage.ScoverageSbtPlugin.instrumentSettings

import com.typesafe.sbteclipse.plugin.EclipsePlugin._

object OPALBuild extends Build {

	// Default settings without scoverage
	lazy val buildSettings =
		Defaults.defaultSettings

	// Includes scoverage scope
	lazy val opalDefaultSettings =
		Defaults.defaultSettings ++
			instrumentSettings

	lazy val opal = Project(
		id = "OPAL",
		base = file("."),
		settings = buildSettings ++ sbtunidoc.Plugin.unidocSettings ++ Seq(
			publishArtifact := false
  	)
	).
	aggregate(
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
		base = file("OPAL/common")
	)

	lazy val bi = Project(
		id = "BytecodeInfrastructure",
		base = file("OPAL/bi"),
		settings = opalDefaultSettings
	) dependsOn(common)

	lazy val br = Project(
		id = "BytecodeRepresentation",
		base = file("OPAL/br"),
		settings = opalDefaultSettings
	) dependsOn(bi)

	lazy val da = Project(
		id = "BytecodeDisassembler",
		base = file("OPAL/da"),
		settings = opalDefaultSettings
	) dependsOn(bi % "test->test;compile->compile")

	lazy val ai = Project(
		id = "AbstractInterpretationFramework",
		base = file("OPAL/ai"),
		settings = opalDefaultSettings ++
			Seq(Defaults.itSettings : _*) ++
			Seq(EclipseKeys.configurations := Set(Compile, Test, IntegrationTest))
	).dependsOn(br % "test->test;compile->compile;it->test")
	 .configs(IntegrationTest)

	// The project "DependenciesExtractionLibrary" depends on
	// the abstract interpretation framework to be able to
	// resolve calls using MethodHandle/MethodType/"invokedynamic"/...
	lazy val de = Project(
		id = "DependenciesExtractionLibrary",
		base = file("OPAL/de"),
		settings = opalDefaultSettings
	) dependsOn(ai % "test->test;compile->compile")

	lazy val av = Project(
		id = "ArchitectureValidation",
		base = file("OPAL/av"),
		settings = opalDefaultSettings
	) dependsOn(de % "test->test;compile->compile")

	lazy val opalDeveloperTools = Project(
		id = "OpalDeveloperTools",
		base = file("DEVELOPING_OPAL/tools")
	) dependsOn(de % "test->test;compile->compile")

	// This project validates OPAL's implemented architecture; hence
	// it is not a "project" in the classical sense!
	lazy val VALIDATE = Project(
		id = "VALIDATE_OPAL",
		base = file("DEVELOPING_OPAL/validate"),
		settings = opalDefaultSettings ++ Seq(
			publishArtifact := false
		)
	) dependsOn(
		opalDeveloperTools % "test->test;compile->compile",
		av % "test->test;compile->compile")

	lazy val demos = Project(
		id = "Demos",
		base = file("OPAL/demo"),
		settings = opalDefaultSettings ++ Seq(
			publishArtifact := false
		)
	) dependsOn(av)

	/*****************************************************************************
	 *
	 * PROJECTS BELONGING TO THE OPAL ECOSYSTEM
	 *
 	 */

	lazy val findRealBugsAnalyses = Project(
		id = "FindRealBugsAnalyses",
		base = file("OPAL/frb/analyses")
	) dependsOn(ai % "test->test;compile->compile")

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
	) dependsOn(findRealBugsAnalyses % "test->test;compile->compile")

	lazy val incubation = Project(
		id = "Incubation",
		base = file("OPAL/incubation"),
		settings = opalDefaultSettings ++ Seq(
			publishArtifact := false
		)
	) dependsOn(
		av % "test->test;compile->compile")

}
