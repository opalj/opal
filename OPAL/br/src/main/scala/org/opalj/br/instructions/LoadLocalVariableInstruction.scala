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
 * An instruction that loads a local variable and puts it on top of the stack.
 *
 * @author Michael Eichberg
 */
abstract class LoadLocalVariableInstruction extends Instruction {

    /**
     * The index of the local variable(register) that is loaded and put on top
     * of the operand stack.
     */
    def lvIndex: Int

    final def runtimeExceptions: List[ObjectType] = Nil

    final def nextInstructions(currentPC: PC, code: Code): PCs =
        UShortSet(indexOfNextInstruction(currentPC, code))

}
/**
 * Defines a factory method for `LoadLocalVariableInstruction`s.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object LoadLocalVariableInstruction {

    /**
     * Returns the `xLoad` instruction that puts value stored at the given index with
     * the specified type on top of the stack.
     */
    def apply(
        fieldType: FieldType,
        lvIndex: Int): LoadLocalVariableInstruction =
        (fieldType.id: @scala.annotation.switch) match {
            case IntegerType.id ⇒ ILOAD(lvIndex)
            case ByteType.id    ⇒ ILOAD(lvIndex)
            case ShortType.id   ⇒ ILOAD(lvIndex)
            case CharType.id    ⇒ ILOAD(lvIndex)
            case BooleanType.id ⇒ ILOAD(lvIndex)
            case LongType.id    ⇒ LLOAD(lvIndex)
            case FloatType.id   ⇒ FLOAD(lvIndex)
            case DoubleType.id  ⇒ DLOAD(lvIndex)
            case _              ⇒ ALOAD(lvIndex)
        }
}