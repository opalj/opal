/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package test
package fixtures
package dynamicConstants

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.bi.ACC_FINAL
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_STATIC
import org.opalj.bi.ACC_SUPER
import org.opalj.bi.Java11MajorVersion
import org.opalj.da.BootstrapArgument
import org.opalj.da.BootstrapMethod
import org.opalj.da.BootstrapMethods_attribute
import org.opalj.da.ClassFile
import org.opalj.da.Code
import org.opalj.da.Code_attribute
import org.opalj.da.CONSTANT_Class_info
import org.opalj.da.CONSTANT_Dynamic_info
import org.opalj.da.CONSTANT_Integer_info
import org.opalj.da.CONSTANT_MethodHandle_info
import org.opalj.da.CONSTANT_Methodref_info
import org.opalj.da.CONSTANT_NameAndType_info
import org.opalj.da.Constant_Pool_Entry
import org.opalj.da.CONSTANT_Utf8
import org.opalj.da.ConstantValue_attribute
import org.opalj.da.Field_Info
import org.opalj.da.Method_Info
import org.opalj.bc.Assembler

import scala.collection.immutable.ArraySeq

/**
 * Creates a fixture to test loading of dynamic constants.
 *
 * @note This is the source for the dynamic_constants.jar in bi/test/resources/classfiles
 *
 * @author Dominik Helm
 */
