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
 * Generates multiple interfaces with default methods and abstract
 * methods to test method resolution w.r.t. the selection of the
 * maximally specific method.
 *
 * @see For further details see: `Readme.md`.
 *
 * @author Michael Eichberg
 */
object MaximallySpecificInterfaceMethods extends App {

    final val InterfaceAccessFlags = ACC_INTERFACE.mask | ACC_ABSTRACT.mask

    {
        val s0_1CF = ClassFile(
            Array[Constant_Pool_Entry](
                /*  0 */ null,
                /*  1 */ CONSTANT_Class_info(2),
                /*  2 */ CONSTANT_Utf8("mr/S0_1"),
                /*  3 */ CONSTANT_Class_info(4),
                /*  4 */ CONSTANT_Utf8("java/lang/Object"),
                /*  5 */ CONSTANT_Class_info(6), //  HERE - UNUSED
                /*  6 */ CONSTANT_Utf8("mr/SuperIntf"), // HERE - UNUSED
                /*  7 */ CONSTANT_Utf8("m"),
                /*  8 */ CONSTANT_Utf8("()V"),
                /*  9 */ CONSTANT_Utf8("Code"),
                /* 10 */ CONSTANT_String_info(11),
                /* 11 */ CONSTANT_Utf8("S0_1.m"), // the printed value
                /* 12 */ CONSTANT_Methodref_info(13, 15),
                /* 13 */ CONSTANT_Class_info(14),
                /* 14 */ CONSTANT_Utf8("mr/Helper"),
                /* 15 */ CONSTANT_NameAndType_info(16, 17),
                /* 16 */ CONSTANT_Utf8("println"),
                /* 17 */ CONSTANT_Utf8("(Ljava/lang/String;)V")
            ),
            minor_version = 0, major_version = 52, access_flags = InterfaceAccessFlags,
            this_class = 1, super_class = 3 /*extends java.lang.Object*/ ,
            methods = ArraySeq(Method_Info(
                access_flags = ACC_PUBLIC.mask, name_index = 7, descriptor_index = 8,
                attributes = ArraySeq(Code_attribute(
                    attribute_name_index = 9, max_stack = 1, max_locals = 1,
                    code = new Code(Array[Byte](
                        18, /* ldc*/ 10, /* #10*/
                        (0xff & 184).toByte, /* invokestatic*/ 0, /* -> Methodref */ 12, /* #12 */
                        (0xff & 177).toByte /* return */
                    ))
                ))
            ))
        )
        val assembledS0_1 = Assembler(s0_1CF)
        val assembledS0_1Path = Paths.get("OPAL/bc/src/test/resources/MaximallySpecificInterfaceMethods/mr/S0_1.class")
        val assembledS0_1File = Files.write(assembledS0_1Path, assembledS0_1)
        println("Created class file: "+assembledS0_1File.toAbsolutePath())
    }

    {
        val s0_2CF = ClassFile(
            Array[Constant_Pool_Entry](
                /*  0 */ null,
                /*  1 */ CONSTANT_Class_info(2),
                /*  2 */ CONSTANT_Utf8("mr/S0_2"),
                /*  3 */ CONSTANT_Class_info(4),
                /*  4 */ CONSTANT_Utf8("java/lang/Object"),
                /*  5 */ CONSTANT_Class_info(6), //  HERE - UNUSED
                /*  6 */ CONSTANT_Utf8("mr/SuperIntf"), // HERE - UNUSED
                /*  7 */ CONSTANT_Utf8("m"),
                /*  8 */ CONSTANT_Utf8("()V"),
                /*  9 */ CONSTANT_Utf8("Code"),
                /* 10 */ CONSTANT_String_info(11),
                /* 11 */ CONSTANT_Utf8("S0_2.m"), // the printed value
                /* 12 */ CONSTANT_Methodref_info(13, 15),
                /* 13 */ CONSTANT_Class_info(14),
                /* 14 */ CONSTANT_Utf8("mr/Helper"),
                /* 15 */ CONSTANT_NameAndType_info(16, 17),
                /* 16 */ CONSTANT_Utf8("println"),
                /* 17 */ CONSTANT_Utf8("(Ljava/lang/String;)V")
            ),
            minor_version = 0, major_version = 52, access_flags = InterfaceAccessFlags,
            this_class = 1, super_class = 3 /*extends java.lang.Object*/ ,
            methods = ArraySeq(Method_Info(
                access_flags = ACC_PUBLIC.mask, name_index = 7, descriptor_index = 8,
                attributes = ArraySeq(Code_attribute(
                    attribute_name_index = 9, max_stack = 1, max_locals = 1,
                    code = new Code(Array[Byte](
                        18, /* ldc*/ 10, /* #10*/
                        (0xff & 184).toByte, /* invokestatic*/ 0, /* -> Methodref */ 12, /* #12 */
                        (0xff & 177).toByte /* return */
                    ))
                ))
            ))
        )
        val assembledS0_2 = Assembler(s0_2CF)
        val assembledS0_2Path = Paths.get("OPAL/bc/src/test/resources/MaximallySpecificInterfaceMethods/mr/S0_2.class")
        val assembledS0_2File = Files.write(assembledS0_2Path, assembledS0_2)
        println("Created class file: "+assembledS0_2File.toAbsolutePath())
    }

