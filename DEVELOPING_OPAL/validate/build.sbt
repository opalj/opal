name := "OPAL-Validate"

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Validate") 
scalacOptions in (Compile, console) := Seq("-deprecation")
