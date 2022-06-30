/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bc

import java.nio.file.Files
import java.nio.file.Paths
import java.io.ByteArrayInputStream
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_SUPER
import org.opalj.bi.ACC_STATIC
import org.opalj.br.MethodTemplate
import org.opalj.da.ClassFile
import org.opalj.da.Method_Info
import org.opalj.da.Constant_Pool_Entry
import org.opalj.da.SourceFile_attribute
import org.opalj.da.CONSTANT_Class_info
import org.opalj.da.CONSTANT_Utf8
import org.opalj.da.CONSTANT_NameAndType_info
import org.opalj.da.CONSTANT_Methodref_info
import org.opalj.da.CONSTANT_Fieldref_info
import org.opalj.da.CONSTANT_String_info
import org.opalj.da.Code_attribute
import org.opalj.da.Code
import org.opalj.br.reader.Java8Framework

import scala.collection.immutable.ArraySeq

/**
 * Demonstrates how to create a "HelloWorld" class and how
 * to interact between br and da.
 * {{{
 * public class Test {
 *  public static void main(String[] args) {
 *      System.out.println("Hello World");
 *  }
 * }
 * }}}
 * {{{
 * public class Test
 * minor version: 0
 * major version: 46
 * flags: ACC_PUBLIC, ACC_SUPER
 * Constant pool:
 * #1 = Class              #2             // Test
 * #2 = Utf8               Test
 * #3 = Class              #4             // java/lang/Object
 * #4 = Utf8               java/lang/Object
 * #5 = Utf8               <init>
 * #6 = Utf8               ()V
 * #7 = Utf8               Code
 * #8 = Methodref          #3.#9          // java/lang/Object."<init>":()V
 * #9 = NameAndType        #5:#6          // "<init>":()V
 * #10 = Utf8               LineNumberTable
 * #11 = Utf8               LocalVariableTable
 * #12 = Utf8               this
 * #13 = Utf8               LTest;
 * #14 = Utf8               main
 * #15 = Utf8               ([Ljava/lang/String;)V
 * #16 = Fieldref           #17.#19        // java/lang/System.out:Ljava/io/PrintStream;
 * #17 = Class              #18            // java/lang/System
 * #18 = Utf8               java/lang/System
 * #19 = NameAndType        #20:#21        // out:Ljava/io/PrintStream;
 * #20 = Utf8               out
 * #21 = Utf8               Ljava/io/PrintStream;
 * #22 = String             #23            // Hello World
 * #23 = Utf8               Hello World
 * #24 = Methodref          #25.#27        // java/io/PrintStream.println:(Ljava/lang/String;)V
 * #25 = Class              #26            // java/io/PrintStream
 * #26 = Utf8               java/io/PrintStream
 * #27 = NameAndType        #28:#29        // println:(Ljava/lang/String;)V
 * #28 = Utf8               println
 * #29 = Utf8               (Ljava/lang/String;)V
 * #30 = Utf8               args
 * #31 = Utf8               [Ljava/lang/String;
 * #32 = Utf8               SourceFile
 * #33 = Utf8               Test.java
 * {
 * public Test();
 * descriptor: ()V
 * flags: ACC_PUBLIC
 * Code:
 * stack=1, locals=1, args_size=1
 * 0: aload_0
 * 1: invokespecial #8                  // Method java/lang/Object."<init>":()V
 * 4: return
 *
 * public static void main(java.lang.String[]);
 * descriptor: ([Ljava/lang/String;)V
 * flags: ACC_PUBLIC, ACC_STATIC
 * Code:
 * stack=2, locals=1, args_size=1
 * 0: getstatic     #16                 // Field java/lang/System.out:Ljava/io/PrintStream;
 * 3: ldc           #22                 // String Hello World
 * 5: invokevirtual #24                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
 * 8: return
 * SourceFile: "Test.java"
 * }}}
 *
 * @author Michael Eichberg
 */
object DAandBR extends App {

    /* ClassFile
        constant_pool: Constant_Pool,
        minor_version: Int,
        major_version: Int,
        access_flags:  Int,
        this_class:    Constant_Pool_Index,
        super_class:   Constant_Pool_Index,
        interfaces:    IndexedSeq[Constant_Pool_Index],
        fields:        Fields,
        methods:       Methods,
        attributes:    Attributes
        */

