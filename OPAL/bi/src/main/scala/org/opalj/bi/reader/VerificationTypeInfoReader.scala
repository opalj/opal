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
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 *
 * @author Michael Eichberg
 */
trait VerificationTypeInfoReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type VerificationTypeInfo

    def TopVariableInfo(): VerificationTypeInfo

    def IntegerVariableInfo(): VerificationTypeInfo

    def FloatVariableInfo(): VerificationTypeInfo

    def LongVariableInfo(): VerificationTypeInfo

    def DoubleVariableInfo(): VerificationTypeInfo

    def NullVariableInfo(): VerificationTypeInfo

    def UninitializedThisVariableInfo(): VerificationTypeInfo

    /**
     * The Uninitialized_variable_info indicates that the location contains the
     * veriﬁcation type uninitialized(offset). The offset item indicates the offset of
     * the new instruction that created the object being stored in the location.
     */
    def UninitializedVariableInfo(offset: Int): VerificationTypeInfo

    /**
     * The Object_variable_info type indicates that the location contains an instance
     * of the class referenced by the constant pool entry.
     */
    def ObjectVariableInfo(
        cp: Constant_Pool,
        type_index: Constant_Pool_Index): VerificationTypeInfo

    //
    // IMPLEMENTATION
    //

    def VerificationTypeInfo(cp: Constant_Pool, in: DataInputStream): VerificationTypeInfo = {
        val tag = in.readUnsignedByte
        verification_type_info_reader(tag)(cp, in)
    }

    private val verification_type_info_reader = {

        import VerificationTypeInfoItem._

        val r = new Array[(Constant_Pool, DataInputStream) ⇒ VerificationTypeInfo](9)

        r(ITEM_Top.id) =
            (cp: Constant_Pool, in: DataInputStream) ⇒ TopVariableInfo()

        r(ITEM_Integer.id) =
            (cp: Constant_Pool, in: DataInputStream) ⇒ IntegerVariableInfo()

        r(ITEM_Float.id) =
            (cp: Constant_Pool, in: DataInputStream) ⇒ FloatVariableInfo()

        r(ITEM_Long.id) =
            (cp: Constant_Pool, in: DataInputStream) ⇒ LongVariableInfo()

        r(ITEM_Double.id) =
            (cp: Constant_Pool, in: DataInputStream) ⇒ DoubleVariableInfo()

        r(ITEM_Null.id) =
            (cp: Constant_Pool, in: DataInputStream) ⇒ NullVariableInfo()

        r(ITEM_UninitializedThis.id) =
            (cp: Constant_Pool, in: DataInputStream) ⇒ UninitializedThisVariableInfo()

        r(ITEM_Object.id) =
            (cp: Constant_Pool, in: DataInputStream) ⇒
                ObjectVariableInfo(cp, in.readUnsignedShort)

        r(ITEM_Unitialized.id) =
            (cp: Constant_Pool, in: DataInputStream) ⇒
                UninitializedVariableInfo(in.readUnsignedShort)

        r
    }
}

object VerificationTypeInfoItem extends Enumeration {
    final val ITEM_Top = Value(0)
    final val ITEM_Integer = Value(1)
    final val ITEM_Float = Value(2)
    final val ITEM_Long = Value(4)
    final val ITEM_Double = Value(3)
    final val ITEM_Null = Value(5)
    final val ITEM_UninitializedThis = Value(6)
    final val ITEM_Object = Value(7)
    final val ITEM_Unitialized = Value(8)
}

