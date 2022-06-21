/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Basic implementation of a `Domain`s `returnVoid` method that does nothing.
 *
 * @author Michael Eichberg
 */
trait DefaultHandlingOfVoidReturns extends ReturnInstructionsDomain {
    domain: ValuesDomain with ExceptionsFactory with Configuration =>

    /*base impl.*/ def returnVoid(pc: Int): Computation[Nothing, ExceptionValue] = {
        handleReturn(pc)
    }

}

