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

import scala.annotation.switch
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.Naught
import org.opalj.collection.immutable.IntTrieSet

/**
 * An instruction that returns from a method.
 *
 * @author Michael Eichberg
 */
abstract class ReturnInstruction extends Instruction with ConstantLengthInstruction with NoLabels {

    final override def isReturnInstruction: Boolean = true

    final override def asReturnInstruction: ReturnInstruction = this

    /**
     * @see [[ReturnInstruction$.jvmExceptions]]
     */
    final def jvmExceptions: List[ObjectType] = ReturnInstruction.jvmExceptions

    final def mayThrowExceptions: Boolean = true

    final def length: Int = 1

    final def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 0

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this eq code.instructions(otherPC)
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
        if (regularSuccessorsOnly)
            Naught
        else {
            val ehs = code.handlersForException(currentPC, ReturnInstruction.jvmExceptions.head)
            ehs.map(_.handlerPC)
        }
    }

    final def expressionResult: NoExpression.type = NoExpression

    final override def toString(currentPC: Int): String = toString()
}

/**
 * Defines common values and a factory method to create a `ReturnInstruction` based
 * on the expected type.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
object ReturnInstruction {

    val jvmExceptions = List(ObjectType.IllegalMonitorStateException)

    def apply(theType: Type): ReturnInstruction = {
        (theType.id: @switch) match {
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

    def unapply(instruction: Instruction): Option[ReturnInstruction] =
        (instruction.opcode: @switch) match {
            case RETURN.opcode
                | IRETURN.opcode
                | LRETURN.opcode
                | FRETURN.opcode
                | DRETURN.opcode
                | ARETURN.opcode ⇒
                Some(instruction.asInstanceOf[ReturnInstruction])
            case _ ⇒
                None
        }

}
/**
 * Defines extractor methods related to return instructions.
 *
 * @author Michael Eichberg
 */
object ReturnInstructions {

    def unapply(code: Code): Option[PCs] = {
        if (code eq null)
            return None;

        val instructions = code.instructions
        val max = instructions.length
        var pc = 0
        var returnPCs = IntTrieSet.empty
        while (pc < max) {
            val instruction = instructions(pc)
            if (instruction.isReturnInstruction)
                returnPCs += pc
            pc = instruction.indexOfNextInstruction(pc)(code)
        }
        Some(returnPCs)
    }
}

object MethodCompletionInstruction {

    def unapply(i: Instruction): Boolean = {
        (i.opcode: @switch) match {
            case ATHROW.opcode |
                RETURN.opcode |
                ARETURN.opcode |
                IRETURN.opcode | LRETURN.opcode | FRETURN.opcode | DRETURN.opcode ⇒ true
            case _ ⇒ false
        }

    }
}

object NoMethodCompletionInstruction {

    def unappy(i: Instruction): Boolean = !MethodCompletionInstruction.unapply(i)

}
