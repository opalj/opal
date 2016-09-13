name := "Demos"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Demos")
scalacOptions in (Compile, console) := Seq("-deprecation")

fork in run := true
