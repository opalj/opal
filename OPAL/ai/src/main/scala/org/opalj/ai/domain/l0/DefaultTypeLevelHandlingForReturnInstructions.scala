/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

/**
 * Provides default implementations for a `Domain`'s return methods that always throw
 * an `IllegalMonitorStateExceptoin`.
 *
 * You can mix in this trait if you are not interested in a method's return values or if
 * you need some default implementations.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelHandlingForReturnInstructions extends ReturnInstructionsDomain {
    domain: ValuesDomain with ExceptionsFactory with Configuration =>

    def areturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
        /*base impl.*/
        handleReturn(pc)
    }

    def dreturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
        /*base impl.*/
        handleReturn(pc)
    }

    def freturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
        /*base impl.*/
        handleReturn(pc)
    }

    def ireturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
        /*base impl.*/
        handleReturn(pc)
    }

    def lreturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
        /*base impl.*/
        handleReturn(pc)
    }

}
