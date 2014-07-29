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

/**
 * Puts a constant value on the stack.
 *
 * @author Michael Eichberg
 */
abstract class LoadConstantInstruction[T] extends Instruction {

    /**
     * The value that is put onto the stack.
     */
    def value: T

    final def runtimeExceptions: List[ObjectType] = Nil

    final def nextInstructions(currentPC: PC, code: Code): PCs =
        collection.mutable.UShortSet(indexOfNextInstruction(currentPC, code))

}
/**
 * Defines factory methods for `LoadConstantInstruction`s.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object LoadConstantInstruction {

    /**
     * Returns the instruction that puts the constant value on top of the stack
     * that represents the default value that is used to initialize fields
     * of the corresponding type.
     */
    def apply(fieldType: FieldType): LoadConstantInstruction[_] =
        (fieldType.id: @scala.annotation.switch) match {
            case IntegerType.id ⇒ ICONST_0
            case ByteType.id    ⇒ ICONST_0
            case CharType.id    ⇒ ICONST_0
            case ShortType.id   ⇒ ICONST_0
            case BooleanType.id ⇒ ICONST_0
            case LongType.id    ⇒ LCONST_0
            case FloatType.id   ⇒ FCONST_0
            case DoubleType.id  ⇒ DCONST_0
            case _              ⇒ ACONST_NULL
        }
}