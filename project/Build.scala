import sbt._
import Keys._

import sbtassembly.Plugin.AssemblyKeys._

object OPALBuild extends Build {
	 
	lazy val buildSettings =
		Defaults.defaultSettings 
	 
	lazy val opal = Project(
		id = "OPAL",
		base = file("."),
		settings = buildSettings ++ sbtunidoc.Plugin.unidocSettings
	).
	aggregate(
		common, 
		bt,
		da,
		ai,
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
	
	lazy val bt = Project(
		id = "BytecodeToolkit",
		base = file("core")
	) dependsOn(common)
	
	lazy val da = Project(
		id = "BytecodeDisassembler",
		base = file("OPAL/da")
	) dependsOn(bt % "test->test;compile->compile")

	lazy val ai = Project(
		id = "AbstractInterpretationFramework",
		base = file("ext/ai")
	) dependsOn(bt % "test->test;compile->compile")

	// The project "DependenciesExtractionLibrary" depends on
	// the abstract interpretation framework to be able to 
	// resolve calls using MethodHandle/MethodType/"invokedynamic"/...
	lazy val de = Project(
		id = "DependenciesExtractionLibrary",
		base = file("ext/dependencies")
	) dependsOn(ai % "test->test;compile->compile")

	lazy val av = Project(
		id = "ArchitectureValidation",
		base = file("av")
	) dependsOn(de % "test->test;compile->compile")

	lazy val opalDeveloperTools = Project(
		id = "OpalDeveloperTools",
		base = file("ext/tools")
	) dependsOn(de % "test->test;compile->compile")

	// This project validates OPAL's implemented architecture; hence
	// it is not a "project" in the classical sense!
	lazy val VALIDATE = Project(
		id = "VALIDATE_OPAL",
		base = file("VALIDATE")
	) dependsOn(
		opalDeveloperTools % "test->test;compile->compile",
		av % "test->test;compile->compile")

	lazy val demos = Project(
		id = "Demos",
		base = file("demo")
	) dependsOn(av)

	/*****************************************************************************
	 *
	 * PROJECTS BELONGING TO THE OPAL ECOSYSTEM 
	 *
 	 */

	lazy val findRealBugsAnalyses = Project(
		id = "FindRealBugsAnalyses",
		base = file("frb/analyses")
	) dependsOn(ai % "test->test;compile->compile")

	lazy val findRealBugsCLI = Project(
		id = "FindRealBugsCLI",
		base = file("frb/cli"),
		settings = 
			buildSettings ++
			sbtassembly.Plugin.assemblySettings ++ 
			Seq (
				test in assembly := {},
				jarName in assembly := "FindREALBugs-" + version.value+".jar",
				mainClass in assembly := Some("de.tud.cs.st.bat.findrealbugs.FindRealBugsCLI")
			)
	) dependsOn(findRealBugsAnalyses % "test->test;compile->compile")

	lazy val incubation = Project(
		id = "Incubation",
		base = file("incubation")
	) dependsOn(av)

}
