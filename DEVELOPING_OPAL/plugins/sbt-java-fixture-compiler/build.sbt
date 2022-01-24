// For information on how to use this plugin, see the accompanying Readme.md document.
name := "sbt-java-fixture-compiler"
version := "1.0.1"
description := "compiles Java projects using a fixed version of the Eclipse compiler"
organization := "de.opal-project"
licenses += ("BSC 2-clause", url("https://opensource.org/licenses/BSD-2-Clause"))

scalaVersion := "2.12.15"

sbtPlugin := true

publishMavenStyle := false

ThisBuild / resolvers += "Eclipse Staging" at "https://repo.eclipse.org/content/repositories/eclipse-staging/"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Xfuture",
  "-Xfatal-warnings",
  "-Ywarn-numeric-widen",
  "-Ywarn-nullary-unit",
  "-Ywarn-nullary-override",
  // 2.12.4 enable: "-Ywarn-unused:imports,privates,locals,implicits",
  // 2.12.4 enable: "-Ywarn-infer-any",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-adapted-args",
  "-Wconf:cat=unused-nowarn:s" // necessary with sbt > 1.5 https://github.com/sbt/sbt/issues/6398
)

// The version of Eclipse JDT compiler library needs to stay fixed for use within OPAL!
libraryDependencies ++= Seq("org.eclipse.jdt" % "ecj" % "3.28.0.v20211021-2009")
