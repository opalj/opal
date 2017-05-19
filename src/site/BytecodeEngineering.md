# Engineering Java Bytecode Using OPAL
OPAL is a versatile bytecode engineering framework that offers you mulitple mechanisms to create `.class` files.

## Creating Class Files Using the Bare Bones Representation

### Basics

TODO

### Complex Example

Defining an interface with a (Java 8) default method which calls another static method.

    import java.nio.file.{Files,Paths}
    import org.opalj.bi._
    import org.opalj.da._
    import org.opalj.bc.Assembler
    val cf = ClassFile(
        Array[Constant_Pool_Entry](
            /*  0 */ null, // must be null due to the specification
            /*  1 */ CONSTANT_Class_info(2),
            /*  2 */ CONSTANT_Utf8("mr/MyIntf"),
            /*  3 */ CONSTANT_Class_info(4),
            /*  4 */ CONSTANT_Utf8("java/lang/Object"),
            /*  5 */ CONSTANT_Class_info(6),
            /*  6 */ CONSTANT_Utf8("mr/MyIntf"),
            /*  7 */ CONSTANT_Utf8("m"),
            /*  8 */ CONSTANT_Utf8("()V"),
            /*  9 */ CONSTANT_Utf8("Code"),
            /* 10 */ CONSTANT_String_info(11),
            /* 11 */ CONSTANT_Utf8("MyIntf.m"),
            /* 12 */ CONSTANT_Methodref_info(13, 15),
            /* 13 */ CONSTANT_Class_info(14),
            /* 14 */ CONSTANT_Utf8("mr/Helper"),
            /* 15 */ CONSTANT_NameAndType_info(16, 17),
            /* 16 */ CONSTANT_Utf8("println"),
            /* 17 */ CONSTANT_Utf8("(Ljava/lang/String;)V")
        ),
        minor_version = 0, major_version = 52,
        access_flags = ACC_INTERFACE.mask | ACC_ABSTRACT.mask,
        this_class = 1 /*mr/MyIntf*/ , super_class = 3 /*extends java.lang.Object*/ ,
        // Interfaces.empty,
        // Fields.empty,
        methods = IndexedSeq(
            Method_Info(
                access_flags = ACC_PUBLIC.mask,
                name_index = 7, descriptor_index = 8,
                attributes = IndexedSeq(
                    Code_attribute(
                        attribute_name_index = 9,
                        max_stack = 1, max_locals = 1,
                        code = new Code(Array[Byte](
                                18,         // ldc
                                10,         //  -> #10
                                184.toByte, // invokestatic
                                0,          //  -> Methodref
                                12,         //     #12
                                177.toByte  // return
    )   )   )   )   )   )   )

    val assembledMyIntf = Assembler(cf)
    val assembledMyIntfPath = Paths.get("MyIntf.class")
    println("Created class file: "+Files.write(assembledMyIntfPath, assembledMyIntf).toAbsolutePath)

### Summary
Using this representation is primarily useful for performing simple method and field filterings of existing class files, or for performing simple operations at the method instructions level. Complex transformations or even the creation of new class files requires explicit management/extension of the constant pool and writing the bare bone instructions array. In such cases, using OPAL's default representation – as described next – is highly recommend.

## Creating Class Files Using OPAL's Default Representation

TODO


## Creating Class Files using OPAL eDSL

### Overview

The bytecode assembler framework provides a lightweight eDSL to engineer Java class files. It still requires some understanding of Java bytecode (e.g., the name of the constructor is always `<init>`), but makes the writing of a method's implementation much more trivial since it is no longer necessary to explicitly think about the precise program counters and the precise layout of the code array. Instead of program counters, labels – i.e., Scala `Symbol`s are used – to mark jump targets. Additionally, the code can be annotated and that information is automatically extracted and can later be used.

The following is a first example, where we create a class called `Test` which defines a default constructor.

    import org.opalj.ba._; import org.opalj.br._ ; import org.opalj.br.instructions._;
    val cb = CLASS(
        accessModifiers = PUBLIC SUPER,
        thisType = "Test",
        methods = METHODS(
            METHOD(PUBLIC, "<init>", "()V", CODE(
                'UnUsedLabel1,
                // The following instruction is annotated with some meta information
                // which can later be used; e.g., to check that some static analyis
                // produced an expected result when this instruction is reached.
                ALOAD_0 → "MarkerAnnotation1",
                'UnUsedLabel2,
                INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                RETURN → "MarkerAnnotation2"
            ))
        )
    )

`cb` is a (reusable) builder for a class file. To actually create the ".class" file, the `da.ClassFile` object has to be built and then serialized.

    val (daClassFile,codeAnnotations) = cb.toDA()
    val rawClassFile : Array[Byte] = org.opalj.bc.Assembler(daClassFile)

The raw class file can then be written to the disk or (e.g., for testing purposes) can be passed to a class loader and immediately be instantiated and executed. The code annotations can then be further processed arbitrarily.

> If your class file doesn't define a default constructor, the DSL will automatically add it when required. I.e., the default constructor is created, when you define a regular class - not an interface –, the supertype is specified and no other constructor exists.