    val cf = ClassFile(
        Array[Constant_Pool_Entry](
            /*  0 */ null,
            /*  1 */ CONSTANT_Class_info(2),
            /*  2 */ CONSTANT_Utf8("Test"),
            /*  3 */ CONSTANT_Class_info(4),
            /*  4 */ CONSTANT_Utf8("java/lang/Object"),
            /*  5 */ CONSTANT_Utf8("<init>"),
            /*  6 */ CONSTANT_Utf8("()V"),
            /*  7 */ CONSTANT_Utf8("Code"),
            /*  8 */ CONSTANT_Methodref_info(3, 9),
            /*  9 */ CONSTANT_NameAndType_info(5, 6),
            /* 10 */ CONSTANT_Utf8("LineNumberTable"),
            /* 11 */ CONSTANT_Utf8("LocalVariableTable"),
            /* 12 */ CONSTANT_Utf8("this"),
            /* 13 */ CONSTANT_Utf8("LTest;"),
            /* 14 */ CONSTANT_Utf8("main"),
            /* 15 */ CONSTANT_Utf8("([Ljava/lang/String;)V"),
            /* 16 */ CONSTANT_Fieldref_info(17, 19),
            /* 17 */ CONSTANT_Class_info(18),
            /* 18 */ CONSTANT_Utf8("java/lang/System"),
            /* 19 */ CONSTANT_NameAndType_info(20, 21),
            /* 20 */ CONSTANT_Utf8("out"),
            /* 21 */ CONSTANT_Utf8("Ljava/io/PrintStream;"),
            /* 22 */ CONSTANT_String_info(23),
            /* 23 */ CONSTANT_Utf8("Hello World"),
            /* 24 */ CONSTANT_Methodref_info(25, 27),
            /* 25 */ CONSTANT_Class_info(26),
            /* 26 */ CONSTANT_Utf8("java/io/PrintStream"),
            /* 27 */ CONSTANT_NameAndType_info(28, 29),
            /* 28 */ CONSTANT_Utf8("println"),
            /* 29 */ CONSTANT_Utf8("(Ljava/lang/String;)V"),
            /* 30 */ CONSTANT_Utf8("args"),
            /* 31 */ CONSTANT_Utf8("[Ljava/lang/String;"),
            /* 32 */ CONSTANT_Utf8("SourceFile"),
            /* 33 */ CONSTANT_Utf8("Test.java")
        ),
        minor_version = 0,
        major_version = 46,
        access_flags = ACC_PUBLIC.mask | ACC_SUPER.mask,
        this_class = 1 /*Test*/ ,
        super_class = 3 /*extends java.lang.Object*/ ,
        // Interfaces.empty,
        // Fields.empty,
        methods = ArraySeq(
            Method_Info(
                access_flags = ACC_PUBLIC.mask,
                name_index = 5,
                descriptor_index = 6,
                attributes = ArraySeq(
                    Code_attribute(
                        attribute_name_index = 7,
                        max_stack = 1,
                        max_locals = 1,
                        code =
                            new Code(
                                Array[Byte](
                                    42, // aload_0
                                    (0xff & 183).toByte, // invokespecial
                                    0, //                    -> Methodref
                                    8, //                       #8
                                    (0xff & 177).toByte
                                )
                            )
                    )
                )
            ),
            Method_Info(
                access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                name_index = 14,
                descriptor_index = 15,
                attributes = ArraySeq(
                    Code_attribute(
                        attribute_name_index = 7,
                        max_stack = 2,
                        max_locals = 1,
                        code =
                            new Code(
                                Array[Byte](
                                    (0xff & 178).toByte, // getstatic
                                    0,
                                    16,
                                    18, // ldc
                                    22,
                                    (0xff & 182).toByte, // invokevirtual
                                    0,
                                    24,
                                    (0xff & 177).toByte // return
                                )
                            )
                    )
                )
            )
        ),
        attributes = ArraySeq(SourceFile_attribute(32, 33))
    )

    val assembledCF = Assembler(cf)

    val brClassFile = Java8Framework.ClassFile(() => new ByteArrayInputStream(assembledCF)).head
    val newBRMethods =
        brClassFile.methods.
            filter(m => /*due some sophisticated analysis...*/ m.name == "<init>").
            map[MethodTemplate](m => m.copy())
    val newBRClassFile = brClassFile.copy(methods = newBRMethods)

    val newDAClassFile = cf.copy(methods = cf.methods.filter { daM =>
        implicit val cp = cf.constant_pool
        brClassFile.methods.exists { brM =>
            brM.name == daM.name && brM.descriptor.toJVMDescriptor == daM.descriptor
        }
    })

    println("Created class file: "+Files.write(Paths.get("Test.class"), assembledCF).toAbsolutePath())
}
