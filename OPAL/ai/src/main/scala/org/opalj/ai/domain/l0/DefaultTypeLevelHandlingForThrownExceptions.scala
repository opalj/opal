/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

/**
 * Basic implementation of a `Domain`'s `abruptMethodExecution` method that does
 * nothing.
 *
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelHandlingForThrownExceptions extends ReturnInstructionsDomain {
    domain: ValuesDomain with Configuration with ExceptionsFactory =>

    /*base impl.*/ def abruptMethodExecution(pc: Int, exception: ExceptionValue): Unit = {
        /* Nothing to do. */
    }
}

