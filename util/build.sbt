name := "Util"

version := "0.8.1-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL-Core Utility Functionality") 

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.10.4" 
