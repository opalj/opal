/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package de.tud.cs.st
package bat

/**
 * A constant pool tag identifies the type of a specific entry in the constant pool.
 *
 * @author Michael Eichberg
 */
object ConstantPoolTags extends Enumeration {

    // IN THE FOLLOWING THE ORDER IS AS DEFINED IN THE JVM SPECIFICATION

    final val CONSTANT_Class_ID = 7
    final val CONSTANT_Fieldref_ID = 9
    final val CONSTANT_Methodref_ID = 10
    final val CONSTANT_InterfaceMethodref_ID = 11
    final val CONSTANT_String_ID = 8
    final val CONSTANT_Integer_ID = 3
    final val CONSTANT_Float_ID = 4
    final val CONSTANT_Long_ID = 5
    final val CONSTANT_Double_ID = 6
    final val CONSTANT_NameAndType_ID = 12
    final val CONSTANT_Utf8_ID = 1
    final val CONSTANT_MethodHandle_ID = 15
    final val CONSTANT_MethodType_ID = 16
    final val CONSTANT_InvokeDynamic_ID = 18

    val CONSTANT_Class = Value(CONSTANT_Class_ID, "CONSTANT_Class")
    val CONSTANT_Fieldref = Value(CONSTANT_Fieldref_ID, "CONSTANT_Fieldref")
    val CONSTANT_Methodref = Value(CONSTANT_Methodref_ID, "CONSTANT_Methodref")
    val CONSTANT_InterfaceMethodref = Value(CONSTANT_InterfaceMethodref_ID, "CONSTANT_InterfaceMethodref")
    val CONSTANT_String = Value(CONSTANT_String_ID, "CONSTANT_String")
    val CONSTANT_Integer = Value(CONSTANT_Integer_ID, "CONSTANT_Integer")
    val CONSTANT_Float = Value(CONSTANT_Float_ID, "CONSTANT_Float")
    val CONSTANT_Long = Value(CONSTANT_Long_ID, "CONSTANT_Long")
    val CONSTANT_Double = Value(CONSTANT_Double_ID, "CONSTANT_Double")
    val CONSTANT_NameAndType = Value(CONSTANT_NameAndType_ID, "CONSTANT_NameAndType")
    val CONSTANT_Utf8 = Value(CONSTANT_Utf8_ID, "CONSTANT_Utf8")
    val CONSTANT_MethodHandle = Value(CONSTANT_MethodHandle_ID, "CONSTANT_MethodHandle")
    val CONSTANT_MethodType = Value(CONSTANT_MethodType_ID, "CONSTANT_MethodType")
    val CONSTANT_InvokeDynamic = Value(CONSTANT_InvokeDynamic_ID, "CONSTANT_InvokeDynamic")

}
