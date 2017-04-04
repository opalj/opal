# Reading Class Files

OPAL provides multiple different representations for Java class files to support different use cases. A bit by bit representation is provided by the bytecode disassembler sub project. In this representation, the constant pool is kept and all other elements (e.g., names of classes, methods and fields, but also constant values etc.) use `int` based references to the constant pool to refer to the respective values. A single class file can trivially be loaded using:

    import java.io.{DataInputStream, FileInputStream}
    import org.opalj.io.process
    import org.opalj.da.ClassFile
    val cfs : List[ClassFile] =
        process(new DataInputStream(new FileInputStream("some class file"))){ in =>
            org.opalj.da.ClassFileReader.ClassFile(in)
        }

A list of class files is returned to support class file transformations while the class file is loaded. For example, the higher-level bytecode representation which replaces indirect int based references to the constant pool by direct object references to the respective values, will transform `invokedynamic` instructions which are created by Java compilers when closures are used in Java code. In this case, code is generated (i.e. complete classes) that will capture the closures call state and the `invokedynamic` instruction will be replaced by a call to the generated class' factory method.
