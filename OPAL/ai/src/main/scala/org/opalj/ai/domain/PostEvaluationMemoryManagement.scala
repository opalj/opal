/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package ai
package domain

import org.opalj.br.instructions.Instruction

/**
 * Provides the possibility to further update the memory layout (registers and operands)
 * after the evaluation of an instruction, but before any potential join is performed.
 *
 * If this domain is mixed in then the domain cannot be used to simultaneously analyze
 * multiple different methods at the same time.
 *
 * @author Michael Eichberg
 */
trait PostEvaluationMemoryManagement extends CoreDomainFunctionality {

    private[this] var oldValue: DomainValue = null
    private[this] var newValue: DomainValue = null

    protected def updateAfterEvaluation(oldValue: DomainValue, newValue: DomainValue): Unit = {
        assert(oldValue ne newValue, "it doesn't make sense to update a value with itself")
        assert(oldValue ne null)
        assert(newValue ne null)

        assert(this.oldValue eq null)

        this.oldValue = oldValue
        this.newValue = newValue
    }

    abstract override def afterEvaluation(
        pc: PC,
        instruction: Instruction,
        oldOperands: Operands,
        oldLocals: Locals,
        targetPC: PC,
        isExceptionalControlFlow: Boolean,
        newOperands: Operands,
        newLocals: Locals): (Operands, Locals) = {
        val oldValue = this.oldValue
        if (oldValue ne null) {
            val (operands1, locals1) =
                updateMemoryLayout(oldValue, newValue, newOperands, newLocals)
            this.oldValue = null
            this.newValue = null

            super.afterEvaluation(
                pc, instruction, oldOperands, oldLocals,
                targetPC, isExceptionalControlFlow, operands1, locals1)
        } else
            super.afterEvaluation(
                pc, instruction, oldOperands, oldLocals,
                targetPC, isExceptionalControlFlow, newOperands, newLocals)
    }

}
