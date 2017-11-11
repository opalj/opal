// For information on how to use this plugin, see the accompanying Readme.md document.
name := "sbt-java-fixture-compiler"
version := "1.0"
description := "compiles Java projects using the Eclipse compiler version 4.6.1"
organization := "de.opal-project"
licenses += ("BSC 2-clause", url("https://opensource.org/licenses/BSD-2-Clause"))

scalaVersion := "2.10.6"

sbtPlugin := true

publishMavenStyle := false

scalacOptions in ThisBuild ++= Seq(
    "-deprecation", "-feature", "-unchecked",
    "-Xlint", "-Xfuture", "-Xfatal-warnings",
    "-Ywarn-numeric-widen", "-Ywarn-nullary-unit", "-Ywarn-nullary-override",
    // 2.12.4 enable: "-Ywarn-unused:imports,privates,locals,implicits",
    // 2.12.4 enable: "-Ywarn-infer-any", 
    "-Ywarn-dead-code" , "-Ywarn-inaccessible", "-Ywarn-adapted-args"
)

// Version of Eclipse JDT compiler library needs to stay fixed for use within OPAL!
libraryDependencies ++= Seq("org.eclipse.jdt.core.compiler" % "ecj" % "4.6.1")
