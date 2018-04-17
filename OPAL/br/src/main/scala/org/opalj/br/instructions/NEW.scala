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
import org.opalj.br.ObjectType.OutOfMemoryError

/**
 * Create new object.
 *
 * @author Michael Eichberg
 */
case class NEW(
        objectType: ObjectType
) extends Instruction with ConstantLengthInstruction with NoLabels {

    final override def opcode: Opcode = NEW.opcode

    final override def asNEW: NEW = this

    final override def mnemonic: String = "new"

    final override def jvmExceptions: List[ObjectType] = NEW.jvmExceptions

    final override def mayThrowExceptions: Boolean = true

    final override def length: Int = 3

    final override def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 0

    final override def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 1

    final override def stackSlotsChange: Int = 1

    final override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }

    final override def readsLocal: Boolean = false

    final override def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final override def writesLocal: Boolean = false

    final override def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final override def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Chain[PC] = {
        if (regularSuccessorsOnly)
            Chain.singleton(indexOfNextInstruction(currentPC))
        else
            Instruction.nextInstructionOrExceptionHandler(this, currentPC, OutOfMemoryError)
    }

    final override def expressionResult: Stack.type = Stack

    override def toString: String = "NEW "+objectType.toJava

    final override def toString(currentPC: Int): String = toString()
}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
object NEW {

    final val opcode = 187

    final val jvmExceptions = List(ObjectType.OutOfMemoryError)

    /**
     * Creates a new [[NEW]] instruction given the fully qualified name in binary notation.
     * @see     [[org.opalj.br.ObjectType$]] for details.
     */
    def apply(fqn: String): NEW = NEW(ObjectType(fqn))

}
