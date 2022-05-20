/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.StackMapTable_attributeReader
import org.opalj.bi.reader.StackMapFrameReader
import org.opalj.bi.reader.VerificationTypeInfoReader

import scala.reflect.ClassTag

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
    override implicit val verificationTypeInfoType: ClassTag[VerificationTypeInfo] = ClassTag(classOf[br.VerificationTypeInfo])
    type StackMapTable_attribute = br.StackMapTable
    type StackMapFrame = br.StackMapFrame
    override implicit val stackMapFrameType: ClassTag[StackMapFrame] = ClassTag(classOf[br.StackMapFrame])
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

    def StackMapTable_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        stack_map_frames:     StackMapFrames
    ): StackMapTable_attribute = StackMapTable(stack_map_frames)

    def SameFrame(frame_type: Int): StackMapFrame = br.SameFrame(frame_type)

    def SameLocals1StackItemFrame(
        frame_type:                   Int,
        verification_type_info_stack: VerificationTypeInfo
    ): StackMapFrame = br.SameLocals1StackItemFrame(frame_type, verification_type_info_stack)

    def SameLocals1StackItemFrameExtended(
        frame_type:                   Int,
        offset_delta:                 Int,
        verification_type_info_stack: VerificationTypeInfo
    ): StackMapFrame =
        br.SameLocals1StackItemFrameExtended(offset_delta, verification_type_info_stack)

    def ChopFrame(
        frame_type:   Int,
        offset_delta: Int
    ): ChopFrame = br.ChopFrame(frame_type, offset_delta)

    def SameFrameExtended(
        frame_type:   Int,
        offset_delta: Int
    ): StackMapFrame = br.SameFrameExtended(offset_delta)

    def AppendFrame(
        frame_type:                    Int,
        offset_delta:                  Int,
        verification_type_info_locals: VerificationTypeInfoLocals
    ): StackMapFrame = br.AppendFrame(frame_type, offset_delta, verification_type_info_locals)

    def FullFrame(
        frame_type:                    Int,
        offset_delta:                  Int,
        verification_type_info_locals: VerificationTypeInfoLocals,
        verification_type_info_stack:  VerificationTypeInfoStack
    ): StackMapFrame = {
        br.FullFrame(offset_delta, verification_type_info_locals, verification_type_info_stack)
    }

    def TopVariableInfo(): br.TopVariableInfo.type = br.TopVariableInfo

    def IntegerVariableInfo(): br.IntegerVariableInfo.type = br.IntegerVariableInfo

    def FloatVariableInfo(): br.FloatVariableInfo.type = br.FloatVariableInfo

    def LongVariableInfo(): br.LongVariableInfo.type = br.LongVariableInfo

    def DoubleVariableInfo(): br.DoubleVariableInfo.type = br.DoubleVariableInfo

    def NullVariableInfo(): br.NullVariableInfo.type = br.NullVariableInfo

    def UninitializedThisVariableInfo(): br.UninitializedThisVariableInfo.type = {
        br.UninitializedThisVariableInfo
    }

    def UninitializedVariableInfo(offset: Int) = new UninitializedVariableInfo(offset)

    def ObjectVariableInfo(
        cp:         Constant_Pool,
        type_index: Constant_Pool_Index
    ): ObjectVariableInfo = {
        new ObjectVariableInfo(cp(type_index).asReferenceType(cp))
    }

}
