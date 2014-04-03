name := "OPAL"

scalaVersion in ThisBuild := "2.10.4"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-target:jvm-1.7", "-feature", "-unchecked")

javacOptions in ThisBuild ++= Seq("-encoding", "utf8")

javacOptions in ThisBuild ++= Seq("-source", "1.7")

libraryDependencies in ThisBuild += "junit" % "junit" % "4.11" % "test"

libraryDependencies in ThisBuild += "org.scalatest" % "scalatest_2.10" % "2.1.0" % "test"

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
