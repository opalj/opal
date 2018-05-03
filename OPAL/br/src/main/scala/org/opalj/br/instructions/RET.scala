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
package instructions

import org.opalj.br.cfg.CFGFactory
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.Chain

/**
 * Return from subroutine.
 *
 * @note A RET instruction always returns to an instruction immediately following a JSR(_W)
 *      instruction.
 *
 * @author Michael Eichberg
 */
case class RET(
        lvIndex: Int
) extends ControlTransferInstruction with ConstantLengthInstruction with NoLabels {

    final override def opcode: Opcode = RET.opcode

    final override def mnemonic: String = "ret"

    final override def isRET: Boolean = true

    final override def length: Int = 2

    final override def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Chain[PC] = {
        nextInstructions(currentPC, () ⇒ CFGFactory(code, classHierarchy))
    }

    override def jumpTargets(
        currentPC: PC
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Iterator[PC] = {
        nextInstructions(currentPC, false /*irrelevant*/ ).toIterator
    }

    final def nextInstructions(
        currentPC: PC, cfg: () ⇒ CFG
    )(
        implicit
        code: Code
    ): Chain[PC] = {

        // If we have just one subroutine it is sufficient to collect the
        // successor instructions of all JSR instructions.
        var jumpTargets = Chain.empty[PC]
        code.iterate { (pc, instruction) ⇒
            if (pc != currentPC) { // filter this ret!
                instruction.opcode match {

                    case JSR.opcode | JSR_W.opcode ⇒
                        jumpTargets :&:= (instruction.indexOfNextInstruction(pc))

                    case RET.opcode ⇒
                        // we have found another RET ... hence, we have at least two subroutines
                        return cfg().successors(currentPC).toChain;

                    case _ ⇒
                    // we don't care
                }
            }
        }
        jumpTargets
    }

    final override def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 0

    final override def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 0

    final override def stackSlotsChange: Int = 0

    final override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }

    final override def readsLocal: Boolean = true

    final override def indexOfReadLocal: Int = lvIndex

    final override def writesLocal: Boolean = false

    final override def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final override def toString(currentPC: Int): String = toString()
}
object RET {

    final val opcode = 169

}
