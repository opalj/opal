name := "Bytecode Disassembler"

version := "0.1.0-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Bytecode Disassembler") 

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.1"

////////////////////// Integration Tests

parallelExecution in IntegrationTest := false

logBuffered in IntegrationTest := false