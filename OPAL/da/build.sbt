name := "Bytecode Disassembler"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Disassembler") 
scalacOptions in (Compile, console) := Seq("-deprecation")
