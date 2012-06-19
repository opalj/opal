/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
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
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.canonical

/**
 *
 * @author Michael Eichberg
 */
trait StackMapFrame {

    type VerificationTypeInfo

    val frame_type: Int

}

trait SameFrame extends StackMapFrame {

    require(0 <= frame_type && frame_type < 64);
}

trait SameLocals1StackItemFrame extends StackMapFrame {

    val verification_type_info_stack: VerificationTypeInfo

    require(63 < frame_type && frame_type < 128);
}

trait SameLocals1StackItemFrameExtended extends StackMapFrame {

    val offset_delta: Int
    val verification_type_info_stack: VerificationTypeInfo

    require(frame_type == 247);
}

trait ChopFrame extends StackMapFrame {

    val offset_delta: Int

    require(247 < frame_type && frame_type < 251);
}

trait SameFrameExtended extends StackMapFrame {

    val offset_delta: Int

    require(frame_type == 251);
}

trait AppendFrame extends StackMapFrame {

    val offset_delta: Int
    val verification_type_info_locals: Seq[VerificationTypeInfo]

    require(251 < frame_type && frame_type < 255);
}

trait FullFrame extends StackMapFrame {

    val offset_delta: Int
    val verification_type_info_locals: Seq[VerificationTypeInfo]
    val verification_type_info_stack: Seq[VerificationTypeInfo]

    require(frame_type == 255);
}