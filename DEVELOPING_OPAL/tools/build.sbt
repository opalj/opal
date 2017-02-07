name := "OPAL-Developer Tools"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Developer Tools") 
scalacOptions in (Compile, console) := Seq("-deprecation")

libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.102-R11"

fork in run := true