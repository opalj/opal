/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Defines the methods that lead to a return from a method. In general, a return instruction
 * can throw an `IllegalMonitorStateException`. If, e.g., the method is synchronized and
 * the method body contains a `Monitorexit` instruction, but no `Monitorenter` instruction.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait ReturnInstructionsDomain { domain: ValuesDomain =>

    /**
     * Called when a return instruction with the given `pc` is reached.
     * In other words, when the method returns normally.
     */
    def returnVoid(pc: Int): Computation[Nothing, ExceptionValue]

    /**
     * The given `value`, which is a value with ''computational type integer'', is returned
     * by the return instruction with the given `pc`.
     */
    def ireturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue]

    /**
     * The given `value`, which is a value with ''computational type long'', is returned
     * by the return instruction with the given `pc`.
     */
    def lreturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue]

    /**
     * The given `value`, which is a value with ''computational type float'', is returned
     * by the return instruction with the given `pc`.
     */
    def freturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue]

    /**
     * The given `value`, which is a value with ''computational type double'', is returned
     * by the return instruction with the given `pc`.
     */
    def dreturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue]

    /**
     * The given `value`, which is a value with ''computational type reference'', is returned
     * by the return instruction with the given `pc`.
     */
    def areturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue]

    /**
     * Called by the abstract interpreter when an exception is thrown that is not
     * (guaranteed to be) handled within the same method.
     *
     * @note If the original exception value is `null` (`/*E.g.*/throw null;`), then
     *      the exception that is actually thrown is a new `NullPointerException`. This
     *      situation is, however, completely handled by OPAL and the exception value
     *      is hence never `null`.
     */
    def abruptMethodExecution(pc: Int, exceptionValue: ExceptionValue): Unit

}
