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
 * An instruction that returns from a method.
 *
 * @author Michael Eichberg
 */
abstract class ReturnInstruction extends Instruction {

    final def runtimeExceptions: List[ObjectType] =
        ReturnInstruction.runtimeExceptions

    final def indexOfNextInstruction(currentPC: Int, code: Code): Int =
        indexOfNextInstruction(currentPC)

    final def indexOfNextInstruction(
        currentPC: PC,
        modifiedByWide: Boolean = false): Int =
        currentPC + 1

    final def nextInstructions(currentPC: PC, code: Code): PCs =
        org.opalj.collection.mutable.UShortSet.empty

}
/**
 * Defines common values and a factory method to create a `ReturnInstruction` based
 * on the expected type.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
object ReturnInstruction {

    val runtimeExceptions = List(ObjectType.IllegalMonitorStateException)

    def apply(theType: Type): ReturnInstruction =
        (theType.id: @scala.annotation.switch) match {
            case VoidType.id    ⇒ RETURN
            case IntegerType.id ⇒ IRETURN
            case ShortType.id   ⇒ IRETURN
            case ByteType.id    ⇒ IRETURN
            case CharType.id    ⇒ IRETURN
            case BooleanType.id ⇒ IRETURN
            case LongType.id    ⇒ LRETURN
            case FloatType.id   ⇒ FRETURN
            case DoubleType.id  ⇒ DRETURN
            case _              ⇒ ARETURN
        }

}