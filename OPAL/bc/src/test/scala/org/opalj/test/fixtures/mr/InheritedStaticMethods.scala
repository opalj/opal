/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package bc

import java.nio.file.Files
import java.nio.file.Paths

import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_STATIC
import org.opalj.bi.ACC_SUPER
import org.opalj.da.ClassFile
import org.opalj.da.Method_Info
import org.opalj.da.Constant_Pool_Entry
import org.opalj.da.CONSTANT_Class_info
import org.opalj.da.CONSTANT_Utf8
import org.opalj.da.CONSTANT_NameAndType_info
import org.opalj.da.CONSTANT_Methodref_info
import org.opalj.da.CONSTANT_InterfaceMethodref_info
import org.opalj.da.CONSTANT_String_info
import org.opalj.da.Code_attribute
import org.opalj.da.Code

/**
 * Generates a main class which invokes a static method defined
 * by a superclass using the declared type of the subclass.
 *
 * @see For further details see: `Readme.md`.
 *
 * @author Michael Eichberg
 */
object InheritedStaticMethods extends App {

    val assembledMainCF = ClassFile(
        Array[Constant_Pool_Entry](
            /*  0 */ null,
            /*  1 */ CONSTANT_Class_info(2),
            /*  2 */ CONSTANT_Utf8("mr/Main"),
            /*  3 */ CONSTANT_Class_info(4),
            /*  4 */ CONSTANT_Utf8("java/lang/Object"),
            /*  5 */ CONSTANT_Utf8("<init>"),
            /*  6 */ CONSTANT_Utf8("()V"),
            /*  7 */ CONSTANT_Utf8("Code"),
            /*  8 */ CONSTANT_Methodref_info(3, 9),
            /*  9 */ CONSTANT_NameAndType_info(5, 6),
            /* 10 */ CONSTANT_InterfaceMethodref_info(16, 27),
            /* 11 */ CONSTANT_Utf8("mr/SubI"),
            /* 12 */ CONSTANT_Utf8("this"),
            /* 13 */ CONSTANT_Utf8("LMain;"),
            /* 14 */ CONSTANT_Utf8("main"),
            /* 15 */ CONSTANT_Utf8("([Ljava/lang/String;)V"),
            /* 16 */ CONSTANT_Class_info(11),
            /* 17 */ CONSTANT_Class_info(18),
            /* 18 */ CONSTANT_Utf8("java/lang/System"),
            /* 19 */ CONSTANT_NameAndType_info(20, 21),
            /* 20 */ CONSTANT_Utf8("out"),
            /* 21 */ CONSTANT_Utf8("Ljava/io/PrintStream;"),
            /* 22 */ CONSTANT_String_info(23),
            /* 23 */ CONSTANT_Utf8("Hello World"),
            /* 24 */ CONSTANT_Methodref_info(25, 27),
            /* 25 */ CONSTANT_Class_info(26),
            /* 26 */ CONSTANT_Utf8("mr/SubX"),
            /* 27 */ CONSTANT_NameAndType_info(28, 29),
            /* 28 */ CONSTANT_Utf8("m"),
            /* 29 */ CONSTANT_Utf8("()V"),
            /* 30 */ CONSTANT_Utf8("args"),
            /* 31 */ CONSTANT_Utf8("[Ljava/lang/String;")
        ),
        minor_version = 0,
        major_version = 52,
        access_flags = ACC_PUBLIC.mask | ACC_SUPER.mask,
        this_class = 1 /*Test*/ ,
        super_class = 3 /*extends java.lang.Object*/ ,
        // Interfaces.empty,
        // Fields.empty,
        methods = IndexedSeq(
            // default constructor
            Method_Info(
                access_flags = ACC_PUBLIC.mask,
                name_index = 5, descriptor_index = 6,
                attributes = IndexedSeq(
                    Code_attribute(
                        attribute_name_index = 7, max_stack = 1, max_locals = 1,
                        code =
                            new Code(
                                Array[Byte](42, (0xff & 183).toByte, 0, 8, (0xff & 177).toByte)
                            )
                    )
                )
            ),
            Method_Info(
                access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                name_index = 14,
                descriptor_index = 15,
                attributes = IndexedSeq(
                    Code_attribute(
                        attribute_name_index = 7, max_stack = 0, max_locals = 1,
                        code =
                            new Code(
                                Array[Byte](
                                    (0xff & 184).toByte, 0, 24, // invokestatic #24 (SubX.m())
                                    (0xff & 184).toByte, 0, 10, // invokestatic #24 (SubIntf.m())
                                    (0xff & 177).toByte // return
                                )
                            )
                    )
                )
            )
        )
    )
    val assembledMain = Assembler(assembledMainCF)
    val assembledMainPath = Paths.get("OPAL/bc/src/test/resources/InheritedStaticInterfaceMethods/mr/Main.class")
    val assembledMainFile = Files.write(assembledMainPath, assembledMain)
    println("Created class file: "+assembledMainFile.toAbsolutePath())

}
