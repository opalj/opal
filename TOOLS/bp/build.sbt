import AssemblyKeys._

name := "BugPicker"

version := "1.0.0"

scalaVersion := "2.11.2"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - BugPicker")

libraryDependencies += "org.scalafx"  %% "scalafx"   % "1.0.0-R8"

jfxSettings

JFX.addJfxrtToClasspath := true

JFX.mainClass := Some("org.opalj.bugpicker.BugPicker")

assemblySettings

jarName in assembly := "bugpicker.jar"

test in assembly := {}

mainClass in assembly := Some("org.opalj.bugpicker.BugPicker")

resourceGenerators in Compile += Def.task {
	val versionFile = new File(baseDirectory.value, "target/scala-2.11/classes/org/opalj/bugpicker/version.txt")
	versionFile.getParentFile.mkdirs()
	val writer = new java.io.PrintWriter(versionFile, "UTF-8")
	writer.append((version in Compile).value).println()
	writer.close()
	Seq(versionFile)
}.taskValue
