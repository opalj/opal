/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package collection
package mutable

import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ConstantLengthInstruction

import scala.collection.mutable.ArrayBuffer

/**
 * A buffer for creating bytecode arrays that automatically adds the required null entries.
 *
 * @author Dominik Helm
 *         @author Michael Eichberg
 */
class InstructionsBuilder private (private val buffer: ArrayBuffer[Instruction]) {

    def this(initialSize: Int) =
        this(new ArrayBuffer[Instruction](initialSize = initialSize))

    /**
     * Adds the given instruction to the buffer and adds the appropriate number of `null`
     * values to the array such that the instruction occupies exactly so many slots as
     * specified by `slots`. Hence, `slotes` has to be >= 1.
     */
    def ++=(value: Instruction, slots: Int): Unit = {
        //        if (slots > 1) buffer.ensureAdditionalCapacity(slots)
        buffer += value
        var i = 1
        while (i < slots) {
            buffer += null
            i += 1
        }
    }

    /**
     * Adds the given instruction to the buffer and adds the appropriate number of `null`
     * values to the array such that the instruction occupies exactly so many slots as
     * required by the JVM specification.
     */
    final def ++=(value: ConstantLengthInstruction): Unit = this.++=(value, value.length)

    /**
     * Adds the given instructions as is to this builder. Hence, the instructions already
     * have to use the required layout.
     */
    def ++=(instructions: Array[Instruction]): Unit = buffer ++= instructions

    /**
     * Returns the build instructions array; this builder is not to be used afterwards.
     */
    def result(): Array[Instruction] = buffer.toArray
}
