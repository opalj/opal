name := "Bytecode Representation"

version := "0.8.0-SNAPSHOT"

//scalacOptions in Compile := Seq("-deprecation", "-feature", "-unchecked", "-Xlint", "-Xdisable-assertions")
//scalacOptions in Compile += "-Xdisable-assertions"

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Bytecode Representation") 

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"

parallelExecution in IntegrationTest := false

logBuffered in IntegrationTest := false