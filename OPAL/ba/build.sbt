name := "Bytecode Assembler"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Assembler") 
scalacOptions in (Compile, console) := Seq("-deprecation")