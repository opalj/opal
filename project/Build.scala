/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
import sbt._
import Keys._

import sbtassembly.AssemblyPlugin.autoImport._

import com.typesafe.sbteclipse.plugin.EclipsePlugin._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

object OPALBuild extends Build {

	// Default settings without scoverage
	lazy val buildSettings = Defaults.coreDefaultSettings ++
		SbtScalariform.scalariformSettingsWithIt ++
		Seq(ScalariformKeys.preferences <<= baseDirectory.apply(getScalariformPreferences)) ++
		Seq(Defaults.itSettings : _*) ++
		Seq(EclipseKeys.configurations := Set(Compile, Test, IntegrationTest)) ++
		Seq(libraryDependencies  ++= Seq(
			"junit" % "junit" % "4.12" % "test,it",
			"org.scalatest" %% "scalatest" % "2.2.4" % "test,it"))

	def getScalariformPreferences(dir: File) = PreferencesImporterExporter.loadPreferences(
		(file("Scalariform Formatter Preferences.properties").getPath))

	lazy val opal = Project(
		id = "OPAL",
		base = file("."),
		settings = Defaults.coreDefaultSettings ++
			sbtunidoc.Plugin.unidocSettings ++ 
			Seq(publishArtifact := false)
	).aggregate(
		common,
		bi,
		br,
		ai,
		fpcfa,
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

  	lazy val fpcfa = Project(
  		id = "FixpointComputationsFrameworkAnalyses",
  		base = file("OPAL/fpcfa"),
  		settings = buildSettings
  	).dependsOn(ai % "it->it;it->test;test->test;compile->compile")
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
	).dependsOn(
		de % "test->test;compile->compile",
		fpcfa % "test->test;compile->compile;it->it")
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
	).dependsOn(av,fpcfa)
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
	).dependsOn(fpcfa % "test->test;compile->compile;it->it")
	 .configs(IntegrationTest)

	lazy val findRealBugsCLI = Project(
		id = "FindRealBugsCLI",
		base = file("OPAL/frb/cli"),
		settings =
			buildSettings ++
			Seq (
				test in assembly := {},
				assemblyJarName in assembly := "FindREALBugs-" + version.value+".jar",
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
