/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Provides default implementations for a `Domain`'s return methods that always throw
 * an `IllegalMonitorStateExceptoin`.
 *
 * You can mix in this trait if you are not interested in a method's return values or if
 * you need some default implementations.
 *
 * @author Michael Eichberg
 */
trait DefaultHandlingForReturnInstructions extends ReturnInstructionsDomain {
    domain: ValuesDomain with ExceptionsFactory with Configuration =>

    /*base impl.*/ def areturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
        handleReturn(pc)

    }

    /*base impl.*/ def dreturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
        handleReturn(pc)
    }

    /*base impl.*/ def freturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
        handleReturn(pc)
    }

    /*base impl.*/ def ireturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
        handleReturn(pc)
    }

    /*base impl.*/ def lreturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
        handleReturn(pc)
    }

}
