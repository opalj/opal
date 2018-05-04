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
 * Common super class of all compound conditional branch instructions
 * (switch instructions!).
 *
 * @author Michael Eichberg
 */
trait CompoundConditionalBranchInstructionLike extends ConditionalBranchInstructionLike {

    override def operandCount: Int = 1

    final override def stackSlotsChange: Int = -1

    /**
     * Returns all case values that are '''not related to the default branch'''.
     */
    def caseValues: Iterable[Int]

}

trait CompoundConditionalBranchInstruction
    extends ConditionalBranchInstruction
    with CompoundConditionalBranchInstructionLike {

    final override def isCompoundConditionalBranchInstruction: Boolean = true
    final override def asCompoundConditionalBranchInstruction: this.type = this

    def defaultOffset: Int

    def jumpOffsets: Iterable[Int]

    final override def jumpTargets(
        currentPC: PC
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Iterator[PC] = {
        jumpOffsets.iterator.map(_ + currentPC) ++ Iterator(defaultOffset + currentPC)
    }

    /**
     * Returns the case value(s) that are associated with the given `jumpOffset`.
     * If the `jumpOffset` is also the `defaultOffset`, the return value's second
     * value is true.
     */
    def caseValueOfJumpOffset(jumpOffset: Int): (Chain[Int], Boolean)

}
