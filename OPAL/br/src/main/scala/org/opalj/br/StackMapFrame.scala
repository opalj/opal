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
package br

/**
 * Part of the Java 6 stack map table attribute.
 *
 * @author Michael Eichberg
 */
sealed abstract class StackMapFrame {
    type VerificationTypeInfo = br.VerificationTypeInfo
    // [DOCUMENTATION ONLY] type VerificationTypeInfoLocals = IndexedSeq[VerificationTypeInfo]
    // [DOCUMENTATION ONLY] type VerificationTypeInfoStack = IndexedSeq[VerificationTypeInfo]

    def frameType: Int

}

final case class SameFrame(frameType: Int) extends StackMapFrame

final case class SameLocals1StackItemFrame(
    frameType:                     Int,
    verificationTypeInfoStackItem: VerificationTypeInfo
) extends StackMapFrame

final case class SameLocals1StackItemFrameExtended(
        offsetDelta:                   Int,
        verificationTypeInfoStackItem: VerificationTypeInfo
) extends StackMapFrame {

    final def frameType: Int = 247

}

sealed trait ChopFrame extends StackMapFrame {
    def offsetDelta: Int
}
object ChopFrame {

    def apply(frameType: Int, offsetDelta: Int): ChopFrame = {
        frameType match {
            case 248 ⇒ new ChopFrame248(offsetDelta)
            case 249 ⇒ new ChopFrame249(offsetDelta)
            case 250 ⇒ new ChopFrame250(offsetDelta)
        }
    }

    def unapply(cf: ChopFrame): Option[(Int, Int)] = Some((cf.frameType, cf.offsetDelta))
}
final case class ChopFrame248(offsetDelta: Int) extends ChopFrame { final def frameType: Int = 248 }
final case class ChopFrame249(offsetDelta: Int) extends ChopFrame { final def frameType: Int = 249 }
final case class ChopFrame250(offsetDelta: Int) extends ChopFrame { final def frameType: Int = 250 }

final case class SameFrameExtended(offsetDelta: Int) extends StackMapFrame {
    final def frameType: Int = 251
}

final case class AppendFrame(
    frameType:                  Int,
    offsetDelta:                Int,
    verificationTypeInfoLocals: VerificationTypeInfoLocals
) extends StackMapFrame

final case class FullFrame(
    frameType:                  Int,
    offsetDelta:                Int,
    verificationTypeInfoLocals: VerificationTypeInfoLocals,
    verificationTypeInfoStack:  VerificationTypeInfoStack
) extends StackMapFrame
