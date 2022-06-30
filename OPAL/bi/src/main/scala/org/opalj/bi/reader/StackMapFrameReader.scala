/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

trait StackMapFrameReader extends Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type StackMapFrame <: AnyRef
    implicit val stackMapFrameType: ClassTag[StackMapFrame] // TODO: Replace in Scala 3 by `type StackMapFrame : ClassTag`

    type VerificationTypeInfo <: AnyRef
    implicit val verificationTypeInfoType: ClassTag[VerificationTypeInfo] // TODO: Replace in Scala 3 by `type VerificationTypeInfo : ClassTag`
    type VerificationTypeInfoLocals = ArraySeq[VerificationTypeInfo]
    type VerificationTypeInfoStack = ArraySeq[VerificationTypeInfo]

    def VerificationTypeInfo(cp: Constant_Pool, in: DataInputStream): VerificationTypeInfo

    def SameFrame(frame_type: Int): StackMapFrame

    def SameLocals1StackItemFrame(
        frame_type:                   Int,
        verification_type_info_stack: VerificationTypeInfo
    ): StackMapFrame

    def SameLocals1StackItemFrameExtended(
        frame_type:                   Int,
        offset_delta:                 Int,
        verification_type_info_stack: VerificationTypeInfo
    ): StackMapFrame

    def ChopFrame(frame_type: Int, offset_delta: Int): StackMapFrame

    def SameFrameExtended(frame_type: Int, offset_delta: Int): StackMapFrame

    def AppendFrame(
        frame_type:                    Int,
        offset_delta:                  Int,
        verification_type_info_locals: VerificationTypeInfoLocals
    ): StackMapFrame

    def FullFrame(
        frame_type:                    Int,
        offset_delta:                  Int,
        verification_type_info_locals: VerificationTypeInfoLocals,
        verification_type_info_stack:  VerificationTypeInfoStack
    ): StackMapFrame

    //
    // IMPLEMENTATION
    //

    def StackMapFrame(cp: Constant_Pool, in: DataInputStream): StackMapFrame = {
        val frame_type = in.readUnsignedByte
        if (frame_type < 64) {
            SameFrame(frame_type)
        } else if (frame_type < 128) {
            SameLocals1StackItemFrame(
                frame_type,
                VerificationTypeInfo(cp, in)
            )
        } /*RESERVED FOR FUTURE USE*/ else if (frame_type < 247) {
            throw new Error(s"unsupported frame type: $frame_type")
        } else if (frame_type == 247) {
            SameLocals1StackItemFrameExtended(
                247,
                in.readUnsignedShort,
                VerificationTypeInfo(cp, in)
            )
        } else if (frame_type < 251) ChopFrame(frame_type, in.readUnsignedShort)
        else if (frame_type == 251) SameFrameExtended(251, in.readUnsignedShort)
        else if (frame_type < 255) {
            AppendFrame(
                frame_type,
                in.readUnsignedShort,
                fillArraySeq(frame_type - 251 /*#entries*/ ) { VerificationTypeInfo(cp, in) }
            )
        } else {
            FullFrame(
                255,
                in.readUnsignedShort,
                fillArraySeq(in.readUnsignedShort /*#entries*/ ) {
                    VerificationTypeInfo(cp, in) // ...locals
                },
                fillArraySeq(in.readUnsignedShort /*#entries*/ ) {
                    VerificationTypeInfo(cp, in) // ...stack items
                }
            )
        }
    }
}
