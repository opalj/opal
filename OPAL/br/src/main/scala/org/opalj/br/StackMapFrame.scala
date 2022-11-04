/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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

    /**
     * Computes the offset described by this frame.
     *
     * @param previousOffset The offset of the previous stack map frame; -1 if this is the first
     *                       frame.
     */
    def offset(previousOffset: Int): Int

}

final case class SameFrame(frameType: Int) extends StackMapFrame {
    final def offset(previousOffset: Int): Int = previousOffset + frameType + 1
}

final case class SameLocals1StackItemFrame(
        frameType:                     Int,
        verificationTypeInfoStackItem: VerificationTypeInfo
) extends StackMapFrame {
    final def offset(previousOffset: Int): Int = previousOffset + frameType - 64 + 1
}

final case class SameLocals1StackItemFrameExtended(
        offsetDelta:                   Int,
        verificationTypeInfoStackItem: VerificationTypeInfo
) extends StackMapFrame {
    final def frameType: Int = 247
    final def offset(previousOffset: Int): Int = previousOffset + offsetDelta + 1

}

sealed trait ChopFrame extends StackMapFrame {
    def offsetDelta: Int
    final def offset(previousOffset: Int): Int = previousOffset + offsetDelta + 1
}
object ChopFrame {

    def apply(frameType: Int, offsetDelta: Int): ChopFrame = {
        frameType match {
            case 248 => new ChopFrame248(offsetDelta)
            case 249 => new ChopFrame249(offsetDelta)
            case 250 => new ChopFrame250(offsetDelta)
        }
    }

    def unapply(cf: ChopFrame): Option[(Int, Int)] = Some((cf.frameType, cf.offsetDelta))

}
final case class ChopFrame248(offsetDelta: Int) extends ChopFrame { final def frameType: Int = 248 }
final case class ChopFrame249(offsetDelta: Int) extends ChopFrame { final def frameType: Int = 249 }
final case class ChopFrame250(offsetDelta: Int) extends ChopFrame { final def frameType: Int = 250 }

final case class SameFrameExtended(offsetDelta: Int) extends StackMapFrame {
    final def frameType: Int = 251
    final def offset(previousOffset: Int): Int = previousOffset + offsetDelta + 1
}

final case class AppendFrame(
        frameType:                  Int,
        offsetDelta:                Int,
        verificationTypeInfoLocals: VerificationTypeInfoLocals
) extends StackMapFrame {
    final def offset(previousOffset: Int): Int = previousOffset + offsetDelta + 1
}

final case class FullFrame(
        offsetDelta:                Int,
        verificationTypeInfoLocals: VerificationTypeInfoLocals,
        verificationTypeInfoStack:  VerificationTypeInfoStack
) extends StackMapFrame {
    final def frameType: Int = 255
    final def offset(previousOffset: Int): Int = previousOffset + offsetDelta + 1
}
