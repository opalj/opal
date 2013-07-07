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

/**
 * Part of the Java 6 stack map table attribute.
 *
 * @author Michael Eichberg
 */
sealed trait StackMapFrame /*extends de.tud.cs.st.bat.StackMapFrame*/ {
    type VerificationTypeInfo = de.tud.cs.st.bat.resolved.VerificationTypeInfo
    // [DOCUMENTATION ONLY]	type VerificationTypeInfoLocals = IndexedSeq[VerificationTypeInfo]
    // [DOCUMENTATION ONLY]	type VerificationTypeInfoStack = IndexedSeq[VerificationTypeInfo]

    def frameType: Int

    //final def frame_type = frameType 
}

case class SameFrame(frameType: Int)
        extends StackMapFrame {

}

case class SameLocals1StackItemFrame(
    frameType: Int,
    verificationTypeInfoStackItem: VerificationTypeInfo)
        extends StackMapFrame {

}

case class SameLocals1StackItemFrameExtended(
    frameType: Int,
    offsetDelta: Int,
    verificationTypeInfoStackItem: VerificationTypeInfo)
        extends StackMapFrame {

}

case class ChopFrame(
    frameType: Int,
    offsetDelta: Int)
        extends StackMapFrame {

}

case class SameFrameExtended(
    frameType: Int,
    offsetDelta: Int)
        extends StackMapFrame {

}

case class AppendFrame(
    frameType: Int,
    offsetDelta: Int,
    verificationTypeInfoLocals: VerificationTypeInfoLocals)
        extends StackMapFrame {

}

case class FullFrame(
    frameType: Int,
    offsetDelta: Int,
    verificationTypeInfoLocals: VerificationTypeInfoLocals,
    verificationTypeInfoStack: VerificationTypeInfoStack)
        extends StackMapFrame {

}
