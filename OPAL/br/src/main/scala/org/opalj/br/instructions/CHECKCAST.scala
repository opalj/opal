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

/**
 * Check whether object is of given type.
 *
 * @author Michael Eichberg
 */
case class CHECKCAST(
        referenceType: ReferenceType
) extends Instruction with ConstantLengthInstruction with NoLabels {

    final override def isCheckcast: Boolean = true

    final def opcode: Opcode = CHECKCAST.opcode

    final def mnemonic: String = "checkcast"

    final def jvmExceptions: List[ObjectType] = CHECKCAST.jvmExceptions

    final def mayThrowExceptions: Boolean = true

    final def length: Int = 3

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }

    final def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = 0

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final def nextInstructions(
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
            Instruction.nextInstructionOrExceptionHandler(
                this, currentPC, ObjectType.ClassCastException
            )
    }

    final def expressionResult: Stack.type = Stack

    override def toString: String = "CHECKCAST("+referenceType.toJava+")"

    final override def toString(currentPC: Int): String = toString()

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object CHECKCAST {

    final val opcode = 192

    val jvmExceptions = List(ObjectType.ClassCastException)

    /**
     * Factory method to create [[CHECKCAST]] instructions.
     *
     * @param   referenceType The name of the [[org.opalj.br.ReferenceType]]. See the corresponding
     *          factory method for further details.
     */
    def apply(referenceType: String): CHECKCAST = CHECKCAST(ReferenceType(referenceType))

}
