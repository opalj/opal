import sbt._
import Keys._

import sbtassembly.Plugin.AssemblyKeys._

object BatBuild extends Build {
	lazy val buildSettings =
		Defaults.defaultSettings ++
		sbtassembly.Plugin.assemblySettings ++
		Seq(
			// Override the default version string  ("0.1-SNAPSHOT"),
			// this will be used in the .jar file names
			version := "snapshot",

			organization := "de.tud.cs.st",

			// Enable this to avoid including the Scala runtime into fat .jars,
			// which would reduce the .jar's file size greatly. However, then the
			// user will need the Scala runtime installed in order to run the .jar.
			//assemblyOption in assembly ~= { _.copy(includeScala = false) },

			// Don't run tests as part of the "assembly" command, it's too inconvenient
			test in assembly := {}
		)

	lazy val bat = Project(
		id = "BAT",
		base = file(".")
	) aggregate(
		util, 
		core, 
		ext_dependencies, 
		ext_ai, 
		ext_tools,
		ext_findrealbugs,
		demo,
		incubation)

	lazy val util = Project(
		id = "Core-Util",
		base = file("util")
	)

	lazy val core = Project(
		id = "Core",
		base = file("core")
	) dependsOn(util)

	lazy val ext_dependencies = Project(
		id = "Ext-Dependencies",
		base = file("ext/dependencies")
	) dependsOn(core % "test->test;compile->compile")

	lazy val ext_ai = Project(
		id = "Ext-AbstractInterpretation",
		base = file("ext/ai")
	) dependsOn(core % "test->test;compile->compile", ext_dependencies % "test->compile")

	/* Projects that facilitate the development of analyses. */

	lazy val ext_tools = Project(
		id = "Ext-Tools",
		base = file("ext/tools")
	) dependsOn(core, ext_ai, ext_dependencies)

	lazy val ext_findrealbugs = Project(
		id = "Ext-FindRealBugs",
		base = file("ext/findrealbugs"),
		settings = buildSettings ++ Seq(
			mainClass in assembly := Some("de.tud.cs.st.bat.findrealbugs.FindRealBugsCLI")
		)
	) dependsOn(core % "test->test;compile->compile", ext_ai, ext_dependencies)

	lazy val demo = Project(
		id = "Demo",
		base = file("demo")
	) dependsOn(core, ext_ai, ext_dependencies)

	lazy val incubation = Project(
		id = "Incubation",
		base = file("incubation")
	) dependsOn(core, ext_ai, ext_dependencies)
}
