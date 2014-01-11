name := "BAT"

scalaVersion in ThisBuild := "2.10.3"

//scalacOptions in ThisBuild ++= Seq("-deprecation", "-target:jvm-1.7", "-feature", "-unchecked")
scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature", "-unchecked")

javacOptions in ThisBuild ++= Seq("-encoding", "utf8")

libraryDependencies in ThisBuild += "junit" % "junit" % "4.10" % "test"

libraryDependencies in ThisBuild += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