    {
        val s1_aCF = ClassFile(
            Array[Constant_Pool_Entry](
                /*  0 */ null,
                /*  1 */ CONSTANT_Class_info(2),
                /*  2 */ CONSTANT_Utf8("mr/S1_a"),
                /*  3 */ CONSTANT_Class_info(4),
                /*  4 */ CONSTANT_Utf8("java/lang/Object"),
                /*  5 */ CONSTANT_Class_info(6),
                /*  6 */ CONSTANT_Utf8("mr/S0_1"),
                /*  7 */ CONSTANT_Utf8("m"),
                /*  8 */ CONSTANT_Utf8("()V")
            ),
            minor_version = 0, major_version = 52, access_flags = InterfaceAccessFlags,
            this_class = 1, super_class = 3 /*extends java.lang.Object*/ , interfaces = ArraySeq(5),
            methods = ArraySeq(Method_Info(
                access_flags = ACC_PUBLIC.mask | ACC_ABSTRACT.mask,
                name_index = 7, descriptor_index = 8
            ))
        )
        val assembledS1_a = Assembler(s1_aCF)
        val assembledS1_aPath = Paths.get("OPAL/bc/src/test/resources/MaximallySpecificInterfaceMethods/mr/S1_a.class")
        val assembledS1_aFile = Files.write(assembledS1_aPath, assembledS1_a)
        println("Created class file: "+assembledS1_aFile.toAbsolutePath())
    }

    {
        val s1_cCF = ClassFile(
            Array[Constant_Pool_Entry](
                /*  0 */ null,
                /*  1 */ CONSTANT_Class_info(2),
                /*  2 */ CONSTANT_Utf8("mr/S1_c"),
                /*  3 */ CONSTANT_Class_info(4),
                /*  4 */ CONSTANT_Utf8("java/lang/Object"),
                /*  5 */ CONSTANT_Class_info(6),
                /*  6 */ CONSTANT_Utf8("mr/S0_1"),
                /*  7 */ CONSTANT_Utf8("m"),
                /*  8 */ CONSTANT_Utf8("()V"),
                /*  9 */ CONSTANT_Utf8("Code"),
                /* 10 */ CONSTANT_String_info(11),
                /* 11 */ CONSTANT_Utf8("S1_c.m"), // the printed value
                /* 12 */ CONSTANT_Methodref_info(13, 15),
                /* 13 */ CONSTANT_Class_info(14),
                /* 14 */ CONSTANT_Utf8("mr/Helper"),
                /* 15 */ CONSTANT_NameAndType_info(16, 17),
                /* 16 */ CONSTANT_Utf8("println"),
                /* 17 */ CONSTANT_Utf8("(Ljava/lang/String;)V"),
                /* 18 */ CONSTANT_Class_info(19),
                /* 19 */ CONSTANT_Utf8("mr/S0_2")
            ),
            minor_version = 0, major_version = 52, access_flags = InterfaceAccessFlags,
            this_class = 1, super_class = 3 /*extends java.lang.Object*/ , interfaces = ArraySeq(5, 18),
            methods = ArraySeq(Method_Info(
                access_flags = ACC_PUBLIC.mask, name_index = 7, descriptor_index = 8,
                attributes = ArraySeq(Code_attribute(
                    attribute_name_index = 9, max_stack = 1, max_locals = 1,
                    code = new Code(Array[Byte](
                        18, /* ldc*/ 10, /* #10*/
                        (0xff & 184).toByte, /* invokestatic*/ 0, /* -> Methodref */ 12, /* #12 */
                        (0xff & 177).toByte /* return */
                    ))
                ))
            ))
        )
        val assembledS1_c = Assembler(s1_cCF)
        val assembledS1_cPath = Paths.get("OPAL/bc/src/test/resources/MaximallySpecificInterfaceMethods/mr/S1_c.class")
        val assembledS1_cFile = Files.write(assembledS1_cPath, assembledS1_c)
        println("Created class file: "+assembledS1_cFile.toAbsolutePath())
    }

