# Java Bytecode Explorer

The Java bytecode explorer is a small JavaFX application that can be used to analyze the bytecode of an application.

# Java Bytecode Disassembler

OPAL has its own Java Bytecode Disassembler which generates an (x)HTML document describing the Java Bytecode. The bytecode Disassembler can be run programmatically as shown next:

    val classFile : org.opalj.da.ClassFile = ...;
    val html = classFile.toXHTML()
    // println(html)
    // org.opalj.io.writeAndOpen(classFile.toXHTML().toString, "ClassFile", ".html")

Alternatively, a [pre-build jar](artifacts/OPALDisassembler.jar) is available which you can use when you want to call the bytecode assembler from the command line. Just run it to get some help:

    java -jar <OPALDisassembler.jar> <JAR Archive> <ClassFiles*>
