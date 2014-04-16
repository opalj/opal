name := "OPAL"

scalaVersion in ThisBuild := "2.10.4"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature", "-unchecked")

javacOptions in ThisBuild ++= Seq("-encoding", "utf8") 

libraryDependencies in ThisBuild += "junit" % "junit" % "4.11" % "test"

libraryDependencies in ThisBuild += "org.scalatest" % "scalatest_2.10" % "2.1.0" % "test"

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
