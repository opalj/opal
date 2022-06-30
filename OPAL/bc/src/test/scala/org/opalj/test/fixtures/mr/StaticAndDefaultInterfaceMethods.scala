/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package test
package fixtures
package mr

import java.nio.file.Files
import java.nio.file.Paths
import org.opalj.bc.Assembler
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_ABSTRACT
import org.opalj.bi.ACC_INTERFACE
import org.opalj.bi.ACC_STATIC
import org.opalj.da.ClassFile
import org.opalj.da.Method_Info
import org.opalj.da.Constant_Pool_Entry
import org.opalj.da.CONSTANT_Class_info
import org.opalj.da.CONSTANT_Utf8
import org.opalj.da.CONSTANT_NameAndType_info
import org.opalj.da.CONSTANT_Methodref_info
import org.opalj.da.CONSTANT_String_info
import org.opalj.da.Code_attribute
import org.opalj.da.Code

import scala.collection.immutable.ArraySeq

/**
 * Generates three interfaces SuperIntf, Intf and SubIntf where Intf inherits from
 * SuperIntf and where Intf defines a static method that has the same
 * name and descriptor as a default method defined SuperIntf.
 *
 * @see For further details see: `Readme.md`.
 *
 * @author Michael Eichberg
 */
object StaticAndDefaultInterfaceMethods extends App {

    val superIntfCF = ClassFile(
        Array[Constant_Pool_Entry](
            /*  0 */ null, // must be null due to the specification
            /*  1 */ CONSTANT_Class_info(2),
            /*  2 */ CONSTANT_Utf8("mr/SuperIntf"),
            /*  3 */ CONSTANT_Class_info(4),
            /*  4 */ CONSTANT_Utf8("java/lang/Object"),
            /*  5 */ CONSTANT_Class_info(6),
            /*  6 */ CONSTANT_Utf8("mr/SuperIntf"),
            /*  7 */ CONSTANT_Utf8("m"),
            /*  8 */ CONSTANT_Utf8("()V"),
            /*  9 */ CONSTANT_Utf8("Code"),
            /* 10 */ CONSTANT_String_info(11),
            /* 11 */ CONSTANT_Utf8("SuperIntf.m"),
            /* 12 */ CONSTANT_Methodref_info(13, 15),
            /* 13 */ CONSTANT_Class_info(14),
            /* 14 */ CONSTANT_Utf8("mr/Helper"),
            /* 15 */ CONSTANT_NameAndType_info(16, 17),
            /* 16 */ CONSTANT_Utf8("println"),
            /* 17 */ CONSTANT_Utf8("(Ljava/lang/String;)V")
        ),
        minor_version = 0, major_version = 52,
        access_flags = ACC_INTERFACE.mask | ACC_ABSTRACT.mask,
        this_class = 1 /*mr/SuperIntf*/ , super_class = 3 /*extends java.lang.Object*/ ,
        // Interfaces.empty,
        // Fields.empty,
        methods = ArraySeq(
            Method_Info(
                access_flags = ACC_PUBLIC.mask,
                name_index = 7, descriptor_index = 8,
                attributes = ArraySeq(
                    Code_attribute(
                        attribute_name_index = 9,
                        max_stack = 1, max_locals = 1,
                        code =
                            new Code(
                                Array[Byte](
                                    18, // ldc
                                    10, // #10
                                    (0xff & 184).toByte, // invokestatic
                                    0, // -> Methodref
                                    12, // #12
                                    (0xff & 177).toByte // return
                                )
                            )
                    )
                )
            )
        )
    )
    val assembledSuperIntf = Assembler(superIntfCF)
    val assembledSuperIntfPath = Paths.get("OPAL/bc/src/test/resources/StaticAndDefaultInterfaceMethods/mr/SuperIntf.class")
    val assembledSuperIntfFile = Files.write(assembledSuperIntfPath, assembledSuperIntf)
    println("Created class file: "+assembledSuperIntfFile.toAbsolutePath())

    val intfCF = ClassFile(
        Array[Constant_Pool_Entry](
            /*  0 */ null,
            /*  1 */ CONSTANT_Class_info(2),
            /*  2 */ CONSTANT_Utf8("mr/Intf"),
            /*  3 */ CONSTANT_Class_info(4),
            /*  4 */ CONSTANT_Utf8("java/lang/Object"),
            /*  5 */ CONSTANT_Class_info(6),
            /*  6 */ CONSTANT_Utf8("mr/SuperIntf"),
            /*  7 */ CONSTANT_Utf8("m"),
            /*  8 */ CONSTANT_Utf8("()V"),
            /*  9 */ CONSTANT_Utf8("Code"),
            /* 10 */ CONSTANT_String_info(11),
            /* 11 */ CONSTANT_Utf8("Intf.m"),
            /* 12 */ CONSTANT_Methodref_info(13, 15),
            /* 13 */ CONSTANT_Class_info(14),
            /* 14 */ CONSTANT_Utf8("mr/Helper"),
            /* 15 */ CONSTANT_NameAndType_info(16, 17),
            /* 16 */ CONSTANT_Utf8("println"),
            /* 17 */ CONSTANT_Utf8("(Ljava/lang/String;)V")
        ),
        minor_version = 0, major_version = 52,
        access_flags = ACC_INTERFACE.mask | ACC_ABSTRACT.mask,
        this_class = 1 /*mr/Intf*/ , super_class = 3 /*extends java.lang.Object*/ ,
        interfaces = ArraySeq(5) /*mr/SuperIntf*/ ,
        // Fields.empty,
        methods = ArraySeq(
            Method_Info(
                access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                name_index = 7, descriptor_index = 8,
                attributes = ArraySeq(
                    Code_attribute(
                        attribute_name_index = 9,
                        max_stack = 1, max_locals = 1,
                        code = new Code(
                            Array[Byte](
                                18, // ldc
                                10, // #10
                                (0xff & 184).toByte, // invokestatic
                                0, // -> Methodref
                                12, //    #12
                                (0xff & 177).toByte // return
                            )
                        )
                    )
                )
            )
        )
    )
    val assembledIntf = Assembler(intfCF)
    val assembledIntfPath = Paths.get("OPAL/bc/src/test/resources/StaticAndDefaultInterfaceMethods/mr/Intf.class")
    val assembledIntfFile = Files.write(assembledIntfPath, assembledIntf)
    println("Created class file: "+assembledIntfFile.toAbsolutePath())

    val subIntfCF = ClassFile(
        Array[Constant_Pool_Entry](
            /*  0 */ null,
            /*  1 */ CONSTANT_Class_info(2),
            /*  2 */ CONSTANT_Utf8("mr/SubIntf"),
            /*  3 */ CONSTANT_Class_info(4),
            /*  4 */ CONSTANT_Utf8("java/lang/Object"),
            /*  5 */ CONSTANT_Class_info(6),
            /*  6 */ CONSTANT_Utf8("mr/Intf")
        ),
        minor_version = 0, major_version = 52,
        access_flags = ACC_INTERFACE.mask | ACC_ABSTRACT.mask,
        this_class = 1 /*mr/SubIntf*/ , super_class = 3 /*extends java.lang.Object*/ ,
        interfaces = ArraySeq(5) //mr/Intf
    )
    val assembledSubIntf = Assembler(subIntfCF)
    val assembledSubIntfPath =
        Paths.get("OPAL/bc/src/test/resources/StaticAndDefaultInterfaceMethods/mr/SubIntf.class")
    val assembledSubIntfFile = Files.write(assembledSubIntfPath, assembledSubIntf)
    println("Created class file: "+assembledSubIntfFile.toAbsolutePath())
}
