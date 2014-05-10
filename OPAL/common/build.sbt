name := "Common"

version := "0.8.1-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL-Common Functionality") 

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
