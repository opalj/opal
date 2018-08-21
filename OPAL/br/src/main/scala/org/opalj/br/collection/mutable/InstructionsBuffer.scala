/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package collection
package mutable

import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ConstantLengthInstruction
import org.opalj.collection.mutable.AnyRefArrayBuffer

import scala.collection.mutable.ArrayBuffer

/**
 * A buffer for creating bytecode arrays that automatically adds the required null entries.
 *
 * @author Dominik Helm
 */
class InstructionsBuffer(val initialSize: Int) {
    private val buffer: AnyRefArrayBuffer[Instruction] =
        AnyRefArrayBuffer.withInitialSize(initialSize)

    def ++=(value: Instruction, slots: Int): Unit = {
        buffer += value
        var i = 1
        while (i < slots) {
            buffer += null
            i += 1
        }
    }

    def ++=(value: ConstantLengthInstruction): Unit = {
        ++=(value, value.length)
    }

    def ++=(instructions: Array[Instruction]): Unit = {
        buffer ++= instructions
    }

    def toArray: Array[Instruction] = buffer.toArray
}
