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
package br
package reader

import scala.reflect.ClassTag

import org.opalj.bi.reader.StackMapTable_attributeReader
import org.opalj.bi.reader.StackMapFrameReader
import org.opalj.bi.reader.VerificationTypeInfoReader

/**
 * Provides the factory methods to create a stack map table attribute and
 * its entries.
 *
 * @author Michael Eichberg
 */
trait StackMapTable_attributeBinding
        extends StackMapTable_attributeReader
        with StackMapFrameReader
        with VerificationTypeInfoReader
        with ConstantPoolBinding
        with AttributeBinding {

    type VerificationTypeInfo = br.VerificationTypeInfo
    val VerificationTypeInfoManifest: ClassTag[VerificationTypeInfo] = implicitly
    type StackMapTable_attribute = br.StackMapTable
    type StackMapFrame = br.StackMapFrame
    type FullFrame = br.FullFrame
    type SameFrame = br.SameFrame
    type AppendFrame = br.AppendFrame
    type SameFrameExtended = br.SameFrameExtended
    type ChopFrame = br.ChopFrame
    type SameLocals1StackItemFrame = br.SameLocals1StackItemFrame
    type SameLocals1StackItemFrameExtended = br.SameLocals1StackItemFrameExtended

    type ObjectVariableInfo = br.ObjectVariableInfo
    type UninitializedVariableInfo = br.UninitializedVariableInfo
    type TopVariableInfo = br.VerificationTypeInfo
    type IntegerVariableInfo = br.VerificationTypeInfo
    type FloatVariableInfo = br.VerificationTypeInfo
    type LongVariableInfo = br.VerificationTypeInfo
    type DoubleVariableInfo = br.VerificationTypeInfo
    type NullVariableInfo = br.VerificationTypeInfo
    type UninitializedThisVariableInfo = br.VerificationTypeInfo

    val StackMapFrameManifest: ClassTag[StackMapFrame] = implicitly

    def StackMapTable_attribute(
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        stack_map_frames: StackMapFrames) =
        new StackMapTable(stack_map_frames)

    def SameFrame(frame_type: Int): StackMapFrame =
        new SameFrame(frame_type)

    def SameLocals1StackItemFrame(
        frame_type: Int,
        verification_type_info_stack: VerificationTypeInfo): StackMapFrame =
        new SameLocals1StackItemFrame(frame_type, verification_type_info_stack)

    def SameLocals1StackItemFrameExtended(
        frame_type: Int,
        offset_delta: Int,
        verification_type_info_stack: VerificationTypeInfo): StackMapFrame =
        new SameLocals1StackItemFrameExtended(frame_type, offset_delta, verification_type_info_stack)

    def ChopFrame(
        frame_type: Int,
        offset_delta: Int): StackMapFrame =
        new ChopFrame(frame_type, offset_delta)

    def SameFrameExtended(
        frame_type: Int,
        offset_delta: Int): StackMapFrame =
        new SameFrameExtended(frame_type, offset_delta)

    def AppendFrame(frame_type: Int,
                    offset_delta: Int,
                    verification_type_info_locals: VerificationTypeInfoLocals): StackMapFrame =
        new AppendFrame(frame_type, offset_delta, verification_type_info_locals)

    def FullFrame(
        frame_type: Int, offset_delta: Int,
        verification_type_info_locals: VerificationTypeInfoLocals,
        verification_type_info_stack: VerificationTypeInfoStack): StackMapFrame =
        new FullFrame(frame_type, offset_delta, verification_type_info_locals, verification_type_info_stack)

    def TopVariableInfo() = br.TopVariableInfo

    def IntegerVariableInfo() = br.IntegerVariableInfo

    def FloatVariableInfo() = br.FloatVariableInfo

    def LongVariableInfo() = br.LongVariableInfo

    def DoubleVariableInfo() = br.DoubleVariableInfo

    def NullVariableInfo() = br.NullVariableInfo

    def UninitializedThisVariableInfo() = br.UninitializedThisVariableInfo

    def UninitializedVariableInfo(offset: Int) = new UninitializedVariableInfo(offset)

    def ObjectVariableInfo(cp: Constant_Pool, type_index: Constant_Pool_Index) = {
        new ObjectVariableInfo(cp(type_index).asReferenceType(cp))
    }

}

