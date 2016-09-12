name := "Bytecode Creator"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Creator") 
scalacOptions in (Compile, console) := Seq("-deprecation")