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

import org.opalj.collection.mutable.{ UShortSet ⇒ MutableUShortSet }

/**
 * Common superclass of all instructions that perform a conditional jump.
 *
 * @author Michael Eichberg
 */
abstract class SimpleConditionalBranchInstruction
        extends ConditionalBranchInstruction
        with ConstantLengthInstruction {

    def branchoffset: Int

    /**
     * The comparison operator (incl. the constant) underlying the if instruction.
     * E.g., `<`, `< 0` or `!= null`.
     */
    def operator: String

    final def length: Int = 3

    final def nextInstructions(currentPC: PC, code: Code): PCs =
        MutableUShortSet(indexOfNextInstruction(currentPC, code), currentPC + branchoffset)

    override def toString(currentPC: Int) =
        getClass.getSimpleName+
            "(true="+(currentPC + branchoffset) + (if (branchoffset >= 0) "↓" else "↑")+
            ", false=↓)"
}

abstract class IF0Instruction extends SimpleConditionalBranchInstruction {

    def operandCount = 1

}

abstract class IFICMPInstruction extends SimpleConditionalBranchInstruction {

    def operandCount = 2

}

abstract class IFACMPInstruction extends SimpleConditionalBranchInstruction {

    def operandCount = 2
}

abstract class IFXNullInstruction extends SimpleConditionalBranchInstruction {

    def operandCount = 1

}

