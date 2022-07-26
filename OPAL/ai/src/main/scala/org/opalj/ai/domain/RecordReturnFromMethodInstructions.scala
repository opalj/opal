/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
    domain: ValuesDomain =>

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
