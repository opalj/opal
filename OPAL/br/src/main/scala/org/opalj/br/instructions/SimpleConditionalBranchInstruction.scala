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
 * Common superclass of all instructions that perform a conditional jump.
 *
 * @author Michael Eichberg
 */
trait SimpleConditionalBranchInstructionLike
    extends ConditionalBranchInstructionLike
    with SimpleBranchInstructionLike {

    /**
     * The comparison operator (incl. the constant) underlying the if instruction.
     * E.g., `<`, `< 0` or `!= null`.
     */
    def operator: String

    final def length: Int = 3

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (this == other)
    }
}

trait SimpleConditionalBranchInstruction[T <: SimpleConditionalBranchInstruction[T]]
    extends ConditionalBranchInstruction
    with SimpleBranchInstruction
    with SimpleConditionalBranchInstructionLike {

    def copy(branchoffset: Int): SimpleConditionalBranchInstruction[T]

    /**
     * Returns the IF instruction that - when compared with this if instruction -
     * performs a jump in case of a fall-through and vice-versa. I.e., given the
     * following condition: `(a < b)`, the negation is performend: `!(a < b)` which
     * is equivalent to `(a &geq; b)`. In other words,  if this IF instruction is an
     * IFGT instruction and IFLE instruction is returned.
     */
    def negate(newBranchoffset: Int = branchoffset): SimpleConditionalBranchInstruction[_]

    final override def isSimpleConditionalBranchInstruction: Boolean = true
    final override def asSimpleConditionalBranchInstruction: this.type = this

    /**
     * @inheritdoc
     *
     * A simple conditional branch instruction has two targets unless both targets point
     * to the same instruction. In that case the jump has only one target, because the state
     * - independent of the taken path - always has to be the same.
     */
    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Chain[PC] = {
        val nextInstruction = indexOfNextInstruction(currentPC)
        val jumpInstruction = currentPC + branchoffset
        if (nextInstruction == jumpInstruction)
            Chain.singleton(nextInstruction)
        else
            nextInstruction :&: jumpInstruction :&: Naught
    }

    override def toString(currentPC: Int): String = {
        val jumpDirection = if (branchoffset >= 0) "↓" else "↑"
        s"${getClass.getSimpleName}(true=${currentPC + branchoffset}$jumpDirection, false=↓)"
    }

}
/**
 * Extractor for [[SimpleConditionalBranchInstruction]]s.
 */
object SimpleConditionalBranchInstruction {

    /**
     * Extracts the instructions branchoffset.
     */
    def unapply(i: SimpleConditionalBranchInstruction[_ <: SimpleConditionalBranchInstruction[_]]): Some[Int] = Some(i.branchoffset)

}
