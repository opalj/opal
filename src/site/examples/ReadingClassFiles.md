# Reading Class Files
OPAL provides multiple different representations for Java class files to support different use cases. Next, we will discuss how to directly read Java class files and how to create the different representations.

> The following examples expect that you have checked out OPAL, and that you started `sbt` in OPAL's main folder. After that, you have changed to the project `project OPAL-DeveloperTools` and started the `console`. Alternatively to checking out OPAL and building OPAL on your own, you can use our preconfigured [Docker Container](https://hub.docker.com/r/opalj/sbt_scala_opal).

> A growing number of small code snippets that demonstrate various aspects of the API can be found [here](https://bitbucket.org/snippets/delors/).


## Bare Bones 1:1 Representation of Java Class Files

 A bit by bit representation is provided by the bytecode disassembler sub project. In this representation, the constant pool is kept and all other elements (e.g., names of classes, methods and fields, but also constant values etc.) use `int` based references to the constant pool to refer to the respective values. A single class file can trivially be loaded using:

    import java.io.{DataInputStream, FileInputStream}
    import org.opalj.io.process
    import org.opalj.da.ClassFile
    val classFileName = "OPAL/ai/target/scala-2.13/classes/org/opalj/ai/AI.class"
    val cfs : List[ClassFile] =
        process(new DataInputStream(new FileInputStream(classFileName))){ in =>
            org.opalj.da.ClassFileReader.ClassFile(in)
        }

When you use this representation, the returned list always contains a single class file object – which you can get by calling `head` on the returned list.

Using this representation is very, very fast and makes it, e.g.,  easily possible to perform some simple method based slicing or to create an HTML representation (by calling `toXHTML`). The loaded class/transformed class can be written to a class file using `org.opalj.bc.Assembler(<da.ClassFile object>)`


## Object-Oriented Representation of Java Class Files

In most cases, an explicit representation of the constant pool actually complicates the implementation of static analyses. To avoid that you have to deal with the constant pool, OPAL provides a standard object oriented representation that suits many needs. This representation is still stack based and, therefore, the operand stack is still present. It, nevertheless, often strikes a nice balance between performance, memory usage and convenience and, therefore, many analyses that are part of OPAL use this representation. In general, a list of class files is returned to support class file transformations while the class file is loaded. For example – if configured – `invokedynamic` instructions, which are, e.g.,  created by Java compilers when closures are used in Java code, will automatically be transformed to standard (*pre Java 7*) classes and method calls to faciliate subsequent analyses. In this case, a class is generated that will capture the closure's call state and the `invokedynamic` instruction will be replaced by a call to the generated class' factory method; this class serves a similar purpose as the *call-site* object that would be created by the JVM at runtime.

    import java.io.{DataInputStream, FileInputStream}
    import org.opalj.io.process
    import org.opalj.br.ClassFile // "br" instead of "da"
    val classFileName = "OPAL/ai/target/scala-2.13/classes/org/opalj/ai/AI.class"
    val cfs : List[ClassFile] =
        process(new DataInputStream(new FileInputStream(classFileName))){ in =>
            org.opalj.br.reader.Java8Framework.ClassFile(in)
        }

### Representing entire Java Projects
Instead of reading the class files on your own, it is also possible to directly create a `Project` which also directly makes the class hierarchy available and offers many methods related to resolving method calls and the like. A `Project` is usually at the core of implementing static analyses on top of OPAL. To read more about it go [here](Projects.html).

### 3-Address Code Representation
On top/based on the object oriented representation OPAL provides a third representation based on 3-address code/quadruples in single static assignment (SSA) form. This representation is directly made available by a `Project` on-demand. To read more about it go [here](TAC.html).
