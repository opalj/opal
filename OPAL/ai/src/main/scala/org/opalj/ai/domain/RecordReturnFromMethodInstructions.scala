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

/**
 * Records the program counters of all instructions that lead to a (ab)normal
 * return from the method. I.e., every instruction that may throw an exception or
 * causes a normal return from method may be recorded. Examples of instructions
 * that may be recorded: `(X)return`, `throw`, `invoke(XXX)`, `get|put(static|field)`,
 * `checkcast`, `(a)new(array)`,... . Instructions such as `swap` or `dup(XXX)`, however,
 * never directly lead to a method return (they will never result in an exception)
 * and will not be recorded.
 *
 * If you are interested in recording the values use: [[RecordReturnedValues]],
 * [[RecordThrownExceptions]].
 *
 * ==Usage==
 * This domain can be stacked on top of other traits that handle
 * return instructions and abrupt method executions.
 *
 * @author Michael Eichberg
 */
trait RecordReturnFromMethodInstructions extends ai.ReturnInstructionsDomain {
    domain: ValuesDomain ⇒

    @volatile private[this] var returnFromMethodInstructions: PCs = NoPCs

    def allReturnFromMethodInstructions: PCs = returnFromMethodInstructions

    abstract override def areturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        returnFromMethodInstructions += pc
        super.areturn(pc, value)
    }

    abstract override def dreturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        returnFromMethodInstructions += pc
        super.dreturn(pc, value)
    }

    abstract override def freturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        returnFromMethodInstructions += pc
        super.freturn(pc, value)
    }

    abstract override def ireturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        returnFromMethodInstructions += pc
        super.ireturn(pc, value)
    }

    abstract override def lreturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        returnFromMethodInstructions += pc
        super.lreturn(pc, value)
    }

    abstract override def returnVoid(pc: Int): Computation[Nothing, ExceptionValue] = {
        returnFromMethodInstructions += pc
        super.returnVoid(pc)
    }

    // handles all kinds of abrupt method returns
    abstract override def abruptMethodExecution(pc: Int, exception: ExceptionValue): Unit = {
        returnFromMethodInstructions += pc
        super.abruptMethodExecution(pc, exception)
    }
}
