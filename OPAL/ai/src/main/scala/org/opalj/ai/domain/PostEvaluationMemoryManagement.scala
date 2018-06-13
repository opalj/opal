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
package ai
package domain

import org.opalj.br.instructions.Instruction

/**
 * Provides the possibility to further update the memory layout (registers and operands)
 * after the execution of an instruction, but before any potential join is performed.
 *
 * Using this domain is only safe if the (partial-)domains that use this functionality
 * never interfere with each other.
 *
 * @note    If this domain is mixed in then the domain cannot be used to simultaneously analyze
 *          multiple different methods at the same time.
 *
 * @author Michael Eichberg
 */
trait PostEvaluationMemoryManagement extends CoreDomainFunctionality {

    private[this] var oldValue: DomainValue = null
    private[this] var newValueAfterEvaluation: DomainValue = null
    private[this] var newValueAfterException: DomainValue = null

    protected def updateAfterExecution(
        oldValue:                DomainValue,
        newValueAfterEvaluation: DomainValue,
        newValueAfterException:  DomainValue
    ): Unit = {
        assert(this.oldValue eq null, "another update is already registered")

        assert(oldValue ne null)
        assert((newValueAfterEvaluation ne null) || (newValueAfterException ne null))
        assert(oldValue ne newValueAfterEvaluation, "useless self update")
        assert(oldValue ne newValueAfterException, "useless self update")

        this.oldValue = oldValue
        this.newValueAfterEvaluation = newValueAfterEvaluation
        this.newValueAfterException = newValueAfterException
    }

    protected def updateAfterEvaluation(oldValue: DomainValue, newValue: DomainValue): Unit = {
        updateAfterExecution(oldValue, newValue, null)
    }

    protected def updateAfterException(oldValue: DomainValue, newValue: DomainValue): Unit = {
        updateAfterExecution(oldValue, null, newValue)
    }

    abstract override def afterEvaluation(
        pc:                       Int,
        instruction:              Instruction,
        oldOperands:              Operands,
        oldLocals:                Locals,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean,
        forceJoin:                Boolean,
        newOperands:              Operands,
        newLocals:                Locals
    ): (Operands, Locals) = {
        val oldValue = this.oldValue
        if (oldValue ne null) {
            val (operands1, locals1) =
                if (isExceptionalControlFlow)
                    updateMemoryLayout(oldValue, newValueAfterException, newOperands, newLocals)
                else
                    updateMemoryLayout(oldValue, newValueAfterEvaluation, newOperands, newLocals)
            this.oldValue = null
            this.newValueAfterEvaluation = null
            this.newValueAfterException = null

            super.afterEvaluation(
                pc, instruction, oldOperands, oldLocals,
                targetPC, isExceptionalControlFlow, forceJoin, operands1, locals1
            )
        } else {
            super.afterEvaluation(
                pc, instruction, oldOperands, oldLocals,
                targetPC, isExceptionalControlFlow, forceJoin, newOperands, newLocals
            )
        }
    }

}
