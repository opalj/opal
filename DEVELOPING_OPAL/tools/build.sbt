name := "OPAL-Developer Tools"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Developer Tools") 
scalacOptions in (Compile, console) := Seq("-deprecation")

fork in run := true