name := "Architecture Validation"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Architecture Validation") 
scalacOptions in (Compile, console) := Seq("-deprecation")