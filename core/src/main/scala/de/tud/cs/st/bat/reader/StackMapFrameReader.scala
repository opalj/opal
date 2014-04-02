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
package reader

import java.io.DataInputStream

import reflect.ClassTag

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

    def VerificationTypeInfo(in: DataInputStream, cp: Constant_Pool): VerificationTypeInfo

    def SameFrame(frame_type: Int): StackMapFrame

    def SameLocals1StackItemFrame(
        frame_type: Int,
        verification_type_info_stack: VerificationTypeInfo): StackMapFrame

    def SameLocals1StackItemFrameExtended(
        frame_type: Int,
        offset_delta: Int,
        verification_type_info_stack: VerificationTypeInfo): StackMapFrame

    def ChopFrame(
        frame_type: Int,
        offset_delta: Int): StackMapFrame

    def SameFrameExtended(
        frame_type: Int,
        offset_delta: Int): StackMapFrame

    def AppendFrame(
        frame_type: Int,
        offset_delta: Int,
        verification_type_info_locals: VerificationTypeInfoLocals): StackMapFrame

    def FullFrame(
        frame_type: Int,
        offset_delta: Int,
        verification_type_info_locals: VerificationTypeInfoLocals,
        verification_type_info_stack: VerificationTypeInfoStack): StackMapFrame

    //
    // IMPLEMENTATION
    //

    import util.ControlAbstractions.repeat

    type VerificationTypeInfoLocals = IndexedSeq[VerificationTypeInfo]
    type VerificationTypeInfoStack = IndexedSeq[VerificationTypeInfo]

    def StackMapFrame(in: DataInputStream, cp: Constant_Pool): StackMapFrame = {
        val frame_type = in.readUnsignedByte
        frame_type match {
            /*Same_Frame*/
            case t if (t < 64) ⇒ SameFrame(t);

            /*Same_Locals_1_Stack_Item_Frame*/
            case t if (t < 128) ⇒
                SameLocals1StackItemFrame(
                    t,
                    VerificationTypeInfo(in, cp));

            /*RESERVED FOR FUTURE USE*/
            case t if (t < 247) ⇒ sys.error("Unknonwn frame type.");

            /*Same_Locals_1_Stack_Item_Frame_Extended*/
            case 247 ⇒
                SameLocals1StackItemFrameExtended(
                    247,
                    in.readUnsignedShort,
                    VerificationTypeInfo(in, cp));

            /*Chop_Frame*/
            case t if (t < 251) ⇒ ChopFrame(t, in.readUnsignedShort)

            /*Same_Frame_Extended*/
            case 251            ⇒ SameFrameExtended(251, in.readUnsignedShort)

            /*Append_Frame*/
            case t if (t < 255) ⇒ AppendFrame(
                t,
                in.readUnsignedShort,
                repeat(t - 251 /*number of entries*/ ) {
                    VerificationTypeInfo(in, cp)
                }
            )

            /*Full_Frame*/
            case 255 ⇒ FullFrame(
                255,
                in.readUnsignedShort,
                repeat(in.readUnsignedShort /*number of entries*/ ) {
                    VerificationTypeInfo(in, cp) // ...locals
                },
                repeat(in.readUnsignedShort /*number of entries*/ ) {
                    VerificationTypeInfo(in, cp) // ...stack items
                }
            )
        }
    }
}
