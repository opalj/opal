name := "Bytecode Representation"

version := "0.8.0-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Bytecode Representation") 

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"