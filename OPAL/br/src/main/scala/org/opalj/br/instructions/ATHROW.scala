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

import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.Naught

/**
 * Throw exception or error.
 *
 * @author Michael Eichberg
 */
case object ATHROW extends Instruction with NoLabels {

    final val opcode = 191

    final val mnemonic = "athrow"

    final override def isAthrow: Boolean = true

    final override def asATHROW: this.type = this

    final def jvmExceptions: List[ObjectType] = Instruction.justNullPointerException

    final def mayThrowExceptions: Boolean = true

    final def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 0

    final def stackSlotsChange: Int = -1 // take the current exception or null

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean =
        this eq code.instructions(otherPC)

    final val readsLocal = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final val writesLocal = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final def indexOfNextInstruction(currentPC: Int)(implicit code: Code) = {
        indexOfNextInstruction(currentPC, false)
    }

    final def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): Int = currentPC + 1

    final def nextInstructions(
        currentPC: PC, regularSuccessorsOnly: Boolean
    )(
        implicit
        code: Code, classHierarchy: ClassHierarchy
    ): Chain[PC] = {
        if (regularSuccessorsOnly)
            Naught
        else
            code.handlerInstructionsFor(currentPC)
    }

    final def expressionResult: NoExpression.type = NoExpression

    final override def toString(currentPC: Int): String = toString()
}
