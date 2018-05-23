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

/**
 * Common superclass of all control transfer instructions.
 *
 * @author Michael Eichberg
 */
trait ControlTransferInstructionLike extends InstructionLike {

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def expressionResult: NoExpression.type = NoExpression

    final override def isControlTransferInstruction: Boolean = true

}

trait ControlTransferInstruction extends Instruction with ControlTransferInstructionLike {

    /**
     * Iterator over all (absolute) pcs to which this instruction will jump to.
     *
     * @note Computing the jump targets is particularly expensive in case of [[RET]] instructions,
     *       and should be avoided if only "all jump targets" of a method's body should be
     *       identified. In that case, collecting the PCs following the JSRs is sufficient.
     *
     * @return All instructions to which this instruction explicitly jumps to. (The instruction to
     *         which an if-instruction i potentially falls through, is not a jump target
     *         w.r.t. i; it may still be a jump target w.r.t. some other control transfer
     *         instruction.)
     */
    def jumpTargets(
        currentPC: Int
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Iterator[Int /*PC*/ ] // IMPROVE Use IntIterator!

    final override def asControlTransferInstruction: ControlTransferInstruction = this

}
