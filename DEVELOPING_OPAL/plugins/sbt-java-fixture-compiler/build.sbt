// For information on how to use this plugin, see the accompanying Readme.md document.
scalaVersion := "2.10.6"
version := "1.0"
organization := "de.opal-project"
licenses += ("BSC 2-clause", url("https://opensource.org/licenses/BSD-2-Clause"))

sbtPlugin := true

publishMavenStyle := false

name := "sbt-java-fixture-compiler"
description := "compiles Java projects using the Eclipse compiler version 4.6.1"

// Version of Eclipse JDT compiler library needs to stay fixed for use within OPAL!
libraryDependencies ++= Seq("org.eclipse.jdt.core.compiler" % "ecj" % "4.6.1")
