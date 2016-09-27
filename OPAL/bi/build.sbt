name := "Bytecode Infrastructure"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Infrastructure") 
scalacOptions in (Compile, console) := Seq("-deprecation")

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4"
