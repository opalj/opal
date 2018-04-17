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
 * Determine if object is of given type.
 *
 * @author Michael Eichberg
 */
case class INSTANCEOF(
        referenceType: ReferenceType
) extends Instruction with ConstantLengthInstruction with NoLabels {

    final def opcode: Opcode = INSTANCEOF.opcode

    final def mnemonic: String = "instanceof"

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def length: Int = INSTANCEOF.length

    final def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = 0

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }

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
        Chain.singleton(indexOfNextInstruction(currentPC))
    }

    final def expressionResult: Stack.type = Stack

    override def toString: String = s"INSTANCEOF(${referenceType.toJava})"

    final override def toString(currentPC: Int): String = toString()
}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object INSTANCEOF {

    final val opcode = 193

    final val length = 3

    /**
     * Factory method to create [[INSTANCEOF]] instructions.
     *
     * @param   referenceTypeName The `referenceType` against which the type test is done; see
     * +            [[org.opalj.br.ReferenceType$]]'s `apply` method for the correct syntax.
     */
    def apply(referenceTypeName: String): INSTANCEOF = INSTANCEOF(ReferenceType(referenceTypeName))
}