    {
        val s2_1CF = ClassFile(
            Array[Constant_Pool_Entry](
                /*  0 */ null,
                /*  1 */ CONSTANT_Class_info(2),
                /*  2 */ CONSTANT_Utf8("mr/S2_1"),
                /*  3 */ CONSTANT_Class_info(4),
                /*  4 */ CONSTANT_Utf8("java/lang/Object"),
                /*  5 */ CONSTANT_Class_info(6),
                /*  6 */ CONSTANT_Utf8("mr/S1_a"),
                /*  7 */ CONSTANT_Class_info(8),
                /*  8 */ CONSTANT_Utf8("mr/S1_c")
            ),
            minor_version = 0, major_version = 52, access_flags = InterfaceAccessFlags,
            this_class = 1, super_class = 3 /*extends java.lang.Object*/ , interfaces = ArraySeq(5, 7)
        )
        val assembledS2_1 = Assembler(s2_1CF)
        val assembledS2_1Path = Paths.get("OPAL/bc/src/test/resources/MaximallySpecificInterfaceMethods/mr/S2_1.class")
        val assembledS2_1File = Files.write(assembledS2_1Path, assembledS2_1)
        println("Created class file: "+assembledS2_1File.toAbsolutePath())
    }

    {
        val s2_2CF = ClassFile(
            Array[Constant_Pool_Entry](
                /*  0 */ null,
                /*  1 */ CONSTANT_Class_info(2),
                /*  2 */ CONSTANT_Utf8("mr/S2_2"),
                /*  3 */ CONSTANT_Class_info(4),
                /*  4 */ CONSTANT_Utf8("java/lang/Object"),
                /*  5 */ CONSTANT_Class_info(6),
                /*  6 */ CONSTANT_Utf8("mr/S0_2")
            ),
            minor_version = 0, major_version = 52, access_flags = InterfaceAccessFlags,
            this_class = 1, super_class = 3 /*extends java.lang.Object*/ , interfaces = ArraySeq(5)
        )
        val assembledS2_2 = Assembler(s2_2CF)
        val assembledS2_2Path = Paths.get("OPAL/bc/src/test/resources/MaximallySpecificInterfaceMethods/mr/S2_2.class")
        val assembledS2_2File = Files.write(assembledS2_2Path, assembledS2_2)
        println("Created class file: "+assembledS2_2File.toAbsolutePath())
    }

    {
        val intfCF = ClassFile(
            Array[Constant_Pool_Entry](
                /*  0 */ null,
                /*  1 */ CONSTANT_Class_info(2),
                /*  2 */ CONSTANT_Utf8("mr/Intf"),
                /*  3 */ CONSTANT_Class_info(4),
                /*  4 */ CONSTANT_Utf8("java/lang/Object"),
                /*  5 */ CONSTANT_Class_info(6),
                /*  6 */ CONSTANT_Utf8("mr/S2_1"),
                /*  7 */ CONSTANT_Class_info(8),
                /*  8 */ CONSTANT_Utf8("mr/S2_2")
            ),
            minor_version = 0, major_version = 52, access_flags = InterfaceAccessFlags,
            this_class = 1, super_class = 3 /*extends java.lang.Object*/ , interfaces = ArraySeq(5, 7)
        )
        val assembledIntf = Assembler(intfCF)
        val assembledIntfPath = Paths.get("OPAL/bc/src/test/resources/MaximallySpecificInterfaceMethods/mr/Intf.class")
        val assembledIntfFile = Files.write(assembledIntfPath, assembledIntf)
        println("Created class file: "+assembledIntfFile.toAbsolutePath())
    }

}
