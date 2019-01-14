import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

import no.vedaadata.sbtjavafx.JavaFXPlugin
import no.vedaadata.sbtjavafx.JavaFXPlugin.JFX

import sbtassembly.AssemblyPlugin.autoImport._

name := "BugPicker"

organization	in ThisBuild := "de.opal-project"
homepage 		in ThisBuild := Some(url("http://www.opal-project.de/"))
licenses 		in ThisBuild := Seq("BSD-2-Clause" -> url("http://opensource.org/licenses/BSD-2-Clause"))
version 		in ThisBuild := "1.3.0-Snapshot"
scalaVersion 	in ThisBuild := "2.12.8"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - BugPicker")

scalacOptions in ThisBuild ++= Seq(
	"-deprecation", "-feature", "-unchecked", "-Xlint", "-Xfuture",
	"-Ywarn-numeric-widen", "-Ywarn-unused", "-Ywarn-unused-import", "-Ywarn-nullary-unit", "-Ywarn-nullary-override", "-Ywarn-dead-code", "-Xfatal-warnings" )

fork in run := true

javaOptions in run += "-Xmx8G" // BETTER: javaOptions in run += "-Xmx16G"
javaOptions in run += "-Xms4G"

mainClass in LocalProject("bp") in Compile := (mainClass in LocalProject("BugPickerUI") in Compile).value
fullClasspath in LocalProject("bp") in Runtime ++= (fullClasspath in LocalProject("BugPickerUI") in Runtime).value

def getScalariformPreferences(dir: File) = PreferencesImporterExporter.loadPreferences(
	(file("./../../Scalariform Formatter Preferences.properties").getPath))

lazy val buildSettings = Defaults.coreDefaultSettings ++
	scalariformItSettings ++
	Seq(ScalariformKeys.preferences := baseDirectory.apply(getScalariformPreferences).value) ++
	Seq(Defaults.itSettings : _*) ++
	Seq(libraryDependencies ++= Dependencies.buildlevel) ++
	Seq(resolvers ++= Seq(
		"Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
	))

val resGenerator = taskKey[Seq[java.io.File]]("write version file")
resGenerator in ui := {
	val versionFile = (baseDirectory in Compile).value / "target" / "scala-2.12" / "classes" / "org" / "opalj" / "bugpicker" / "version.txt"
	versionFile.getParentFile.mkdirs()
	IO.write(versionFile, (version in Compile).value)
	Seq(versionFile)
}

lazy val ui = Project(
	id = "BugPickerUI",
	base = file("ui"),
	settings = buildSettings ++
		JavaFXPlugin.jfxSettings ++
		Seq(
			JFX.mainClass := Option("org.opalj.bugpicker.BugPicker"),
			JFX.addJfxrtToClasspath := true
		) ++
		Seq(mainClass in (Compile, run) := Some("org.opalj.bugpicker.ui.BugPicker")) ++
		Seq(libraryDependencies ++= Dependencies.ui) ++
		Seq(
			resourceGenerators in Compile += resGenerator.taskValue
		)
)
