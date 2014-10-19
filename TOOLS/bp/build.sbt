import AssemblyKeys._

name := "BugPicker"

version := "ALWAYS-SNAPSHOT"

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
