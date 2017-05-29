name := "Bytecode Disassembler"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Disassembler")
scalacOptions in (Compile, console) := Seq("-deprecation")

//[currently we can only use an unversioned version] assemblyJarName in assembly := "OPALBytecodeDisassembler.jar-" + version.value
assemblyJarName in assembly := "OPALDisassembler.jar"
mainClass in assembly := Some("org.opalj.da.Disassembler")