@RunWith(classOf[JUnitRunner])
class DynamicConstantsCreationTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

    behavior of "the Bytecode Infrastructure for dynamic constants"

    it should "be able to assemble a class file containing dynamic constants" in {
        Assembler(
            ClassFile(
                Array[Constant_Pool_Entry](
                    /*   0 */ null,
                    /*   1 */ CONSTANT_Utf8("Test"),
                    /*   2 */ CONSTANT_Class_info(1),
                    /*   3 */ CONSTANT_Utf8("java/lang/Object"),
                    /*   4 */ CONSTANT_Class_info(3),
                    /*   5 */ CONSTANT_Utf8("instanceField"),
                    /*   6 */ CONSTANT_Utf8("LTest;"),
                    /*   7 */ CONSTANT_Utf8("singletonField"),
                    /*   8 */ CONSTANT_Utf8("staticField"),
                    /*   9 */ CONSTANT_Utf8("I"),
                    /*  10 */ CONSTANT_Utf8("ConstantValue"),
                    /*  11 */ CONSTANT_Integer_info(1337),
                    /*  12 */ CONSTANT_Utf8("<init>"),
                    /*  13 */ CONSTANT_Utf8("()V"),
                    /*  14 */ CONSTANT_NameAndType_info(12, 13),
                    /*  15 */ CONSTANT_Methodref_info(4, 14),
                    /*  16 */ CONSTANT_Utf8("Code"),
                    /*  17 */ CONSTANT_Utf8("BootstrapMethods"),
                    /*  18 */ CONSTANT_Utf8("java/lang/invoke/ConstantBootstraps"),
                    /*  19 */ CONSTANT_Class_info(18),
                    /*  20 */ CONSTANT_Utf8("arrayVarHandle"),
                    /*  21 */ CONSTANT_Utf8("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;"),
                    /*  22 */ CONSTANT_NameAndType_info(20, 21),
                    /*  23 */ CONSTANT_Methodref_info(19, 22),
                    /*  24 */ CONSTANT_MethodHandle_info(6, 23),
                    /*  25 */ CONSTANT_Utf8("[Ljava/lang/String;"),
                    /*  26 */ CONSTANT_Class_info(25),
                    /*  27 */ CONSTANT_Utf8("Ljava/lang/invoke/VarHandle;"),
                    /*  28 */ CONSTANT_NameAndType_info(20, 27),
                    /*  29 */ CONSTANT_Dynamic_info(0, 28),
                    /*  30 */ CONSTANT_Utf8("()Ljava/lang/invoke/VarHandle;"),
                    /*  31 */ CONSTANT_Utf8("enumConstant"),
                    /*  32 */ CONSTANT_Utf8("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Enum;"),
                    /*  33 */ CONSTANT_NameAndType_info(31, 32),
                    /*  34 */ CONSTANT_Methodref_info(19, 33),
                    /*  35 */ CONSTANT_MethodHandle_info(6, 34),
                    /*  36 */ CONSTANT_Utf8("DOWN"),
                    /*  37 */ CONSTANT_Utf8("Ljava/math/RoundingMode;"),
                    /*  38 */ CONSTANT_NameAndType_info(36, 37),
                    /*  39 */ CONSTANT_Dynamic_info(1, 38),
                    /*  40 */ CONSTANT_Utf8("()Ljava/math/RoundingMode;"),
                    /*  41 */ CONSTANT_Utf8("explicitCast"),
                    /*  42 */ CONSTANT_Utf8("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;"),
                    /*  43 */ CONSTANT_NameAndType_info(41, 42),
                    /*  44 */ CONSTANT_Methodref_info(19, 43),
                    /*  45 */ CONSTANT_MethodHandle_info(6, 44),
                    /*  46 */ CONSTANT_Utf8("Ljava/lang/Object;"),
                    /*  47 */ CONSTANT_NameAndType_info(41, 46),
                    /*  48 */ CONSTANT_Dynamic_info(2, 47),
                    /*  49 */ CONSTANT_Utf8("()Ljava/lang/Object;"),
                    /*  50 */ CONSTANT_Utf8("fieldVarHandle"),
                    /*  51 */ CONSTANT_Utf8("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;"),
                    /*  52 */ CONSTANT_NameAndType_info(50, 51),
                    /*  53 */ CONSTANT_Methodref_info(19, 52),
                    /*  54 */ CONSTANT_MethodHandle_info(6, 53),
                    /*  55 */ CONSTANT_NameAndType_info(5, 27),
                    /*  56 */ CONSTANT_Dynamic_info(3, 55),
                    /*  57 */ CONSTANT_Utf8("getStaticFinal"),
                    /*  58 */ CONSTANT_Utf8("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;"),
                    /*  59 */ CONSTANT_NameAndType_info(57, 58),
                    /*  60 */ CONSTANT_Methodref_info(19, 59),
                    /*  61 */ CONSTANT_MethodHandle_info(6, 60),
                    /*  62 */ CONSTANT_NameAndType_info(7, 6),
                    /*  63 */ CONSTANT_Dynamic_info(4, 62),
                    /*  64 */ CONSTANT_Utf8("getStaticFinal1"),
                    /*  65 */ CONSTANT_Utf8("()LTest;"),
                    /*  66 */ CONSTANT_Utf8("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/Object;"),
                    /*  67 */ CONSTANT_NameAndType_info(57, 66),
                    /*  68 */ CONSTANT_Methodref_info(19, 67),
                    /*  69 */ CONSTANT_MethodHandle_info(6, 68),
                    /*  70 */ CONSTANT_NameAndType_info(8, 9),
                    /*  71 */ CONSTANT_Dynamic_info(5, 70),
                    /*  72 */ CONSTANT_Utf8("getStaticFinal2"),
                    /*  73 */ CONSTANT_Utf8("()I"),
                    /*  74 */ CONSTANT_Utf8("invoke"),
                    /*  75 */ CONSTANT_Utf8("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/Object;"),
                    /*  76 */ CONSTANT_NameAndType_info(74, 75),
                    /*  77 */ CONSTANT_Methodref_info(19, 76),
                    /*  78 */ CONSTANT_MethodHandle_info(6, 77),
                    /*  79 */ CONSTANT_Utf8("java/lang/Math"),
                    /*  80 */ CONSTANT_Class_info(79),
                    /*  81 */ CONSTANT_Utf8("max"),
                    /*  82 */ CONSTANT_Utf8("(II)I"),
                    /*  83 */ CONSTANT_NameAndType_info(81, 82),
                    /*  84 */ CONSTANT_Methodref_info(80, 83),
                    /*  85 */ CONSTANT_MethodHandle_info(6, 84),
                    /*  86 */ CONSTANT_Integer_info(42),
                    /*  87 */ CONSTANT_NameAndType_info(74, 9),
                    /*  88 */ CONSTANT_Dynamic_info(6, 87),
                    /*  89 */ CONSTANT_Dynamic_info(7, 87),
                    /*  90 */ CONSTANT_Utf8("nestedConstants1"),
                    /*  91 */ CONSTANT_Utf8("Ljava/lang/Integer;"),
                    /*  92 */ CONSTANT_NameAndType_info(74, 91),
                    /*  93 */ CONSTANT_Dynamic_info(7, 92),
                    /*  94 */ CONSTANT_Utf8("J"),
                    /*  95 */ CONSTANT_NameAndType_info(41, 94),
                    /*  96 */ CONSTANT_Dynamic_info(8, 95),
                    /*  97 */ CONSTANT_Utf8("nestedConstants2"),
                    /*  98 */ CONSTANT_Utf8("()J"),
                    /*  99 */ CONSTANT_Utf8("nullConstant"),
                    /* 100 */ CONSTANT_NameAndType_info(99, 58),
                    /* 101 */ CONSTANT_Methodref_info(19, 100),
                    /* 102 */ CONSTANT_MethodHandle_info(6, 101),
                    /* 103 */ CONSTANT_NameAndType_info(99, 6),
                    /* 104 */ CONSTANT_Dynamic_info(9, 103),
                    /* 105 */ CONSTANT_Utf8("primitiveClass"),
                    /* 106 */ CONSTANT_Utf8("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Class;"),
                    /* 107 */ CONSTANT_NameAndType_info(105, 106),
                    /* 108 */ CONSTANT_Methodref_info(19, 107),
                    /* 109 */ CONSTANT_MethodHandle_info(6, 108),
                    /* 110 */ CONSTANT_Utf8("Ljava/lang/Class;"),
                    /* 111 */ CONSTANT_NameAndType_info(9, 110),
                    /* 112 */ CONSTANT_Dynamic_info(10, 111),
                    /* 113 */ CONSTANT_Utf8("()Ljava/lang/Class;"),
                    /* 114 */ CONSTANT_Utf8("staticFieldVarHandle"),
                    /* 115 */ CONSTANT_NameAndType_info(114, 51),
                    /* 116 */ CONSTANT_Methodref_info(19, 115),
                    /* 117 */ CONSTANT_MethodHandle_info(6, 116),
                    /* 118 */ CONSTANT_NameAndType_info(7, 27),
                    /* 119 */ CONSTANT_Dynamic_info(11, 118)
                ),
                minor_version = 0,
                major_version = Java11MajorVersion,
                access_flags = ACC_PUBLIC.mask | ACC_SUPER.mask,
                this_class = 2, /*Test*/
                super_class = 4, /*extends java.lang.Object*/
                // Interfaces.empty,
                fields = ArraySeq(
                    Field_Info(
                        access_flags = ACC_PUBLIC.mask,
                        name_index = 5, /*instanceField*/
                        descriptor_index = 6 /*Test*/
                    ),
                    Field_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask | ACC_FINAL.mask,
                        name_index = 7, /*singletonField*/
                        descriptor_index = 6 /*Test*/
                    ),
                    Field_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask | ACC_FINAL.mask,
                        name_index = 8, /*staticField*/
                        descriptor_index = 9, /*I*/
                        ArraySeq(ConstantValue_attribute(10, 11 /*1337*/ ))
                    )
                ),
                methods = ArraySeq(
                    Method_Info(
                        access_flags = ACC_PRIVATE.mask,
                        name_index = 12, /*<init>*/
                        descriptor_index = 13, /*()V*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 1,
                            code = new Code(Array[Byte](
                                42, // aload_0
                                (0xff & 183).toByte, // invokespecial
                                0, //
                                15, // java/lang/Object.<init>()V
                                (0xff & 177).toByte // return
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 20, /*arrayVarHandle*/
                        descriptor_index = 30, /*()Ljava/lang/invoke/VarHandle;*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, //
                                29, // ConstantBootstraps.arrayVarHandle(String[].class)
                                (0xff & 176).toByte // areturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 31, /*enumConstant*/
                        descriptor_index = 40, /*()Ljava/math/RoundingMode;*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, //
                                39, // ConstantBootstraps.enumConstant(RoundingMode.DOWN)
                                (0xff & 176).toByte // areturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 41, /*explicitCast*/
                        descriptor_index = 49, /*()Ljava/lang/Object;*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, //
                                48, // ConstantBootstraps.explicitCast(Object.class, Test.class)
                                (0xff & 176).toByte // areturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 50, /*fieldVarHandle*/
                        descriptor_index = 30, /*()Ljava/lang/invoke/VarHandle;*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, //
                                56, // ConstantBootstraps.fieldVarHandle(Test.instanceField)
                                (0xff & 176).toByte // areturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 64, /*getStaticFinal1*/
                        descriptor_index = 65, /*()LTest;*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, //
                                63, // ConstantBootstraps.getStaticFinal(Test.instanceField)
                                (0xff & 176).toByte // areturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 72, /*getStaticFinal2*/
                        descriptor_index = 73, /*()I*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, //
                                71, // ConstantBootstraps.getStaticFinal(Test.staticField)
                                (0xff & 172).toByte // ireturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 74, /*invoke*/
                        descriptor_index = 73, /*()I*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, //
                                88, // ConstantBootstraps.invoke(Math.max(42, 1137))
                                (0xff & 172).toByte // ireturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 90, /*nestedConstants1*/
                        descriptor_index = 73, /*()I*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, // ConstantBootstraps.invoke(42,
                                89, //   ConstantBootstraps.getStaticFinal(staticField))
                                (0xff & 172).toByte // ireturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 97, /*nestedConstants2*/
                        descriptor_index = 98, /*()J*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 2,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                20, // ldc2_w
                                0, // ConstantBootstraps.explicitCast(long.class, ConstantBootstraps.invoke(
                                96, //   42, ConstantBootstraps.getStaticFinal(staticField)))
                                (0xff & 173).toByte // lreturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 99, /*nullConstant*/
                        descriptor_index = 65, /*()LTest;*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, //
                                104, // ConstantBootstraps.nullConstant(Test.class)
                                (0xff & 176).toByte // areturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 105, /*primitiveClass*/
                        descriptor_index = 113, /*()Ljava/lang/Class;*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, //
                                112, // ConstantBootstraps.primitiveClass("I")
                                (0xff & 176).toByte // areturn
                            ))
                        ))
                    ),
                    Method_Info(
                        access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                        name_index = 114, /*staticFieldVarHandle*/
                        descriptor_index = 30, /*()Ljava/lang/invoke/VarHandle;*/
                        attributes = ArraySeq(Code_attribute(
                            attribute_name_index = 16,
                            max_stack = 1,
                            max_locals = 0,
                            code = new Code(Array[Byte](
                                19, // ldc_w
                                0, //
                                119, // ConstantBootstraps.staticFieldVarHandle(Test.singletonField)
                                (0xff & 176).toByte // areturn
                            ))
                        ))
                    ),
                ),
                attributes = ArraySeq(
                    BootstrapMethods_attribute(17, ArraySeq(
                        BootstrapMethod( // 0
                            24, /*ConstantBootstraps.arrayVarHandle*/
                            ArraySeq(BootstrapArgument(26 /*[Ljava/lang/String;*/ ))
                        ),
                        BootstrapMethod( // 1
                            35, /*ConstantBootstraps.enumConstant*/
                            Seq.empty
                        ),
                        BootstrapMethod( // 2
                            45, /*ConstantBootstraps.explicitClast*/
                            ArraySeq(BootstrapArgument(2) /*Test.class*/ )
                        ),
                        BootstrapMethod( // 3
                            54, /*ConstantBootstraps.fieldVarHandle*/
                            ArraySeq(
                                BootstrapArgument(2), /*Test*/
                                BootstrapArgument(2) /*Test*/
                            )
                        ),
                        BootstrapMethod( // 4
                            61, /*ConstantBootstraps.getStaticFinal(Lookup,String,Class)*/
                            Seq.empty
                        ),
                        BootstrapMethod( // 5
                            69, /*ConstantBootstraps.getStaticFinal(Lookup,String,Class,Class)*/
                            ArraySeq(BootstrapArgument(2) /*Test*/ )
                        ),
                        BootstrapMethod( // 6
                            78, /*ConstantBootstraps.invoke*/
                            ArraySeq(
                                BootstrapArgument(85), /*Math.max*/
                                BootstrapArgument(86), /*42*/
                                BootstrapArgument(11) /*getStaticFinal(Test.staticField)*/
                            )
                        ),
                        BootstrapMethod( // 7
                            78, /*ConstantBootstraps.invoke*/
                            ArraySeq(
                                BootstrapArgument(85), /*Math.max*/
                                BootstrapArgument(86), /*42*/
                                BootstrapArgument(71) /*1337*/
                            )
                        ),
                        BootstrapMethod( // 8
                            45, /*ConstantBootstraps.explicitCast*/
                            ArraySeq(BootstrapArgument(93) /*invoke(Math.max(42,getStaticFinal(Test.staticField))*/ )
                        ),
                        BootstrapMethod( // 9
                            102, /*ConstantBootstraps.nullConstant*/
                            Seq.empty
                        ),
                        BootstrapMethod( // 10
                            109, /*ConstantBootstraps.primitiveClass*/
                            Seq.empty
                        ),
                        BootstrapMethod( // 11
                            117, /*ConstantBootstraps.staticFieldVarHandle*/
                            ArraySeq(
                                BootstrapArgument(2) /*Test*/ ,
                                BootstrapArgument(2) /*Test*/
                            )
                        )
                    ))
                )
            )
        )

        // You can recreate the class file using the following code
        //val cf = Assembler(...)
        //println("Created class file: "+Files.write(Paths.get("../Test.class"), cf).toRealPath())
    }
}
