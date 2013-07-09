/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package resolved
package reader

import reflect.ClassTag

import de.tud.cs.st.bat.reader.StackMapTable_attributeReader
import de.tud.cs.st.bat.reader.StackMapFrameReader
import de.tud.cs.st.bat.reader.VerificationTypeInfoReader

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

    type VerificationTypeInfo = de.tud.cs.st.bat.resolved.VerificationTypeInfo
    val VerificationTypeInfoManifest: ClassTag[VerificationTypeInfo] = implicitly
    type StackMapTable_attribute = de.tud.cs.st.bat.resolved.StackMapTable
    type StackMapFrame = de.tud.cs.st.bat.resolved.StackMapFrame
    type FullFrame = de.tud.cs.st.bat.resolved.FullFrame
    type SameFrame = de.tud.cs.st.bat.resolved.SameFrame
    type AppendFrame = de.tud.cs.st.bat.resolved.AppendFrame
    type SameFrameExtended = de.tud.cs.st.bat.resolved.SameFrameExtended
    type ChopFrame = de.tud.cs.st.bat.resolved.ChopFrame
    type SameLocals1StackItemFrame = de.tud.cs.st.bat.resolved.SameLocals1StackItemFrame
    type SameLocals1StackItemFrameExtended = de.tud.cs.st.bat.resolved.SameLocals1StackItemFrameExtended

    type ObjectVariableInfo = de.tud.cs.st.bat.resolved.ObjectVariableInfo
    type UninitializedVariableInfo = de.tud.cs.st.bat.resolved.UninitializedVariableInfo
    type TopVariableInfo = de.tud.cs.st.bat.resolved.VerificationTypeInfo
    type IntegerVariableInfo = de.tud.cs.st.bat.resolved.VerificationTypeInfo
    type FloatVariableInfo = de.tud.cs.st.bat.resolved.VerificationTypeInfo
    type LongVariableInfo = de.tud.cs.st.bat.resolved.VerificationTypeInfo
    type DoubleVariableInfo = de.tud.cs.st.bat.resolved.VerificationTypeInfo
    type NullVariableInfo = de.tud.cs.st.bat.resolved.VerificationTypeInfo
    type UninitializedThisVariableInfo = de.tud.cs.st.bat.resolved.VerificationTypeInfo

    val StackMapFrameManifest: ClassTag[StackMapFrame] = implicitly

    def StackMapTable_attribute(attribute_name_index: Constant_Pool_Index,
                                attribute_length: Int,
                                stack_map_frames: StackMapFrames)(implicit cp: Constant_Pool) =
        new StackMapTable(stack_map_frames)

    def SameFrame(frame_type: Int): StackMapFrame =
        new SameFrame(frame_type)

    def SameLocals1StackItemFrame(
        frame_type: Int, verification_type_info_stack: VerificationTypeInfo): StackMapFrame =
        new SameLocals1StackItemFrame(frame_type, verification_type_info_stack)

    def SameLocals1StackItemFrameExtended(frame_type: Int,
                                          offset_delta: Int,
                                          verification_type_info_stack: VerificationTypeInfo): StackMapFrame =
        new SameLocals1StackItemFrameExtended(frame_type, offset_delta, verification_type_info_stack)

    def ChopFrame(frame_type: Int, offset_delta: Int): StackMapFrame =
        new ChopFrame(frame_type, offset_delta)

    def SameFrameExtended(frame_type: Int, offset_delta: Int): StackMapFrame =
        new SameFrameExtended(frame_type, offset_delta)

    def AppendFrame(frame_type: Int, offset_delta: Int, verification_type_info_locals: VerificationTypeInfoLocals): StackMapFrame =
        new AppendFrame(frame_type, offset_delta, verification_type_info_locals)

    def FullFrame(frame_type: Int, offset_delta: Int,
                  verification_type_info_locals: VerificationTypeInfoLocals,
                  verification_type_info_stack: VerificationTypeInfoStack): StackMapFrame =
        new FullFrame(frame_type, offset_delta, verification_type_info_locals, verification_type_info_stack)

    def TopVariableInfo() = de.tud.cs.st.bat.resolved.TopVariableInfo

    def IntegerVariableInfo() = de.tud.cs.st.bat.resolved.IntegerVariableInfo

    def FloatVariableInfo() = de.tud.cs.st.bat.resolved.FloatVariableInfo

    def LongVariableInfo() = de.tud.cs.st.bat.resolved.LongVariableInfo

    def DoubleVariableInfo() = de.tud.cs.st.bat.resolved.DoubleVariableInfo

    def NullVariableInfo() = de.tud.cs.st.bat.resolved.NullVariableInfo

    def UninitializedThisVariableInfo() = de.tud.cs.st.bat.resolved.UninitializedThisVariableInfo

    def UninitializedVariableInfo(offset: Int) = new UninitializedVariableInfo(offset)

    def ObjectVariableInfo(cpool_index: Int)(implicit cp: Constant_Pool) = {
        new ObjectVariableInfo(cpool_index.asReferenceType)
    }

}


