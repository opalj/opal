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
package org.opalj
package br
package instructions

import org.opalj.br.cfg.CFGFactory
import org.opalj.collection.mutable.UShortSet

/**
 * Return from subroutine.
 *
 * @note A RET instruction always returns to an instruction immediately following a JSR(_W)
 * 		instruction.
 *
 * @author Michael Eichberg
 */
case class RET(lvIndex: Int)
        extends ControlTransferInstruction
        with ConstantLengthInstruction {

    final def opcode: Opcode = RET.opcode

    final def mnemonic: String = "ret"

    final def length: Int = 2

    final def nextInstructions(
        currentPC:             PC,
        code:                  Code,
        regularSuccessorsOnly: Boolean
    ): PCs = {
        // the fallback is only used if we have multiple return instructions
        def fallback() = {
            val classHierarchy = ClassHierarchy.preInitializedClassHierarchy
            val cfg = CFGFactory(code, classHierarchy)
            UShortSet.create((cfg.successors(currentPC).toSeq: _*))
        }

        // If we have just one subroutine it is sufficient to collect the 
        // successor instructions of all JSR instructions.
        var jumpTargets = UShortSet.empty
        code.foreach { (pc, instruction) ⇒
            if (pc != currentPC) {
                instruction.opcode match {
                    case JSR.opcode | JSR_W.opcode ⇒
                        jumpTargets = (instruction.indexOfNextInstruction(pc, code)) +≈: jumpTargets
                    case RET.opcode ⇒
                        // we have found another RET ... hence, we have at least two subroutines
                        return fallback();
                    case _ ⇒ // we don't care 
                }
            }
        }
        jumpTargets
    }

    final def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 0

    final def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 0

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }

    final def readsLocal: Boolean = true

    final def indexOfReadLocal: Int = lvIndex

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

}
object RET {

    final val opcode = 169

}
