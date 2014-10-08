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

import org.opalj.collection.mutable.UShortSet

/**
 * An instruction that stores the top-most stack value in a local variable.
 *
 * @author Michael Eichberg
 */
abstract class StoreLocalVariableInstruction extends Instruction {

    def lvIndex: Int

    def runtimeExceptions: List[ObjectType] = Nil

    final def nextInstructions(currentPC: PC, code: Code): PCs =
        UShortSet(indexOfNextInstruction(currentPC, code))

    final def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 0

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = true

    final def indexOfWrittenLocal: Int = lvIndex

}

/**
 * Factory for `StoreLocalVariableInstruction`s.
 *
 * @author Arne Lottmann
 */
object StoreLocalVariableInstruction {

    /**
     * Returns the `xStore` instruction that stores the variable at the top of the stack
     * of the specified type in the local variable at the given index.
     */
    def apply(fieldType: FieldType, lvIndex: Int): StoreLocalVariableInstruction =
        (fieldType.id: @scala.annotation.switch) match {
            case IntegerType.id ⇒ ISTORE.canonicalRepresentation(lvIndex)
            case ByteType.id    ⇒ ISTORE.canonicalRepresentation(lvIndex)
            case ShortType.id   ⇒ ISTORE.canonicalRepresentation(lvIndex)
            case CharType.id    ⇒ ISTORE.canonicalRepresentation(lvIndex)
            case BooleanType.id ⇒ ISTORE.canonicalRepresentation(lvIndex)
            case LongType.id    ⇒ LSTORE.canonicalRepresentation(lvIndex)
            case FloatType.id   ⇒ FSTORE.canonicalRepresentation(lvIndex)
            case DoubleType.id  ⇒ DSTORE.canonicalRepresentation(lvIndex)
            case _              ⇒ ASTORE.canonicalRepresentation(lvIndex)
        }
}