// For information on how to use this plugin, see the accompanying Readme.md document.
name := "sbt-java-fixture-compiler"
version := "1.0.1"
description := "compiles Java projects using a fixed version of the Eclipse compiler"
organization := "de.opal-project"
licenses += ("BSC 2-clause", url("https://opensource.org/licenses/BSD-2-Clause"))

scalaVersion := "2.12.6"

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

// The version of Eclipse JDT compiler library needs to stay fixed for use within OPAL!
// The version 4.6.1 which we use contains a bug when compiling
// certain method references that return a value; however, even new(er) versions
// still contain the bug (the stack is not empty, when the method returns.)
// [still buggy] libraryDependencies ++= Seq("com.reubenpeeris" % "org.eclipse.jdt.core.compiler.ecj" % "4.7-201706120950")
libraryDependencies ++= Seq("org.eclipse.jdt.core.compiler" % "ecj" % "4.6.1")
