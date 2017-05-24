name := "Bytecode Disassembler"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Disassembler")
scalacOptions in (Compile, console) := Seq("-deprecation")

mainClass in assembly := Some("org.opalj.da.Disassembler")
assemblyJarName in assembly := "OPALDisassembler.jar-" + version.value
