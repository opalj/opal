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
package bi
package reader

import java.io.DataInputStream

import scala.reflect.ClassTag
import org.opalj.control.repeat

/**
 *
 * @author Michael Eichberg
 */
trait StackMapFrameReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type StackMapFrame
    type VerificationTypeInfo
    implicit val VerificationTypeInfoManifest: ClassTag[VerificationTypeInfo]

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

    type VerificationTypeInfoLocals = IndexedSeq[VerificationTypeInfo]
    type VerificationTypeInfoStack = IndexedSeq[VerificationTypeInfo]

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
                repeat(frame_type - 251 /*number of entries*/ ) { VerificationTypeInfo(cp, in) }
            )
        } else {
            FullFrame(
                255,
                in.readUnsignedShort,
                repeat(in.readUnsignedShort /*number of entries*/ ) {
                    VerificationTypeInfo(cp, in) // ...locals
                },
                repeat(in.readUnsignedShort /*number of entries*/ ) {
                    VerificationTypeInfo(cp, in) // ...stack items
                }
            )
        }
    }
}
